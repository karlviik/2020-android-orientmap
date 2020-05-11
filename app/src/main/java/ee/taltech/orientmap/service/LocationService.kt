package ee.taltech.orientmap.service

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.volley.Response
import com.google.android.gms.location.*
import ee.taltech.orientmap.C
import ee.taltech.orientmap.R
import ee.taltech.orientmap.db.LocationRepository
import ee.taltech.orientmap.db.SessionRepository
import ee.taltech.orientmap.poko.LocationModel
import ee.taltech.orientmap.poko.SessionModel
import ee.taltech.orientmap.utils.ApiUtils
import ee.taltech.orientmap.utils.PreferenceUtils
import ee.taltech.orientmap.utils.Utils
import org.json.JSONObject
import org.threeten.bp.LocalDateTime
import org.threeten.bp.temporal.ChronoUnit
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

// TODO: on session activate, put it to preferences, on session end, put it to false
class LocationService : Service() {
	companion object {
		private val TAG = this::class.java.declaringClass!!.simpleName
		private var mInstance: LocationService? = null
		
		// maximum allowed distance jump between 2 consecutive location updates
		private const val MAXIMUM_ALLOWED_DISTANCE_JUMP = 50
		private const val MINIMUM_ALLOWED_DISTANCE_JUMP = 5
		private const val MAXIMUM_ALLOWED_ACCURACY = 20f
		
		// The desired intervals for location updates. Inexact. Updates may be more or less frequent.
		
		fun isServiceCreated(): Boolean {
			return try {
				// If instance was not cleared but the service was destroyed an Exception will be thrown
				mInstance != null && mInstance!!.ping()
			} catch (e: NullPointerException) {
				// destroyed/not-started
				false
			}
		}
	}
	
	// Simply returns true. If the service is still active, this method will be accessible.
	private fun ping(): Boolean {
		return true
	}
	
	private var updateIntervalInMilliseconds: Long? = null
	private var fastestUpdateIntervalInMilliseconds: Long? = null
	
	private val broadcastReceiver = InnerBroadcastReceiver()
	private val broadcastReceiverIntentFilter: IntentFilter = IntentFilter()
	
	private val mLocationRequest: LocationRequest = LocationRequest()
	private lateinit var mFusedLocationClient: FusedLocationProviderClient
	private var mLocationCallback: LocationCallback? = null
	
	// last received location
	private var currentLocation: Location? = null
	
	// location for handling potential error flicks
	private var bufferLocation: Location? = null
	
	private var distanceOverallTotal = 0f
	private var overallStartTime: LocalDateTime? = null
	private var locationStart: Location? = null
	
	private var isCpSet = false
	private var distanceCpDirect = 0f
	private var distanceCpTotal = 0f
	private var cpStartTime: LocalDateTime? = null
	private var locationCp: Location? = null
	
	private var isWpSet = false
	private var distanceWpDirect = 0f
	private var distanceWpTotal = 0f
	private var wpStartTime: LocalDateTime? = null
	private var locationWp: Location? = null
	
	private var allLocations = ArrayList<Location>()
	private var allCpLocations = ArrayList<Location>()
	
	private lateinit var sessionRepository: SessionRepository
	private lateinit var locationRepository: LocationRepository
	private lateinit var session: SessionModel
	
	private var sessionId: String? = null
	private var locationBuffer: HashSet<LocationModel> = HashSet()
	
	private lateinit var timer: Timer
	private var syncInterval: Int? = null
	
	override fun onCreate() {
		Log.d(TAG, "onCreate")
		mInstance = this
		super.onCreate()
		syncInterval = PreferenceUtils.getDefaultSyncInterval(this)
		updateIntervalInMilliseconds = PreferenceUtils.getGpsUpdateInterval(this@LocationService) * 1000L
		fastestUpdateIntervalInMilliseconds = updateIntervalInMilliseconds!! / 2
		
		sessionRepository = SessionRepository(this).open()
		locationRepository = LocationRepository(this).open()
		
		val slowTime = PreferenceUtils.getSlowSpeedTime(this)
		val fastTime = PreferenceUtils.getFastSpeedTime(this)
		session = SessionModel("", "Session", LocalDateTime.now())
		
		sessionRepository.add(session)
		
		broadcastReceiverIntentFilter.addAction(C.NOTIFICATION_CP_ADD_TO_CURRENT)
		broadcastReceiverIntentFilter.addAction(C.NOTIFICATION_WP_ADD_TO_CURRENT)
		broadcastReceiverIntentFilter.addAction(C.WP_REMOVE)
		broadcastReceiverIntentFilter.addAction(C.LOCATION_UPDATE_ACTION)
		broadcastReceiverIntentFilter.addAction(C.WP_ADD_TO_CURRENT)
		broadcastReceiverIntentFilter.addAction(C.CP_ADD_TO_CURRENT)
		broadcastReceiverIntentFilter.addAction(C.REQUEST_CP_LOCATIONS)
		broadcastReceiverIntentFilter.addAction(C.REQUEST_POINTS_LOCATIONS)
		broadcastReceiverIntentFilter.addAction(C.REQUEST_WP_LOCATION)
		
		registerReceiver(broadcastReceiver, broadcastReceiverIntentFilter)
		
		mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
		
		mLocationCallback = object : LocationCallback() {
			override fun onLocationResult(locationResult: LocationResult) {
				super.onLocationResult(locationResult)
				onNewLocation(locationResult.lastLocation)
			}
		}
		
		getLastLocation()
		
		createLocationRequest()
		requestLocationUpdates()
		
		// creates session and pushes to backend if can
		val timerTask = object : TimerTask() {
			override fun run() {
				if (PreferenceUtils.isLoggedIn(this@LocationService)) {
					// get token first (if no token, return), also get user email
					val token: String = PreferenceUtils.getToken(this@LocationService) ?: return
					val email = PreferenceUtils.getUserEmail(this@LocationService) ?: return
					if (sessionId == null) {
						// if session hasn't been started
						val listener: Response.Listener<JSONObject> = Response.Listener { response ->
							sessionId = response.getString("id")
							session.apiId = sessionId as String
							session.userEmail = email
							sessionRepository.update(session)
						}
						// Not much to do if error, just try again later
						val errorListener = Response.ErrorListener {}
						ApiUtils.createSession(this@LocationService, listener, errorListener, session, token)
					} else {
						// session has been created in back-end, see if there are any locations in buffer waiting for push
						// make to list and effectively remove them
						val tempLocList = locationBuffer.toList()
						locationBuffer = HashSet()
						
						pushLocationsToApi(tempLocList, token, false)
					}
				}
				// else not logged in, can't do anything against that
			}
		}
		
		timer = Timer()
		// just make min 1s so no spam
		val period = if (syncInterval!! <= 1) 1000L else syncInterval!! * 1000L
		timer.schedule(timerTask, 0L, period)
	}
	
	private fun pushLocationsToApi(locations: Collection<LocationModel>, token: String, skipError: Boolean) {
		if (sessionId == null) return
		val listener: Response.Listener<JSONObject> = Response.Listener { _ ->
			for (location in locations) {
				location.isUploaded = true
				locationRepository.update(location)
			}
			session.uploadedLocationCount += locations.size
			sessionRepository.update(session)
		}
		val errorListener = Response.ErrorListener { _ ->
			if (!skipError) locationBuffer.addAll(locations)
		}
		ApiUtils.createLocations(this@LocationService, listener, errorListener, locations, sessionId!!, token)
	}
	
	// 0 is loc, 1 wp, 2 cp
	private fun addLocationToDbAndBuffer(location: Location, type: Int) {
		val locM = LocationModel(location, type, session.id)
		locationRepository.add(locM)
		locationBuffer.add(locM)
		session.locationCount += 1
		sessionRepository.update(session)
	}
	
	private fun requestLocationUpdates() {
		Log.i(TAG, "Requesting location updates")
		
		try {
			mFusedLocationClient.requestLocationUpdates(
				mLocationRequest,
				mLocationCallback, Looper.myLooper()
			)
		} catch (unlikely: SecurityException) {
			Log.e(
				TAG,
				"Lost location permission. Could not request updates. $unlikely"
			)
		}
	}
	
	private fun createStringsForSessionStatistics(): Map<String, String> {
		val strings = HashMap<String, String>()
		
		val now = LocalDateTime.now()
		
		var overallDistance = ""
		var overallTime = ""
		var overallTempo = ""
		
		if (locationStart != null) {
			val hoursFromStart: Int = ChronoUnit.HOURS.between(overallStartTime, now).toInt()
			val minutesFromStart: Int = ChronoUnit.MINUTES.between(overallStartTime, now).toInt() % 60
			val secondsFromStart: Int = ChronoUnit.SECONDS.between(overallStartTime, now).toInt() % 60
			val onlyMinutesFromStart = ChronoUnit.SECONDS.between(overallStartTime, now) / 60.0
			var minutesPerKm = onlyMinutesFromStart / (distanceOverallTotal / 1000)
			val hoursPerKm: Int = minutesPerKm.toInt() / 60
			minutesPerKm -= 60 * hoursPerKm
			
			overallDistance = "%d".format(distanceOverallTotal.toInt())
			
			if (hoursFromStart > 0) {
				overallTime = "%d:%02d:%02d".format(hoursFromStart, minutesFromStart, secondsFromStart)
			} else {
				overallTime = "%d:%02d".format(minutesFromStart, secondsFromStart)
			}
			
			if (hoursPerKm > 0) {
				if (hoursPerKm > 50) {
					overallTempo = ">50h"
				} else {
					overallTempo = "%d:%02d:%02d".format(hoursPerKm, minutesPerKm.toInt(), (minutesPerKm * 60).toInt() % 60)
				}
			} else {
				overallTempo = "%d:%02d".format(minutesPerKm.toInt(), (minutesPerKm * 60).toInt() % 60)
			}
			
		} else {
			overallDistance = "-----"
			overallTime = "-----"
			overallTempo = "-----"
		}
		
		strings["overallDistance"] = overallDistance
		strings["overallTime"] = overallTime
		strings["overallTempo"] = overallTempo
		
		var wpDistance = ""
		var wpDirect = ""
		var wpTempo = ""
		
		if (isWpSet) {
			val onlyMinutesFromStart = ChronoUnit.SECONDS.between(wpStartTime, now) / 60.0
			var minutesPerKm = onlyMinutesFromStart / (distanceWpTotal / 1000)
			val hoursPerKm: Int = minutesPerKm.toInt() / 60
			minutesPerKm -= 60 * hoursPerKm
			
			wpDistance = "%d".format(distanceWpTotal.toInt())
			wpDirect = "%d".format(distanceWpDirect.toInt())
			
			if (hoursPerKm > 0) {
				if (hoursPerKm > 50) {
					wpTempo = ">50h"
				} else {
					wpTempo = "%d:%02d:%02d".format(hoursPerKm, minutesPerKm.toInt(), (minutesPerKm * 60).toInt() % 60)
				}
			} else {
				wpTempo = "%d:%02d".format(minutesPerKm.toInt(), (minutesPerKm * 60).toInt() % 60)
			}
			
		} else {
			wpDistance = "-----"
			wpDirect = "-----"
			wpTempo = "-----"
		}
		
		strings["wpDistance"] = wpDistance
		strings["wpDirect"] = wpDirect
		strings["wpTempo"] = wpTempo
		
		
		var cpDistance = ""
		var cpDirect = ""
		var cpTempo = ""
		
		if (isCpSet) {
			val onlyMinutesFromStart = ChronoUnit.SECONDS.between(cpStartTime, now) / 60.0
			var minutesPerKm = onlyMinutesFromStart / (distanceCpTotal / 1000)
			val hoursPerKm: Int = minutesPerKm.toInt() / 60
			minutesPerKm -= 60 * hoursPerKm
			
			cpDistance = "%d".format(distanceCpTotal.toInt())
			cpDirect = "%d".format(distanceCpDirect.toInt())
			if (hoursPerKm > 0) {
				if (hoursPerKm > 50) {
					cpTempo = ">50h"
				} else {
					cpTempo = "%d:%02d:%02d".format(hoursPerKm, minutesPerKm.toInt(), (minutesPerKm * 60).toInt() % 60)
				}
			} else {
				cpTempo = "%d:%02d".format(minutesPerKm.toInt(), (minutesPerKm * 60).toInt() % 60)
			}
		} else {
			cpDistance = "-----"
			cpDirect = "-----"
			cpTempo = "-----"
		}
		
		strings["cpDistance"] = cpDistance
		strings["cpDirect"] = cpDirect
		strings["cpTempo"] = cpTempo
		
		return strings
		
	}
	
	private fun onNewLocation(location: Location) {
		Log.i(TAG, "New location: $location")
		
		// handling for too big distance jumps
		if (currentLocation != null && bufferLocation != null && location.distanceTo(bufferLocation) > MAXIMUM_ALLOWED_DISTANCE_JUMP) {
			bufferLocation = location
			return
		}
		// handling for too low distance change or no / low accuracy
		if (currentLocation != null && currentLocation!!.distanceTo(location) < MINIMUM_ALLOWED_DISTANCE_JUMP || !location.hasAccuracy() || location.accuracy > MAXIMUM_ALLOWED_ACCURACY) {
			return
		}
		
		
		allLocations.add(location)
		addLocationToDbAndBuffer(location, 0)
		
		if (currentLocation == null) {
			locationStart = location
			overallStartTime = LocalDateTime.now()
			session.start = LocalDateTime.now() // session edit here m8
		} else {
			distanceOverallTotal += location.distanceTo(currentLocation)
			session.distance = distanceOverallTotal.toInt() // session edit here m8
			session.duration = ChronoUnit.SECONDS.between(session.start, LocalDateTime.now()).toInt() // session edit here m8
			session.timePerKm = (session.duration / (session.distance / 1000.0)).toInt()
			sessionRepository.update(session)
			
			if (isCpSet) {
				distanceCpDirect = location.distanceTo(locationCp)
				distanceCpTotal += location.distanceTo(currentLocation)
			}
			if (isWpSet) {
				distanceWpDirect = location.distanceTo(locationWp)
				distanceWpTotal += location.distanceTo(currentLocation)
			}
		}
		// save the location for calculations
		val prevLocation = currentLocation
		currentLocation = location
		bufferLocation = location
		
		showNotification()
		
		// broadcast new location to UI
		val intent = Intent(C.LOCATION_UPDATE_ACTION)
		
		var hasLocation = false
		if (prevLocation != null) {
			hasLocation = true
			intent.putExtra(C.LOCATION_UPDATE_ACTION_LATITUDE, location.latitude)
			intent.putExtra(C.LOCATION_UPDATE_ACTION_LONGITUDE, location.longitude)
			intent.putExtra(C.LOCATION_UPDATE_ACTION_PREV_LATITUDE, prevLocation.latitude)
			intent.putExtra(C.LOCATION_UPDATE_ACTION_PREV_LONGITUDE, prevLocation.longitude)
			intent.putExtra(
				C.LOCATION_UPDATE_ACTION_COLOR,
				Utils.getColorBasedOnGradient(prevLocation, currentLocation!!, PreferenceUtils.getFastSpeedTime(this), PreferenceUtils.getSlowSpeedTime(this), C.FAST_COLOR, C.SLOW_COLOR)
			)
		}
		intent.putExtra(C.LOCATION_UPDATE_ACTION_HAS_LOCATION, hasLocation)
		
		intent.putExtra(C.LOCATION_UPDATE_MOVEMENT_BEARING, prevLocation?.bearingTo(currentLocation))
		
		val map = createStringsForSessionStatistics()
		
		intent.putExtra(C.LOCATION_UPDATE_ACTION_OVERALL_DISTANCE, map["overallDistance"])
		intent.putExtra(C.LOCATION_UPDATE_ACTION_OVERALL_PACE, map["overallTempo"])
		intent.putExtra(C.LOCATION_UPDATE_ACTION_OVERALL_TIME, map["overallTime"])
		
		intent.putExtra(C.LOCATION_UPDATE_ACTION_WP_DIRECT_DISTANCE, map["wpDirect"])
		intent.putExtra(C.LOCATION_UPDATE_ACTION_WP_TOTAL_DISTANCE, map["wpDistance"])
		intent.putExtra(C.LOCATION_UPDATE_ACTION_WP_PACE, map["wpTempo"])
		
		intent.putExtra(C.LOCATION_UPDATE_ACTION_CP_DIRECT_DISTANCE, map["cpDirect"])
		intent.putExtra(C.LOCATION_UPDATE_ACTION_CP_TOTAL_DISTANCE, map["cpDistance"])
		intent.putExtra(C.LOCATION_UPDATE_ACTION_CP_PACE, map["cpTempo"])
		
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
		
	}
	
	private fun createLocationRequest() {
		mLocationRequest.interval = updateIntervalInMilliseconds!!
		mLocationRequest.fastestInterval = fastestUpdateIntervalInMilliseconds!!
		mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
		mLocationRequest.maxWaitTime = updateIntervalInMilliseconds!!
	}
	
	private fun getLastLocation() {
		try {
			mFusedLocationClient.lastLocation
				.addOnCompleteListener { task ->
					if (task.isSuccessful) {
						Log.w(TAG, "Task successful");
						if (task.result != null) {
							onNewLocation(task.result!!)
						}
					} else {
						Log.w(TAG, "Failed to get location." + task.exception)
					}
				}
		} catch (unlikely: SecurityException) {
			Log.e(TAG, "Lost location permission. $unlikely")
		}
	}
	
	override fun onDestroy() {
		Log.d(TAG, "onDestroy")
		mInstance = null
		timer.cancel()
		val token = PreferenceUtils.getToken(this)
		
		try {
			if (token != null) {
				val listener: Response.Listener<JSONObject> = Response.Listener { _ ->
					for (location in locationBuffer) {
						location.isUploaded = true
						locationRepository.update(location)
					}
					session.uploadedLocationCount += locationBuffer.size
					sessionRepository.update(session)
					locationRepository.close()
					sessionRepository.close()
				}
				val errorListener = Response.ErrorListener { _ ->
					locationRepository.close()
					sessionRepository.close()
				}
				ApiUtils.createLocations(this@LocationService, listener, errorListener, locationBuffer, sessionId!!, token)
			}
		} catch (e: Exception) {}
		
		
		super.onDestroy()
		
		//stop location updates
		mFusedLocationClient.removeLocationUpdates(mLocationCallback)
		
		// remove notifications
		NotificationManagerCompat.from(this).cancelAll()
		
		// don't forget to unregister broadcast receiver
		unregisterReceiver(broadcastReceiver)
		
		/// this seems weird
		// broadcast stop to UI
//		val intent = Intent(C.LOCATION_UPDATE_ACTION)
//		LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
	}
	
	override fun onLowMemory() {
		Log.d(TAG, "onLowMemory")
		super.onLowMemory()
	}
	
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		Log.d(TAG, "onStartCommand")
		
		// set counters and locations to 0/null
		currentLocation = null
		locationStart = null
		locationCp = null
		isCpSet = false
		cpStartTime = null
		isWpSet = false
		locationWp = null
		wpStartTime = null
		
		distanceOverallTotal = 0f
		distanceCpDirect = 0f
		distanceCpTotal = 0f
		distanceWpDirect = 0f
		distanceWpTotal = 0f
		
		
		showNotification()
		
		return START_STICKY
		//return super.onStartCommand(intent, flags, startId)
	}
	
	override fun onBind(intent: Intent?): IBinder? {
		Log.d(TAG, "onBind")
		TODO("not implemented")
	}
	
	override fun onRebind(intent: Intent?) {
		Log.d(TAG, "onRebind")
		super.onRebind(intent)
	}
	
	override fun onUnbind(intent: Intent?): Boolean {
		Log.d(TAG, "onUnbind")
		return super.onUnbind(intent)
	}
	
	fun showNotification() {
		val intentCp = Intent(C.NOTIFICATION_CP_ADD_TO_CURRENT)
		val intentWp = Intent(C.NOTIFICATION_WP_ADD_TO_CURRENT)
		
		val pendingIntentCp = PendingIntent.getBroadcast(this, 0, intentCp, 0)
		val pendingIntentWp = PendingIntent.getBroadcast(this, 0, intentWp, 0)
		
		val notifyview = RemoteViews(packageName, R.layout.notification_control)
		
		notifyview.setOnClickPendingIntent(R.id.imageButtonCP, pendingIntentCp)
		notifyview.setOnClickPendingIntent(R.id.imageButtonWP, pendingIntentWp)
		
		val map = createStringsForSessionStatistics()
		
		notifyview.setTextViewText(R.id.textViewOverallTotal, map["overallDistance"])
		notifyview.setTextViewText(R.id.textViewOverallTime, map["overallTime"])
		notifyview.setTextViewText(R.id.textViewOverallTempo, map["overallTempo"])
		
		notifyview.setTextViewText(R.id.textViewWPTotal, map["wpDistance"])
		notifyview.setTextViewText(R.id.textViewWPDirect, map["wpDirect"])
		notifyview.setTextViewText(R.id.textViewWPTempo, map["wpTempo"])
		
		notifyview.setTextViewText(R.id.textViewCPTotal, map["cpDistance"])
		notifyview.setTextViewText(R.id.textViewCPDirect, map["cpDirect"])
		notifyview.setTextViewText(R.id.textViewCPTempo, map["cpTempo"])
		
		// construct and show notification
		val builder = NotificationCompat.Builder(applicationContext, C.NOTIFICATION_CHANNEL)
			.setSmallIcon(R.drawable.baseline_gps_fixed_24)
			.setPriority(NotificationCompat.PRIORITY_LOW)
			.setOngoing(true)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
		
		builder.setContent(notifyview)
		
		// Super important, start as foreground service - ie android considers this as an active app. Need visual reminder - notification.
		// must be called within 5 secs after service starts.
		startForeground(C.NOTIFICATION_ID, builder.build())
		
	}
	
	private inner class InnerBroadcastReceiver : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			Log.d(TAG, intent!!.action!!)   // maybe not action!!
			when (intent.action) {
				C.NOTIFICATION_WP_ADD_TO_CURRENT, C.WP_ADD_TO_CURRENT -> {
					isWpSet = true
					locationWp = currentLocation
					distanceWpDirect = 0f
					distanceWpTotal = 0f
					wpStartTime = LocalDateTime.now()
					try {
						addLocationToDbAndBuffer(currentLocation!!, 1)
					} catch (e: Exception) {
						// some weird stuff is happening
					}
					showNotification()
				}
				C.WP_REMOVE -> {
					isWpSet = false
					distanceWpDirect = 0f
					distanceWpTotal = 0f
					wpStartTime = null
					locationWp = null
				}
				C.NOTIFICATION_CP_ADD_TO_CURRENT, C.CP_ADD_TO_CURRENT -> {
					if (currentLocation != null) {
						isCpSet = true
						locationCp = currentLocation
						allCpLocations.add(currentLocation!!)
						distanceCpDirect = 0f
						distanceCpTotal = 0f
						cpStartTime = LocalDateTime.now()
						addLocationToDbAndBuffer(currentLocation!!, 2)
						showNotification()
					}
				}
				C.REQUEST_CP_LOCATIONS -> {
					val replyIntent = Intent(C.REPLY_CP_LOCATIONS)
					replyIntent.putExtra(C.GENERAL_LOCATIONS, allCpLocations)
					LocalBroadcastManager.getInstance(mInstance!!.applicationContext).sendBroadcast(replyIntent)
				}
				C.REQUEST_POINTS_LOCATIONS -> {
					val replyIntent = Intent(C.REPLY_POINTS_LOCATIONS)
					val tempAllLocations: ArrayList<Location> = allLocations
					replyIntent.putExtra(C.GENERAL_LOCATIONS, tempAllLocations)
					val colors = ArrayList<Int>()
					for (i in 1 until tempAllLocations.size) {
						colors.add(
							Utils.getColorBasedOnGradient(tempAllLocations[i - 1], tempAllLocations[i], PreferenceUtils.getFastSpeedTime(mInstance!!), PreferenceUtils.getSlowSpeedTime(mInstance!!), C.FAST_COLOR, C.SLOW_COLOR)
						)
					}
					replyIntent.putExtra(C.GENERAL_COLORS, colors)
					LocalBroadcastManager.getInstance(mInstance!!.applicationContext).sendBroadcast(replyIntent)
				}
				C.REQUEST_WP_LOCATION -> {
					val replyIntent = Intent(C.REPLY_WP_LOCATION)
					replyIntent.putExtra(C.GENERAL_LOCATION, locationWp)
					LocalBroadcastManager.getInstance(mInstance!!.applicationContext).sendBroadcast(replyIntent)
				}
			}
		}
		
	}
	
}
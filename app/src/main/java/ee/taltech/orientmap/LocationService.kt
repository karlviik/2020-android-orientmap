package ee.taltech.orientmap

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
import com.google.android.gms.location.*
import org.threeten.bp.LocalDateTime
import org.threeten.bp.temporal.ChronoUnit
import org.threeten.bp.temporal.TemporalUnit


class LocationService : Service() {
	companion object {
		private val TAG = this::class.java.declaringClass!!.simpleName
		private var mInstance: LocationService? = null
		
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
	
	// The desired intervals for location updates. Inexact. Updates may be more or less frequent.
	private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 2000
	private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2
	
	private val broadcastReceiver = InnerBroadcastReceiver()
	private val broadcastReceiverIntentFilter: IntentFilter = IntentFilter()
	
	private val mLocationRequest: LocationRequest = LocationRequest()
	private lateinit var mFusedLocationClient: FusedLocationProviderClient
	private var mLocationCallback: LocationCallback? = null
	
	// last received location
	private var currentLocation: Location? = null
	
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
	
	
	override fun onCreate() {
		Log.d(TAG, "onCreate")
		mInstance = this
		super.onCreate()
		
		broadcastReceiverIntentFilter.addAction(C.CP_ADD_TO_CURRENT)
		broadcastReceiverIntentFilter.addAction(C.WP_ADD_TO_CURRENT)
		broadcastReceiverIntentFilter.addAction(C.WP_REMOVE)
		broadcastReceiverIntentFilter.addAction(C.LOCATION_UPDATE_ACTION)
		
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
	
	private fun onNewLocation(location: Location) {
		Log.i(TAG, "New location: $location")
		if (currentLocation == null) {
			locationStart = location
			overallStartTime = LocalDateTime.now()
		} else {
			distanceOverallTotal += location.distanceTo(currentLocation)
			
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
		currentLocation = location
		
		showNotification()
		
		// broadcast new location to UI
		val intent = Intent(C.LOCATION_UPDATE_ACTION)
		intent.putExtra(C.LOCATION_UPDATE_ACTION_LATITUDE, location.latitude)
		intent.putExtra(C.LOCATION_UPDATE_ACTION_LONGITUDE, location.longitude)
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
		
	}
	
	private fun createLocationRequest() {
		mLocationRequest.interval = UPDATE_INTERVAL_IN_MILLISECONDS
		mLocationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
		mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
		mLocationRequest.maxWaitTime = UPDATE_INTERVAL_IN_MILLISECONDS
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
		val intentCp = Intent(C.CP_ADD_TO_CURRENT)
		val intentWp = Intent(C.WP_ADD_TO_CURRENT)
		
		val pendingIntentCp = PendingIntent.getBroadcast(this, 0, intentCp, 0)
		val pendingIntentWp = PendingIntent.getBroadcast(this, 0, intentWp, 0)
		
		val notifyview = RemoteViews(packageName, R.layout.notification_control)
		
		notifyview.setOnClickPendingIntent(R.id.imageButtonCP, pendingIntentCp)
		notifyview.setOnClickPendingIntent(R.id.imageButtonWP, pendingIntentWp)

		val now = LocalDateTime.now()
		
		if (locationStart != null) {
			val hoursFromStart: Int = ChronoUnit.HOURS.between(overallStartTime, now).toInt()
			val minutesFromStart: Int = ChronoUnit.MINUTES.between(overallStartTime, now).toInt() % 60
			val secondsFromStart: Int = ChronoUnit.SECONDS.between(overallStartTime, now).toInt() % 60
			val onlyMinutesFromStart = ChronoUnit.SECONDS.between(overallStartTime, now) / 60.0
			val minutesPerKm = onlyMinutesFromStart / (distanceOverallTotal / 1000)
			
			notifyview.setTextViewText(R.id.textViewOverallTotal, "%.1f".format(distanceOverallTotal))
			notifyview.setTextViewText(R.id.textViewOverallTime, "%d:%02d:%02d".format(hoursFromStart, minutesFromStart, secondsFromStart))
			notifyview.setTextViewText(R.id.textViewOverallTempo, "%d:%02d".format(minutesPerKm.toInt(), (minutesPerKm * 60).toInt() % 60))
		} else {
			notifyview.setTextViewText(R.id.textViewOverallTotal, "-----")
			notifyview.setTextViewText(R.id.textViewOverallTime, "-----")
			notifyview.setTextViewText(R.id.textViewOverallTempo, "-----")
		}
		
		
		if (isWpSet) {
			val onlyMinutesFromStart = ChronoUnit.SECONDS.between(wpStartTime, now) / 60.0
			val minutesPerKm = onlyMinutesFromStart / (distanceWpTotal / 1000)
			
			notifyview.setTextViewText(R.id.textViewWPTotal, "%.1f".format(distanceWpTotal))
			notifyview.setTextViewText(R.id.textViewWPDirect, "%.1f".format(distanceWpDirect))
			notifyview.setTextViewText(R.id.textViewWPTempo, "%d:%02d".format(minutesPerKm.toInt(), (minutesPerKm * 60).toInt() % 60))
		} else {
			notifyview.setTextViewText(R.id.textViewWPTotal, "-----")
			notifyview.setTextViewText(R.id.textViewWPDirect, "-----")
			notifyview.setTextViewText(R.id.textViewWPTempo, "-----")
		}
		
		if (isCpSet) {
			val onlyMinutesFromStart = ChronoUnit.SECONDS.between(cpStartTime, now) / 60.0
			val minutesPerKm = onlyMinutesFromStart / (distanceCpTotal / 1000)
			
			notifyview.setTextViewText(R.id.textViewCPTotal, "%.1f".format(distanceCpTotal))
			notifyview.setTextViewText(R.id.textViewCPDirect, "%.1f".format(distanceCpDirect))
			notifyview.setTextViewText(R.id.textViewCPTempo, "%d:%02d".format(minutesPerKm.toInt(), (minutesPerKm * 60).toInt() % 60))
		} else {
			notifyview.setTextViewText(R.id.textViewCPTotal, "-----")
			notifyview.setTextViewText(R.id.textViewCPDirect, "-----")
			notifyview.setTextViewText(R.id.textViewCPTempo, "-----")
		}
		
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
				C.WP_ADD_TO_CURRENT -> {
					isWpSet = true
					locationWp = currentLocation
					distanceWpDirect = 0f
					distanceWpTotal = 0f
					wpStartTime = LocalDateTime.now()
					showNotification()
				}
				C.WP_REMOVE -> {
					isWpSet = false
					distanceWpDirect = 0f
					distanceWpTotal = 0f
					wpStartTime = null
					locationWp = null
				}
				C.CP_ADD_TO_CURRENT -> {
					isCpSet = true
					locationCp = currentLocation
					distanceCpDirect = 0f
					distanceCpTotal = 0f
					cpStartTime = LocalDateTime.now()
					showNotification()
				}
			}
		}
		
	}
	
}
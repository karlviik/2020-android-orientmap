package ee.taltech.orientmap

// do not import this! never! If this get inserted automatically when pasting java code, remove it
//import android.R
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.map_general_control.*
import kotlinx.android.synthetic.main.map_track_control.*


class MainActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {
	
	// map display fragment
	private lateinit var mMap: GoogleMap
	
	private var lastPos: LatLng? = null
	private var polyline: Polyline? = null
	private var points = ArrayList<LatLng>()
	
	
	// broadcast values
	private val broadcastReceiver = InnerBroadcastReceiver()
	private val broadcastReceiverIntentFilter: IntentFilter = IntentFilter()
	
	// location service bool
	private var locationServiceActive = false
	
	// compassEnabled bool
	private var compassEnabled = false
	
	// movement center
	private var movementCentered = false
	
	// rotation mode, 0 free to rota, 1 northbound, 2 selfbound
	private var rotationLock = 0
	
	// something to use on map
	private var waitingForMap = true
	private var tempWp: LatLng? = null
	private var tempWpInit = false
	
	//// compass related vars
	private lateinit var sensorManager: SensorManager
	private lateinit var compass: ImageView
	private lateinit var accelerometer: Sensor
	private lateinit var magnetometer: Sensor
	
	private var currentDegree = 0.0f
	private var lastAccelerometer = FloatArray(3)
	private var lastMagnetometer = FloatArray(3)
	private var lastAccelerometerSet = false
	private var lastMagnetometerSet = false
	//// end of compass related vars
	
	companion object {
		// tag for logging
		private val TAG = this::class.java.declaringClass!!.simpleName
		
		// TODO: probably doesn't survive onDestroy
		private var wp: Marker? = null
	}
	
	/**
	 * Manipulates the map once available.
	 * This callback is triggered when the map is ready to be used.
	 * This is where we can add markers or lines, add listeners or move the camera. In this case,
	 * we just add a marker near Sydney, Australia.
	 * If Google Play services is not installed on the device, the user will be prompted to install
	 * it inside the SupportMapFragment. This method will only be triggered once the user has
	 * installed Google Play services and returned to the app.
	 */
	override fun onMapReady(googleMap: GoogleMap) {
		
		mMap = googleMap
		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(59.437, 24.745), 13f))
		if (checkPermissions()) {
			mMap.isMyLocationEnabled = true // enable blue dot
		}
		mMap.uiSettings.isCompassEnabled = false            // disable gmap compass
		mMap.uiSettings.isMyLocationButtonEnabled = false   // disable gmap center
		polyline = mMap.addPolyline(PolylineOptions().width(10F).color(Color.RED))
		
		if (waitingForMap) {
			changeMapCenter()
			changeRotationLock()
		}
		
		if (tempWpInit) {
			tempWpInit = false
			wp = mMap.addMarker(MarkerOptions().position(tempWp!!).icon(BitmapDescriptorFactory.fromResource(R.drawable.baseline_arrow_downward_black_36)))
			
		}
		
	}
	
	// ============================================== MAIN ENTRY - ONCREATE =============================================
	override fun onCreate(savedInstanceState: Bundle?) {
		if (!checkPermissions()) {
			requestPermissions()
		}
		
		Log.d(TAG, "onCreate")
		super.onCreate(savedInstanceState)
		
		// open a specific view
		setContentView(R.layout.activity_main)
		
		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		val mapFragment = supportFragmentManager
			.findFragmentById(R.id.map) as SupportMapFragment
		mapFragment.getMapAsync(this)
		
		
		// safe to call every time
		createNotificationChannel()
		
		// start accepting location update broadcasts
		broadcastReceiverIntentFilter.addAction(C.LOCATION_UPDATE_ACTION)
		broadcastReceiverIntentFilter.addAction(C.REPLY_CP_LOCATIONS)
		broadcastReceiverIntentFilter.addAction(C.REPLY_POINTS_LOCATIONS)
		broadcastReceiverIntentFilter.addAction(C.REPLY_WP_LOCATION)
		
		
		// some compass things
		compass = findViewById(R.id.imageViewCompass)
		sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
		magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
		
		locationServiceActive = LocationService.isServiceCreated()
	}
	
	// ============================================== LIFECYCLE CALLBACKS =============================================
	override fun onStart() {
		Log.d(TAG, "onStart")
		
		locationServiceActive = LocationService.isServiceCreated()
		Log.d(TAG, "$locationServiceActive")
		
		
		super.onStart()
	}
	
	override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		
		if (savedInstanceState.getBoolean("lastPosExists")) {
			lastPos = LatLng(
				savedInstanceState.getDouble("lastPosLat"),
				savedInstanceState.getDouble("lastPosLng")
			)
		}
		
		compassEnabled = savedInstanceState.getBoolean("compassEnabled")
		changeCompass()
		
		movementCentered = savedInstanceState.getBoolean("movementCentered")
		
		rotationLock = savedInstanceState.getInt("rotationLock")
		
		if (::mMap.isInitialized) {
			changeMapCenter()
			changeRotationLock()
		} else {
			waitingForMap = true
		}
		
		super.onRestoreInstanceState(savedInstanceState)
	}
	
	override fun onResume() {
		Log.d(TAG, "onResume")
		super.onResume()
		
		LocalBroadcastManager.getInstance(this)
			.registerReceiver(broadcastReceiver, broadcastReceiverIntentFilter)
		
		// some compass things
		sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
		sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME)
		
		if (locationServiceActive) {
			buttonTrack.text = resources.getString(R.string.trackStop)
			
			Log.d(TAG, "Sending intents for map data requests")
			sendBroadcast(Intent(C.REQUEST_WP_LOCATION))
			sendBroadcast(Intent(C.REQUEST_CP_LOCATIONS))
			sendBroadcast(Intent(C.REQUEST_POINTS_LOCATIONS))
		}
	}
	
	override fun onSaveInstanceState(outState: Bundle) {
		// lastPos: LatLng? = null
		outState.putBoolean("lastPosExists", lastPos != null)
		if (lastPos != null) {
			outState.putDouble("lastPosLat", lastPos!!.latitude)
			outState.putDouble("lastPosLng", lastPos!!.longitude)
		}
		
		// compassEnabled = false
		outState.putBoolean("compassEnabled", compassEnabled)
		
		// movementCentered = false
		outState.putBoolean("movementCentered", movementCentered)
		
		// rotationLock = 0
		outState.putInt("rotationLock", rotationLock)
		
		super.onSaveInstanceState(outState)
	}
	
	override fun onPause() {
		Log.d(TAG, "onPause")
		super.onPause()
		
		// some compass things
		sensorManager.unregisterListener(this, accelerometer)
		sensorManager.unregisterListener(this, magnetometer)
	}
	
	override fun onStop() {
		Log.d(TAG, "onStop")
		super.onStop()
		
		LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
	}
	
	override fun onDestroy() {
		Log.d(TAG, "onDestroy")
		super.onDestroy()
	}
	
	override fun onRestart() {
		Log.d(TAG, "onRestart")
		super.onRestart()
	}
	
	// ============================================== NOTIFICATION CHANNEL CREATION =============================================
	private fun createNotificationChannel() {
		// when on 8 Oreo or higher
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = NotificationChannel(
				C.NOTIFICATION_CHANNEL,
				"Default channel",
				NotificationManager.IMPORTANCE_DEFAULT
			)
			
			//.setShowBadge(false).setSound(null, null);
			
			channel.description = "Default channel for OrientMap"
			
			val notificationManager =
				getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			notificationManager.createNotificationChannel(channel)
		}
	}
	
	// ============================================== PERMISSION HANDLING =============================================
	// Returns the current state of the permissions needed.
	private fun checkPermissions(): Boolean {
		return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
			this,
			Manifest.permission.ACCESS_FINE_LOCATION
		)
	}
	
	private fun requestPermissions() {
		val shouldProvideRationale =
			ActivityCompat.shouldShowRequestPermissionRationale(
				this,
				Manifest.permission.ACCESS_FINE_LOCATION
			)
		// Provide an additional rationale to the user. This would happen if the user denied the
		// request previously, but didn't check the "Don't ask again" checkbox.
		if (shouldProvideRationale) {
			Log.i(
				TAG,
				"Displaying permission rationale to provide additional context."
			)
			Snackbar.make(
				findViewById(R.id.activity_main),
				"Hey, i really need to access GPS!",
				Snackbar.LENGTH_INDEFINITE
			)
				.setAction("OK", View.OnClickListener {
					// Request permission
					ActivityCompat.requestPermissions(
						this,
						arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
						C.REQUEST_PERMISSIONS_REQUEST_CODE
					)
				})
				.show()
		} else {
			Log.i(TAG, "Requesting permission")
			// Request permission. It's possible this can be auto answered if device policy
			// sets the permission in a given state or the user denied the permission
			// previously and checked "Never ask again".
			ActivityCompat.requestPermissions(
				this,
				arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
				C.REQUEST_PERMISSIONS_REQUEST_CODE
			)
		}
	}
	
	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		Log.i(TAG, "onRequestPermissionResult")
		if (requestCode == C.REQUEST_PERMISSIONS_REQUEST_CODE) {
			if (grantResults.count() <= 0) { // If user interaction was interrupted, the permission request is cancelled and you receive empty arrays.
				Log.i(TAG, "User interaction was cancelled.")
				Toast.makeText(this, "User interaction was cancelled.", Toast.LENGTH_SHORT).show()
			} else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {// Permission was granted.
				Log.i(TAG, "Permission was granted")
				Toast.makeText(this, "Permission was granted", Toast.LENGTH_SHORT).show()
			} else { // Permission denied.
				Snackbar.make(
					findViewById(R.id.activity_main),
					"You denied GPS! What can I do?",
					Snackbar.LENGTH_INDEFINITE
				)
					.setAction("Settings", View.OnClickListener {
						// Build intent that displays the App settings screen.
						val intent = Intent()
						intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
						val uri: Uri = Uri.fromParts(
							"package",
							BuildConfig.APPLICATION_ID, null
						)
						intent.data = uri
						intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
						startActivity(intent)
					})
					.show()
			}
		}
	}
	
	// ============================================== MAP CHANGERS =============================================
	
	private fun changeMapCenter() {
		val colour: Int
		if (movementCentered) {
			colour = R.color.colorSelf
			mMap.uiSettings.isScrollGesturesEnabled = false
		} else {
			colour = R.color.design_default_color_on_secondary
			mMap.uiSettings.isScrollGesturesEnabled = true
		}
		changeTint(imageButtonMainCenter.drawable, colour)
	}
	
	private fun changeCompass() {
		val colour: Int
		if (compassEnabled) {
			colour = R.color.colorCompass
			compass.visibility = View.VISIBLE
		} else {
			colour = R.color.design_default_color_on_secondary
			compass.clearAnimation()
			compass.visibility = View.GONE
		}
		changeTint(imageButtonMainCompass.drawable, colour)
	}
	
	private fun changeRotationLock() {
		var colour = 0
		when (rotationLock) {
			0 -> {
				colour = R.color.design_default_color_on_secondary
				mMap.uiSettings.isRotateGesturesEnabled = true
			}
			1 -> {
				colour = R.color.colorNorth
				mMap.uiSettings.isRotateGesturesEnabled = false
				mMap.animateCamera(
					CameraUpdateFactory.newCameraPosition(
						CameraPosition.builder()
							.bearing(0F)
							.target(mMap.cameraPosition.target)
							.tilt(mMap.cameraPosition.tilt)
							.zoom(mMap.cameraPosition.zoom)
							.build()
					)
				)
			}
			2 -> {
				colour = R.color.colorSelf
			}
		}
		changeTint(imageButtonMainRotation.drawable, colour)
	}
	
	// ============================================== CLICK HANDLERS =============================================
	fun buttonStartStopOnClick(view: View) {
		Log.d(TAG, "buttonStartStopOnClick. locationServiceActive: $locationServiceActive")
		// try to start/stop the background service
		
		if (locationServiceActive) {
			// stopping the service
			stopService(Intent(this, LocationService::class.java))
			
			buttonTrack.text = resources.getString(R.string.trackStart)
		} else {
			if (checkPermissions()) {
				mMap.isMyLocationEnabled = true
				if (Build.VERSION.SDK_INT >= 26) {
					// starting the FOREGROUND service
					// service has to display non-dismissable notification within 5 secs
					startForegroundService(Intent(this, LocationService::class.java))
				} else {
					startService(Intent(this, LocationService::class.java))
				}
				buttonTrack.text = resources.getString(R.string.trackStop)
				points = ArrayList()
			}
		}
		locationServiceActive = !locationServiceActive
	}
	
	fun buttonWPOnClick(view: View) {
		Log.d(TAG, "buttonWPOnClick")
		wp?.remove()
		if (lastPos != null) {
			wp = mMap.addMarker(MarkerOptions().position(lastPos!!).icon(BitmapDescriptorFactory.fromResource(R.drawable.baseline_arrow_downward_black_36)))
		}
		sendBroadcast(Intent(C.WP_ADD_TO_CURRENT))
	}
	
	fun buttonCPOnClick(view: View) {
		Log.d(TAG, "buttonCPOnClick")
		if (lastPos != null) {
			mMap.addMarker(MarkerOptions().position(lastPos!!).icon(BitmapDescriptorFactory.fromResource(R.drawable.baseline_beenhere_black_36)))
		}
		sendBroadcast(Intent(C.CP_ADD_TO_CURRENT))
	}
	
	fun buttonClear(view: View) {
		if (!locationServiceActive) {
			mMap.clear()
		}
		// TODO: add a confirmation message
	}
	
	fun buttonRotationCycle(view: View) {
		// rotation mode, 0 free to rota, 1 northbound, 2 selfbound
		rotationLock = (rotationLock + 1) % 3
		changeRotationLock()
	}
	
	fun buttonCompassToggle(view: View) {
		compassEnabled = !compassEnabled
		changeCompass()
	}
	
	fun buttonMenu(view: View) {
		// TODO: bring up menu view for future things
	}
	
	fun buttonCenterToggle(view: View) {
		movementCentered = !movementCentered
		changeMapCenter()
	}
	
	// ============================================== BROADCAST RECEIVER =============================================
	private inner class InnerBroadcastReceiver : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			Log.d(TAG, intent!!.action!!)
			when (intent.action) {
				C.LOCATION_UPDATE_ACTION -> {
					lastPos = LatLng(
						intent.getDoubleExtra(C.LOCATION_UPDATE_ACTION_LATITUDE, 0.0),
						intent.getDoubleExtra(C.LOCATION_UPDATE_ACTION_LONGITUDE, 0.0)
					)
					points.add(lastPos!!)
					polyline!!.points = points
					
					if (movementCentered) {
						mMap.animateCamera(CameraUpdateFactory.newLatLng(lastPos))
					}
					textViewStart1.text = intent.getStringExtra(C.LOCATION_UPDATE_ACTION_OVERALL_DISTANCE)
					textViewStart2.text = intent.getStringExtra(C.LOCATION_UPDATE_ACTION_OVERALL_TIME)
					textViewStart3.text = intent.getStringExtra(C.LOCATION_UPDATE_ACTION_OVERALL_PACE)
					
					textViewWp1.text = intent.getStringExtra(C.LOCATION_UPDATE_ACTION_WP_TOTAL_DISTANCE)
					textViewWp2.text = intent.getStringExtra(C.LOCATION_UPDATE_ACTION_WP_DIRECT_DISTANCE)
					textViewWp3.text = intent.getStringExtra(C.LOCATION_UPDATE_ACTION_WP_PACE)
					
					textViewCp1.text = intent.getStringExtra(C.LOCATION_UPDATE_ACTION_CP_TOTAL_DISTANCE)
					textViewCp2.text = intent.getStringExtra(C.LOCATION_UPDATE_ACTION_CP_DIRECT_DISTANCE)
					textViewCp3.text = intent.getStringExtra(C.LOCATION_UPDATE_ACTION_CP_PACE)
				}
				C.REPLY_CP_LOCATIONS -> {
					mMap.setOnMapLoadedCallback {
						intent
							.getParcelableArrayListExtra<Location>(C.GENERAL_LOCATIONS)!!
							.forEach { x -> mMap.addMarker(MarkerOptions().position(LatLng(x.latitude, x.longitude)).icon(BitmapDescriptorFactory.fromResource(R.drawable.baseline_beenhere_black_36))) }
					}
				}
				C.REPLY_WP_LOCATION -> {
					val temp = intent.getParcelableExtra<Location>(C.GENERAL_LOCATION)
					
					if (temp != null) {
						if (::mMap.isInitialized) {
							wp = mMap.addMarker(MarkerOptions().position(LatLng(temp.latitude, temp.longitude)).icon(BitmapDescriptorFactory.fromResource(R.drawable.baseline_arrow_downward_black_36)))
						} else {
							tempWp = LatLng(temp.latitude, temp.longitude)
							tempWpInit = true
						}
					}
				}
				C.REPLY_POINTS_LOCATIONS -> {
					intent
						.getParcelableArrayListExtra<Location>(C.GENERAL_LOCATIONS)!!
						.forEach { x -> points.add(LatLng(x.latitude, x.longitude)) }
				}
			}
		}
	}
	
	// ============================================== COMPASS METHODS =============================================
	private fun lowPass(input: FloatArray, output: FloatArray) {
		val alpha = 0.05f
		
		for (i in input.indices) {
			output[i] = output[i] + alpha * (input[i] - output[i])
		}
	}
	
	override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
	
	override fun onSensorChanged(event: SensorEvent) {
		if (event.sensor === accelerometer) {
			lowPass(event.values, lastAccelerometer)
			lastAccelerometerSet = true
		} else if (event.sensor === magnetometer) {
			lowPass(event.values, lastMagnetometer)
			lastMagnetometerSet = true
		}
		
		if (lastAccelerometerSet && lastMagnetometerSet) {
			val r = FloatArray(9)
			if (SensorManager.getRotationMatrix(r, null, lastAccelerometer, lastMagnetometer)) {
				val orientation = FloatArray(3)
				SensorManager.getOrientation(r, orientation)
				val degree = (Math.toDegrees(orientation[0].toDouble()) + 360).toFloat() % 360
				if (rotationLock == 2) {
					if (::mMap.isInitialized) {
						mMap.moveCamera(
							CameraUpdateFactory.newCameraPosition(
								CameraPosition.builder()
									.bearing(degree)
									.target(mMap.cameraPosition.target)
									.tilt(mMap.cameraPosition.tilt)
									.zoom(mMap.cameraPosition.zoom)
									.build()
							)
						)
					}
				}
				if (compassEnabled) {
					val rotateAnimation = RotateAnimation(
						currentDegree,
						-degree,
						Animation.RELATIVE_TO_SELF, 0.5f,
						Animation.RELATIVE_TO_SELF, 0.5f
					)
					rotateAnimation.duration = 1000
					rotateAnimation.fillAfter = true
					
					compass.startAnimation(rotateAnimation)
				}
				currentDegree = -degree
			}
		}
	}
	
	// ============================================== HELPERS =============================================
	private fun changeTint(drawable: Drawable, colour: Int) {
		DrawableCompat.setTint(DrawableCompat.wrap(drawable), ContextCompat.getColor(this, colour))
	}
	
}

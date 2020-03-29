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
	
	// broadcast vals
	private val broadcastReceiver = InnerBroadcastReceiver()
	private val broadcastReceiverIntentFilter: IntentFilter = IntentFilter()
	
	// location service bool
	private var locationServiceActive = false
	
	// compassenabled bool
	private var compassEnabled = false
	
	// movement center
	private var movementCentered = false
	
	// rotation mode, 0 free to rota, 1 northbound, 2 selfbound
	private var rotationLock = 0
	
	private var wp: Marker? = null
	private var points = ArrayList<LatLng>()
	
	//// compass related vars
	lateinit var sensorManager: SensorManager
	lateinit var compass: ImageView
	lateinit var accelerometer: Sensor
	lateinit var magnetometer: Sensor
	
	var currentDegree = 0.0f
	var lastAccelerometer = FloatArray(3)
	var lastMagnetometer = FloatArray(3)
	var lastAccelerometerSet = false
	var lastMagnetometerSet = false
	//// end of compass related vars
	
	companion object {
		// tag for logging
		private val TAG = this::class.java.declaringClass!!.simpleName
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
		mMap.isMyLocationEnabled = true                     // enable blue dot
		mMap.uiSettings.isCompassEnabled = false            // disable gmap compass
		mMap.uiSettings.isMyLocationButtonEnabled = false   // disable gmap center
		
		// Add a marker in Sydney and move the camera
//        val sydney = LatLng(-34.0, 151.0)
//        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
//        mMap.cameraPosition
	}
	
	
	// ============================================== MAIN ENTRY - ONCREATE =============================================
	override fun onCreate(savedInstanceState: Bundle?) {
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
		
		if (!checkPermissions()) {
			requestPermissions()
		}
		
		// start accepting location update broadcasts
		broadcastReceiverIntentFilter.addAction(C.LOCATION_UPDATE_ACTION)
		
		
		// some compass things
		compass = findViewById(R.id.imageViewCompass) as ImageView
		sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
		magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
	}
	
	// ============================================== LIFECYCLE CALLBACKS =============================================
	override fun onStart() {
		Log.d(TAG, "onStart")
		super.onStart()
	}
	
	override fun onResume() {
		Log.d(TAG, "onResume")
		super.onResume()
		
		LocalBroadcastManager.getInstance(this)
			.registerReceiver(broadcastReceiver, broadcastReceiverIntentFilter)
		
		// some compass things
		sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
		sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME)
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
			);
			
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
	
	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<out String>,
		grantResults: IntArray
	) {
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
	
	// ============================================== CLICK HANDLERS =============================================
	fun buttonStartStopOnClick(view: View) {
		Log.d(TAG, "buttonStartStopOnClick. locationServiceActive: $locationServiceActive")
		// try to start/stop the background service
		
		if (locationServiceActive) {
			// stopping the service
			stopService(Intent(this, LocationService::class.java))
			
			buttonTrack.text = "START"
		} else {
			if (Build.VERSION.SDK_INT >= 26) {
				// starting the FOREGROUND service
				// service has to display non-dismissable notification within 5 secs
				startForegroundService(Intent(this, LocationService::class.java))
			} else {
				startService(Intent(this, LocationService::class.java))
			}
			buttonTrack.text = "STOP"
			polyline = mMap.addPolyline(PolylineOptions().width(10F).color(Color.RED))
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
		var colour = 0
		// rotation mode, 0 free to rota, 1 northbound, 2 selfbound
		rotationLock = (rotationLock + 1) % 3
		when (rotationLock) {
			0 -> {
				colour = R.color.design_default_color_on_secondary
				// TODO: enable rotation, remove force
			}
			1 -> {
				colour = R.color.colorNorth
				// TODO: disable rotation, force north
			}
			2 -> {
				colour = R.color.colorSelf
				// TODO: disable rotation, force blue dot direction?
			}
		}
		changeTint(imageButtonMainRotation.drawable, colour)
		
	}
	
	fun buttonCompassToggle(view: View) {
		val colour: Int
		compassEnabled = !compassEnabled
		if (compassEnabled) {
			colour = R.color.colorCompass
			compass.visibility = View.VISIBLE
		} else {
			colour = R.color.design_default_color_on_secondary
			compass.clearAnimation()
			compass.visibility = View.GONE
			// TODO: hide compass
		}
		changeTint(imageButtonMainCompass.drawable, colour)
		
	}
	
	fun buttonMenu(view: View) {
		// TODO: bring up menu view for future things
	}
	
	fun buttonCenterToggle(view: View) {
		val colour: Int
		movementCentered = !movementCentered
		if (movementCentered) {
			colour = R.color.colorSelf
			// TODO: disable panning and keep centered
		} else {
			colour = R.color.design_default_color_on_secondary
			// TODO: enable panning and don't center
		}
		changeTint(imageButtonMainCenter.drawable, colour)
	}
	
	// ============================================== BROADCAST RECEIVER =============================================
	private inner class InnerBroadcastReceiver : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			Log.d(TAG, intent!!.action)
			when (intent!!.action) {
				C.LOCATION_UPDATE_ACTION -> {
					lastPos = LatLng(
						intent.getDoubleExtra(C.LOCATION_UPDATE_ACTION_LATITUDE, 0.0),
						intent.getDoubleExtra(C.LOCATION_UPDATE_ACTION_LONGITUDE, 0.0)
					)
					points.add(lastPos!!)
					polyline!!.points = points
					
					
					
					if (movementCentered) {
						mMap.moveCamera(CameraUpdateFactory.newLatLng(lastPos))
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
			}
		}
	}
	
	
	// ============================================== COMPASS METHODS =============================================
	fun lowPass(input: FloatArray, output: FloatArray) {
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
		
		if (compassEnabled && lastAccelerometerSet && lastMagnetometerSet) {
			val r = FloatArray(9)
			if (SensorManager.getRotationMatrix(r, null, lastAccelerometer, lastMagnetometer)) {
				val orientation = FloatArray(3)
				SensorManager.getOrientation(r, orientation)
				val degree = (Math.toDegrees(orientation[0].toDouble()) + 360).toFloat() % 360
				
				val rotateAnimation = RotateAnimation(
					currentDegree,
					-degree,
					Animation.RELATIVE_TO_SELF, 0.5f,
					Animation.RELATIVE_TO_SELF, 0.5f
				)
				rotateAnimation.duration = 1000
				rotateAnimation.fillAfter = true
				
				compass.startAnimation(rotateAnimation)
				currentDegree = -degree
			}
		}
	}
	
	// ============================================== HELPERS =============================================
	fun changeTint(drawable: Drawable, colour: Int) {
		DrawableCompat.setTint(DrawableCompat.wrap(drawable), ContextCompat.getColor(this, colour))
	}
	
}

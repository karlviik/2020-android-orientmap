package ee.taltech.orientmap.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ee.taltech.orientmap.DataRecyclerViewAdapterSessions
import ee.taltech.orientmap.R
import ee.taltech.orientmap.db.LocationRepository
import ee.taltech.orientmap.db.SessionRepository
import ee.taltech.orientmap.utils.ApiUtils
import ee.taltech.orientmap.utils.PreferenceUtils
import kotlinx.android.synthetic.main.activity_session_view.*

class SessionViewActivity : AppCompatActivity() {
	
	private lateinit var sessionRepository: SessionRepository
	private lateinit var locationRepository: LocationRepository
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_session_view)
		
		sessionRepository = SessionRepository(this).open()
		locationRepository = LocationRepository(this).open()
		
		recyclerViewSessions.layoutManager = LinearLayoutManager(this)
		recyclerViewSessions.adapter = DataRecyclerViewAdapterSessions(this, sessionRepository, locationRepository)
		
		editTextFastTime.setText(PreferenceUtils.getFastSpeedTime(this).toString(), TextView.BufferType.EDITABLE)
		editTextSlowTime.setText(PreferenceUtils.getSlowSpeedTime(this).toString(), TextView.BufferType.EDITABLE)
		editTextGpsInterval.setText(PreferenceUtils.getGpsUpdateInterval(this).toString(), TextView.BufferType.EDITABLE)
		editTextSyncInterval.setText(PreferenceUtils.getDefaultSyncInterval(this).toString(), TextView.BufferType.EDITABLE)
		
		if (PreferenceUtils.isLoggedIn(this)) {
			buttonLoginOrLogout.text = "Logout"
		} else {
			buttonLoginOrLogout.text = "Login"
		}
	}
	
	override fun onDestroy() {
		super.onDestroy()
		sessionRepository.close()
	}
	
	fun onClickLoginLogout(view: View) {
		if (PreferenceUtils.isLoggedIn(this)) {
			ApiUtils.cancelAllRequests(this)
			PreferenceUtils.setLoggedIn(this, false)
			PreferenceUtils.setUserEmail(this, null)
			PreferenceUtils.setToken(this, null)
			buttonLoginOrLogout.text = "Login"
		} else {
			val intent = Intent(this, LoginActivity::class.java)
			startActivity(intent)
			this.finish()
		}
	}
	
	fun onClickSaveSettings(view: View) {
		// hide kb
		val inputManager: InputMethodManager? = this.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
		inputManager?.hideSoftInputFromWindow(view.windowToken, 0)
		
		val fastTime = editTextFastTime.text.toString().toIntOrNull()
		val slowTime = editTextSlowTime.text.toString().toIntOrNull()
		val gpsInterval = editTextGpsInterval.text.toString().toIntOrNull()
		val syncInterval = editTextSyncInterval.text.toString().toIntOrNull()
		if (fastTime == null || slowTime == null || gpsInterval == null || syncInterval == null ||
			fastTime < 0 || slowTime < 0 || gpsInterval < 0 || syncInterval < 0
		) {
			Toast.makeText(this, "Invalid settings!", Toast.LENGTH_SHORT).show()
			return
		}
		if (fastTime >= slowTime) {
			Toast.makeText(this, "Fast time has to be less than slow time!", Toast.LENGTH_SHORT).show()
			return
		}
		if (gpsInterval == 0) {
			Toast.makeText(this, "GPS interval can't be 0!", Toast.LENGTH_SHORT).show()
			return
		}
		PreferenceUtils.setDefaultSyncInterval(this, syncInterval)
		PreferenceUtils.setGpsUpdateInterval(this, gpsInterval)
		PreferenceUtils.setFastSpeedTime(this, fastTime)
		PreferenceUtils.setSlowSpeedTime(this, slowTime)
		Toast.makeText(this, "Settings saved! Will take effect next session", Toast.LENGTH_SHORT).show()
	}
}

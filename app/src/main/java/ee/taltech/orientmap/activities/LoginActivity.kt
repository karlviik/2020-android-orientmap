package ee.taltech.orientmap.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import ee.taltech.orientmap.R

class LoginActivity : AppCompatActivity() {
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_login)
	}
	
	fun onClickMoveToRegister(view: View) {}
	fun onClickPerformLogin(view: View) {}
}

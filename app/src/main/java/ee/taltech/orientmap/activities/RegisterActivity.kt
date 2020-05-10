package ee.taltech.orientmap.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import ee.taltech.orientmap.R

class RegisterActivity : AppCompatActivity() {
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_register)
	}
	
	fun onClickPerformRegister(view: View) {}
}

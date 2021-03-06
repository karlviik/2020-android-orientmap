package ee.taltech.orientmap.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import ee.taltech.orientmap.R
import ee.taltech.orientmap.utils.ApiUtils
import ee.taltech.orientmap.utils.PreferenceUtils
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_login)
	}
	
	fun onClickMoveToRegister(view: View) {
		val intent = Intent(this, RegisterActivity::class.java)
		startActivity(intent)
	}
	
	fun onClickPerformLogin(view: View) {
		// hide kb
		val inputManager: InputMethodManager? = this.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
		inputManager?.hideSoftInputFromWindow(view.windowToken, 0)
		val password = editTextPassword.text.toString()
		val email = editTextEmail.text.toString()
		val listener = Response.Listener<JSONObject> { response ->
			Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
			val token = response.getString("token")
			PreferenceUtils.setUserEmail(this, email)
			PreferenceUtils.setLoggedIn(this, true)
			PreferenceUtils.setToken(this, token)
			this.finish()
		}
		
		val errorListener = Response.ErrorListener { error ->
			// TODO: maybe break down different errors?
			Toast.makeText(this, "Error trying to log in!\n" + error, Toast.LENGTH_SHORT).show()
		}
		ApiUtils.loginUser(this, listener, errorListener, email, password)
		
	}
}

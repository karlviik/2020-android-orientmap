package ee.taltech.orientmap.activities

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.android.volley.Response
import ee.taltech.orientmap.R
import ee.taltech.orientmap.utils.ApiUtils
import kotlinx.android.synthetic.main.activity_register.view.*
import org.json.JSONObject

class RegisterActivity : AppCompatActivity() {
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_register)
	}
	
	fun onClickPerformRegister(view: View) {
		val password = view.editTextPassword.text.toString()
		val email = view.editTextEmail.text.toString()
		val firstName = view.editTextFirstName.text.toString()
		val lastName = view.editTextLastName.text.toString()
		if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName) || TextUtils.isEmpty(email)) {
			Toast.makeText(this, "All fields must be filled!", Toast.LENGTH_SHORT).show()
			return
		}
		if (password.length < 6) {
			Toast.makeText(this, "Password must be at least 6 characters!", Toast.LENGTH_SHORT).show()
			return
		}
		// hide keyboard, I think
		val inputManager: InputMethodManager? = this.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
		inputManager?.hideSoftInputFromWindow(view.windowToken, 0)
		
		val listener = Response.Listener<JSONObject> { response ->
			Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
			this.finish()
		}
		val errorListener = Response.ErrorListener { error ->
			// TODO: maybe break down different errors?
			Toast.makeText(this, "Error creating an account!\n" + error, Toast.LENGTH_SHORT).show()
		}
		ApiUtils.createUser(this, listener, errorListener, firstName, lastName, email, password)
		
	}
}

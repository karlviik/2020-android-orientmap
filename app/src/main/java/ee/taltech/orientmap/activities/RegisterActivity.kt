package ee.taltech.orientmap.activities

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import ee.taltech.orientmap.R
import ee.taltech.orientmap.utils.ApiUtils
import kotlinx.android.synthetic.main.activity_register.*
import org.json.JSONObject

class RegisterActivity : AppCompatActivity() {
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_register)
	}
	
	fun onClickPerformRegister(view: View) {
		val password = editTextPassword.text.toString()
		val email = editTextEmail.text.toString()
		val firstName = editTextFirstName.text.toString()
		val lastName = editTextLastName.text.toString()
		if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName) || TextUtils.isEmpty(email)) {
			Toast.makeText(this, "All fields must be filled!", Toast.LENGTH_SHORT).show()
			return
		}
		if (password.length < 6) {
			Toast.makeText(this, "Password must be at least 6 characters!", Toast.LENGTH_SHORT).show()
			return
		}
		if (!password.contains(Regex("[0-9]")) || !password.contains(Regex("[A-Z]")) || !password.contains(Regex("[^a-zA-Z\\d]"))) {
			Toast.makeText(this, "Password must have at least 1 num, 1 uppercase and 1 non alphanumeric character", Toast.LENGTH_SHORT).show()
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
			Toast.makeText(this, "Error creating an account!\n" + error.message, Toast.LENGTH_SHORT).show()
		}
		ApiUtils.createUser(this, listener, errorListener, firstName, lastName, email, password)
		
	}
}

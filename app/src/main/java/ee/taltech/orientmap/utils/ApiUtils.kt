package ee.taltech.orientmap.utils

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import ee.taltech.orientmap.WebApiSingletonHandler
import ee.taltech.orientmap.poko.LocationModel
import ee.taltech.orientmap.poko.SessionModel
import org.json.JSONObject

class ApiUtils {
	companion object {
		private val TAG = this::class.java.declaringClass!!.simpleName
		private const val REST_BASE_URL = "https://sportmap.akaver.com/api/v1.0/"
		private var mJwt : String? = null
		
		fun createUser(context: Context, listener: Response.Listener<JSONObject>, errorListener: Response.ErrorListener, firstName: String, lastName: String, email: String, password: String) {
			Log.d(TAG, "createUser")
			
			var handler = WebApiSingletonHandler.getInstance(context)
			val requestJsonParams = JSONObject()
			
			requestJsonParams.put("firstName", firstName)
			requestJsonParams.put("lastName", lastName)
			requestJsonParams.put("email", email)
			requestJsonParams.put("password", password)
			
			var httpRequest = JsonObjectRequest(
				Request.Method.POST,
				REST_BASE_URL + "Account/Register",
				requestJsonParams,
				listener,
				errorListener
			)
			handler.addToRequestQueue(httpRequest)
			
		}
		
		fun loginUser(context: Context, listener: Response.Listener<JSONObject>, errorListener: Response.ErrorListener, email: String, password: String) {
			Log.d(TAG, "loginUser")
			
			val handler = WebApiSingletonHandler.getInstance(context)
			val requestJsonParams = JSONObject()
			
			requestJsonParams.put("email", email)
			requestJsonParams.put("password", password)
			
			val httpRequest = JsonObjectRequest(
				Request.Method.POST,
				REST_BASE_URL + "Account/Login",
				requestJsonParams,
				listener,
				errorListener
			)
			
			handler.addToRequestQueue(httpRequest)
		}
		
		fun createSession(context: Context, listener: Response.Listener<JSONObject>, errorListener: Response.ErrorListener, session: SessionModel, jwt: String) {
			Log.d(TAG, "createSession")
			
			var handler = WebApiSingletonHandler.getInstance(context)
			val requestJsonParams = JSONObject()
			
			requestJsonParams.put("name", session.name)
			requestJsonParams.put("description", session.name)
			requestJsonParams.put("recordedAt", session.start.toString())
			
			var httpRequest = object : JsonObjectRequest(
				Request.Method.POST,
				REST_BASE_URL + "GpsSessions",
				requestJsonParams,
				listener,
				errorListener
			) {
				override fun getHeaders(): MutableMap<String, String> {
					val headers = HashMap<String, String>()
					for ((key, value) in super.getHeaders()) {
						headers[key] = value
					}
					headers["Authorization"] = "Bearer $jwt"
					return headers
				}
			}
			handler.addToRequestQueue(httpRequest)
			
		}
		
		
		fun createLocation(context: Context, listener: Response.Listener<JSONObject>, errorListener: Response.ErrorListener, location: LocationModel, sessionId: String, jwt: String) {
			Log.d(TAG, "createLocation")
			
			val handler = WebApiSingletonHandler.getInstance(context)
			val requestJsonParams = JSONObject()
			
			requestJsonParams.put("recordedAt", location.recordedAt.toString())
			requestJsonParams.put("latitude", location.latitude)
			requestJsonParams.put("longitude", location.longitude)
			requestJsonParams.put("accuracy", location.accuracy)
			requestJsonParams.put("altitude", location.altitude)
			requestJsonParams.put("verticalAccuracy", location.verticalAccuracy)
			requestJsonParams.put("gpsSessionId", sessionId)
			requestJsonParams.put("gpsLocationTypeId", Utils.getLocationTypeStringBasedOnId(location.locationType))
			
			val httpRequest = object : JsonObjectRequest(
				Request.Method.POST,
				REST_BASE_URL + "GpsLocations",
				requestJsonParams,
				listener,
				errorListener
			) {
				override fun getHeaders(): MutableMap<String, String> {
					val headers = HashMap<String, String>()
					for ((key, value) in super.getHeaders()) {
						headers[key] = value
					}
					headers["Authorization"] = "Bearer $jwt"
					return headers
				}
			}
			handler.addToRequestQueue(httpRequest)
			
		}
		
		fun cancelAllRequests(context: Context) {
			val handler = WebApiSingletonHandler.getInstance(context)
			handler.cancelPendingRequests()
		}
	}
	

}
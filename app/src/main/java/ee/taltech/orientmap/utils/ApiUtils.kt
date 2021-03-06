package ee.taltech.orientmap.utils

import android.content.Context
import android.util.Log
import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.JsonRequest
import ee.taltech.orientmap.WebApiSingletonHandler
import ee.taltech.orientmap.poko.LocationModel
import ee.taltech.orientmap.poko.SessionModel
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

class ApiUtils {
	companion object {
		private val TAG = this::class.java.declaringClass!!.simpleName
		private const val REST_BASE_URL = "https://sportmap.akaver.com/api/v1.0/"
		
		fun createUser(context: Context, listener: Response.Listener<JSONObject>, errorListener: Response.ErrorListener, firstName: String, lastName: String, email: String, password: String) {
			Log.d(TAG, "createUser")
			
			val handler = WebApiSingletonHandler.getInstance(context)
			val requestJsonParams = JSONObject()
			
			requestJsonParams.put("firstName", firstName)
			requestJsonParams.put("lastName", lastName)
			requestJsonParams.put("email", email)
			requestJsonParams.put("password", password)
			
			val httpRequest = JsonObjectRequest(
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
			
			val handler = WebApiSingletonHandler.getInstance(context)
			val requestJsonParams = JSONObject()
			
			requestJsonParams.put("name", session.name)
			requestJsonParams.put("description", session.name)
			requestJsonParams.put("recordedAt", session.start.toString())
			requestJsonParams.put("PaceMin", if (session.gradientFastTime != null) session.gradientFastTime else PreferenceUtils.getFastSpeedTime(context))
			requestJsonParams.put("PaceMax", if (session.gradientSlowTime != null) session.gradientSlowTime else PreferenceUtils.getSlowSpeedTime(context))
			
			
			val httpRequest = object : JsonObjectRequest(
				Method.POST,
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
		
		fun updateSession(context: Context, listener: Response.Listener<JSONObject>, errorListener: Response.ErrorListener, session: SessionModel, token: String) {
			val handler = WebApiSingletonHandler.getInstance(context)
			val requestJsonParams = JSONObject()
			requestJsonParams.put("id", session.apiId)
			requestJsonParams.put("name", session.name)
			requestJsonParams.put("description", session.name)
			requestJsonParams.put("paceMin", if (session.gradientFastTime != null) session.gradientFastTime else PreferenceUtils.getFastSpeedTime(context))
			requestJsonParams.put("paceMax", if (session.gradientSlowTime != null) session.gradientSlowTime else PreferenceUtils.getSlowSpeedTime(context))
			requestJsonParams.put("gpsSessionTypeId", "00000000-0000-0000-0000-000000000001") // why is this mandatory?
			
			
			val httpRequest = object : JsonObjectRequest(
				Method.PUT,
				REST_BASE_URL + "GpsSessions/" + session.apiId,
				requestJsonParams,
				listener,
				errorListener
			) {
				override fun getHeaders(): MutableMap<String, String> {
					val headers = HashMap<String, String>()
					for ((key, value) in super.getHeaders()) {
						headers[key] = value
					}
					headers["Authorization"] = "Bearer $token"
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
				Method.POST,
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
		
		fun createLocations(context: Context, listener: Response.Listener<JSONObject>, errorListener: Response.ErrorListener, locations: Collection<LocationModel>, sessionId: String, jwt: String) {
			val handler = WebApiSingletonHandler.getInstance(context)
			val requestJsonParams = JSONArray()
			for (location in locations) {
				val item = JSONObject()
				item.put("recordedAt", location.recordedAt.toString())
				item.put("latitude", location.latitude)
				item.put("longitude", location.longitude)
				item.put("accuracy", location.accuracy)
				item.put("altitude", location.altitude)
				item.put("verticalAccuracy", location.verticalAccuracy)
				item.put("gpsSessionId", sessionId)
				item.put("gpsLocationTypeId", Utils.getLocationTypeStringBasedOnId(location.locationType))
				requestJsonParams.put(item)
			}
			val httpRequest = object : JsonRequest<JSONObject>(
				Method.POST,
				REST_BASE_URL + "GpsLocations/bulkupload/" + sessionId,
				requestJsonParams.toString(),
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
				
				override fun parseNetworkResponse(response: NetworkResponse?): Response<JSONObject> {
					return try {
						val jsonString = String(
							response!!.data,
							Charset.forName(HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET))
						)
						Response.success(
							JSONObject(jsonString), HttpHeaderParser.parseCacheHeaders(response)
						)
					} catch (e: UnsupportedEncodingException) {
						Response.error(ParseError(e))
					} catch (je: JSONException) {
						Response.error(ParseError(je))
					}
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
package ee.taltech.orientmap

import android.content.Context
import android.text.TextUtils
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley

class WebApiSingletonHandler {
	companion object {
		private val TAG = this::class.java.declaringClass!!.simpleName
		private var instance: WebApiSingletonHandler? = null
		private var mContext: Context? = null
		
		@Synchronized
		fun getInstance(context: Context): WebApiSingletonHandler {
			if (instance == null) {
				instance = WebApiSingletonHandler(context)
			}
			return instance!!
		}
	}
	
	private constructor(context: Context) {
		mContext = context
	}
	
	private var requestQueue: RequestQueue? = null
		get() {
			if (field == null) {
				field = Volley.newRequestQueue(mContext)
			}
			return field
		}
	
	fun <T> addToRequestQueue(request: Request<T>, tag: String) {
		request.tag = if (TextUtils.isEmpty(tag)) TAG else tag
		requestQueue?.add(request)
	}
	
	fun <T> addToRequestQueue(request: Request<T>) {
		request.tag = TAG
		requestQueue?.add(request)
	}
	
	fun cancelPendingRequests(tag: String? = null) {
		if (requestQueue != null) {
			requestQueue!!.cancelAll(if (tag == null || TextUtils.isEmpty(tag)) TAG else tag)
		}
	}
}
package ee.taltech.orientmap.utils

import android.content.Context
import android.content.SharedPreferences
import ee.taltech.orientmap.C

class PreferenceUtils {
	companion object {
		private const val name = "someTotallyRandomNameThatIsSuperRandomAndIShouldMaybeDoThisKindaBetter"
		
		private fun getPreference(context: Context): SharedPreferences {
			return context.getSharedPreferences(name, 0)
		}
		// isLoggedIn, token, user?, fastSpeed, slowSpeed, gpsupdateinterval, syncinginterval
		fun isLoggedIn(context: Context): Boolean {
			return getPreference(context).getBoolean("loggedIn", false)
		}
		
		fun getToken(context: Context): String? {
			return getPreference(context).getString("token", null)
		}
		
		fun getUserFirstName(context: Context): String? {
			return getPreference(context).getString("userFirstName", null)
		}
		
		fun getSlowSpeedTime(context: Context): Int {
			return getPreference(context).getInt("slowSpeedTime", C.DEFAULT_SLOW_SPEED)
		}
		
		fun getFastSpeedTime(context: Context): Int {
			return getPreference(context).getInt("fastSpeedTime", C.DEFAULT_FAST_SPEED)
		}
		
		fun getGpsUpdateInterval(context: Context): Long {
			return getPreference(context).getLong("gpsUpdateIntercal", C.DEFAULT_GPS_UPDATE_INTERVAL_MILLISECONDS)
		}
		
		fun getDefaultSyncInterval(context: Context): Int {
			return getPreference(context).getInt("syncIntercal", C.DEFAULT_SYNC_INTERVAL_SECONDS)
		}
		
		
		
		fun setLoggedIn(context: Context, boolean: Boolean): Boolean {
			return getPreference(context).edit().putBoolean("loggedIn", boolean).commit()
		}
		
		fun setToken(context: Context, token: String?): Boolean {
			return getPreference(context).edit().putString("token", token).commit()
		}
		
		fun setUserFirstName(context: Context, name: String?): Boolean {
			return getPreference(context).edit().putString("userFirstName", name).commit()
		}
		
		fun setSlowSpeedTime(context: Context, time: Int): Boolean {
			return getPreference(context).edit().putInt("slowSpeedTime", time).commit()
		}
		
		fun setFastSpeedTime(context: Context, time: Int): Boolean {
			return getPreference(context).edit().putInt("fastSpeedTime", time).commit()
		}
		
		fun setGpsUpdateInterval(context: Context, interval: Long): Boolean {
			return getPreference(context).edit().putLong("gpsUpdateIntercal", interval).commit()
		}
		
		fun setDefaultSyncInterval(context: Context, interval: Int): Boolean {
			return getPreference(context).edit().putInt("syncIntercal", interval).commit()
		}
	}
}
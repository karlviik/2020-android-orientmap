package ee.taltech.orientmap.utils

import android.location.Location
import ee.taltech.orientmap.C
import kotlin.math.abs

class Utils {
	companion object {
		fun getColorBasedOnGradient(loc1: Location, loc2: Location, fastSpeed: Int, slowSpeed: Int, fastColor: Int, slowColor: Int): Int {
			val distanceInMeters = loc1.distanceTo(loc2)
			val timeDifferenceInSeconds = abs(loc2.time - loc1.time) / 1000.0
			val speedSecondsPerKm = timeDifferenceInSeconds / (distanceInMeters / 1000.0)
			if (speedSecondsPerKm < fastSpeed) {
				return fastColor
			}
			if (speedSecondsPerKm > slowSpeed) {
				return slowColor
			}
			val gradientRangeSeconds = abs(slowSpeed - fastSpeed)
			val convertedSecondsPerKm = (speedSecondsPerKm - fastSpeed).toInt()
			
			val multiplier: Float = convertedSecondsPerKm / gradientRangeSeconds.toFloat()
			
			val sa: Float = (slowColor shr 24 and 0xff) / 255.0f
			val sr: Float = (slowColor shr 16 and 0xff) / 255.0f
			val sg: Float = (slowColor shr 8 and 0xff) / 255.0f
			val sb: Float = (slowColor and 0xff) / 255.0f
			
			val fa: Float = (fastColor shr 24 and 0xff) / 255.0f
			val fr: Float = (fastColor shr 16 and 0xff) / 255.0f
			val fg: Float = (fastColor shr 8 and 0xff) / 255.0f
			val fb: Float = (fastColor and 0xff) / 255.0f
			
			
			val ma: Float = fa + multiplier * (sa - fa)
			val mr: Float = fr + multiplier * (sr - fr)
			val mg: Float = fg + multiplier * (sg - fg)
			val mb: Float = fb + multiplier * (sb - fb)
			
			val ia: Int = (ma * 255).toInt() shl 24
			val ir: Int = (mr * 255).toInt() shl 16
			val ig: Int = (mg * 255).toInt() shl 8
			val ib: Int = (mb * 255).toInt()
			
			return ia + ir + ig + ib
		}
		
		fun getLocationTypeStringBasedOnId(int: Int): String {
			return when(int) {
				1 -> C.API_WP_ID
				2 -> C.API_CP_ID
				else -> C.API_LOCATION_ID
			}
		}
	}
}
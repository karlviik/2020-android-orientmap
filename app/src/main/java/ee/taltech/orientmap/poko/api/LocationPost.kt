package ee.taltech.orientmap.poko.api

import android.location.Location
import android.os.Build
import ee.taltech.orientmap.C
import ee.taltech.orientmap.poko.LocationModel
import org.threeten.bp.LocalDateTime
import org.threeten.bp.OffsetDateTime

class LocationPost {
	var recordedAt: String = ""
	var latitude: Double = 0.0
	var longitude: Double = 0.0
	var accuracy: Float = 0f
	var altitude: Double = 0.0
	var verticalAccuracy: Float = 0f
	var gpsSessionId: String = "" // this one is important I guess
	var gpsLocationTypeId: String = ""
	
	// 0 for location, 1 for wp, 2 for cp
	constructor(location: Location, gpsSessionId: String, gpsLocationTypeIdId: Int) {
		this.recordedAt = LocalDateTime.ofEpochSecond(location.time / 1000, (location.time % 1000 * 1000000).toInt(), OffsetDateTime.now().offset).toString()
		this.latitude = location.latitude
		this.longitude = location.longitude
		this.accuracy = location.accuracy
		this.altitude = location.altitude
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasVerticalAccuracy()) {
			this.verticalAccuracy = location.verticalAccuracyMeters
		} else {
			this.verticalAccuracy = 0f
		}
		
		when(gpsLocationTypeIdId) {
			1 -> this.gpsLocationTypeId = C.API_WP_ID
			2 -> this.gpsLocationTypeId = C.API_CP_ID
			else -> this.gpsLocationTypeId = C.API_LOCATION_ID
		}
		this.gpsSessionId = gpsSessionId
	}
	
	constructor(location: LocationModel, gpsSessionId: String) {
		this.recordedAt = location.recordedAt.toString()
		this.latitude = location.latitude
		this.longitude = location.longitude
		this.accuracy = location.accuracy
		this.altitude = location.altitude
		this.verticalAccuracy = location.verticalAccuracy
		when(location.locationType) {
			1 -> this.gpsLocationTypeId = C.API_WP_ID
			2 -> this.gpsLocationTypeId = C.API_CP_ID
			else -> this.gpsLocationTypeId = C.API_LOCATION_ID
		}
		this.gpsSessionId = gpsSessionId
	}
}
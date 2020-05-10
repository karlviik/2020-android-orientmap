package ee.taltech.orientmap.poko

import android.location.Location
import android.os.Build
import org.threeten.bp.LocalDateTime
import org.threeten.bp.OffsetDateTime

class LocationModel {
	var id: Long = 0
	var sessionId: Long = 0
	var isUploaded: Boolean = false
	
	var recordedAt: LocalDateTime = LocalDateTime.now()
	var latitude: Double = 0.0
	var longitude: Double = 0.0
	var accuracy: Float = 0f
	var altitude: Double = 0.0
	var verticalAccuracy: Float = 0f
	var locationType: Int = 0 // 0 for loc, 1 for wp, 2 for cp
	
	constructor(location: Location, locationType: Int, sessionId: Long) {
		this.sessionId = sessionId
		this.recordedAt = LocalDateTime.ofEpochSecond(location.time / 1000, (location.time % 1000 * 1000000).toInt(), OffsetDateTime.now().offset)
		this.latitude = location.latitude
		this.longitude = location.longitude
		this.accuracy = location.accuracy
		this.altitude = location.altitude
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasVerticalAccuracy()) {
			this.verticalAccuracy = location.verticalAccuracyMeters
		} else {
			this.verticalAccuracy = 0f
		}
		this.locationType = locationType
	}
	
	constructor(id: Long, sessionId: Long, recordedAt: LocalDateTime, latitude: Double, longitude: Double, accuracy: Float, altitude: Double, verticalAccuracy: Float, locationType: Int, isUploaded: Boolean) {
		this.id = id
		this.sessionId = sessionId
		this.recordedAt = recordedAt
		this.latitude = latitude
		this.longitude = longitude
		this.accuracy = accuracy
		this.altitude = altitude
		this.verticalAccuracy = verticalAccuracy
		this.locationType = locationType
		this.isUploaded = isUploaded
	}
	
	
}
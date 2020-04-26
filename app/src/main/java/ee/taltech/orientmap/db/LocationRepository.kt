package ee.taltech.orientmap.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import ee.taltech.orientmap.poko.LocationModel
import ee.taltech.orientmap.poko.SessionModel
import org.threeten.bp.LocalDateTime
import org.threeten.bp.OffsetDateTime


class LocationRepository(val context: Context) {
	
	private lateinit var dbHelper: DbHelper
	private lateinit var db: SQLiteDatabase
	
	fun open(): LocationRepository {
		dbHelper = DbHelper(context)
		db = dbHelper.writableDatabase
		return this
	}
	
	fun close() {
		dbHelper.close()
	}
	
	fun add(location: LocationModel) {
		val contentValues = ContentValues()
		contentValues.put(DbHelper.LOCATION_SESSION_ID, location.sessionId)
		contentValues.put(DbHelper.LOCATION_RECORDED_AT, location.recordedAt.toEpochSecond(OffsetDateTime.now().offset) * 1000000000 + location.recordedAt.nano)
		contentValues.put(DbHelper.LOCATION_LATITUDE, location.latitude)
		contentValues.put(DbHelper.LOCATION_LONGITUDE, location.longitude)
		contentValues.put(DbHelper.LOCATION_ACCURACY, location.accuracy)
		contentValues.put(DbHelper.LOCATION_ALTITUDE, location.altitude)
		contentValues.put(DbHelper.LOCATION_VERTICAL_ACCURACY, location.verticalAccuracy)
		contentValues.put(DbHelper.LOCATION_TYPE, location.locationType)
		location.id = db.insert(DbHelper.LOCATION_TABLE_NAME, null, contentValues)
	}
	
	private fun fetch(sessionId: Long): Cursor {
		val columns = arrayOf(
			DbHelper.LOCATION_ID,
			DbHelper.LOCATION_SESSION_ID,
			DbHelper.LOCATION_RECORDED_AT,
			DbHelper.LOCATION_LATITUDE,
			DbHelper.LOCATION_LONGITUDE,
			DbHelper.LOCATION_ACCURACY,
			DbHelper.LOCATION_ALTITUDE,
			DbHelper.LOCATION_VERTICAL_ACCURACY,
			DbHelper.LOCATION_TYPE
		)
		val orderBy = DbHelper.LOCATION_RECORDED_AT
		return db.query(DbHelper.SESSION_TABLE_NAME, columns, DbHelper.LOCATION_SESSION_ID + "=?", arrayOf(sessionId.toString()), null, null, orderBy)
	}
	
	fun getSessionLocations(sessionId: Long): List<LocationModel> {
		val locations = ArrayList<LocationModel>()
		val cursor = fetch(sessionId)
		while (cursor.moveToNext()) {
			val time = cursor.getLong(cursor.getColumnIndex(DbHelper.LOCATION_RECORDED_AT))
			locations.add(
				LocationModel(
					cursor.getLong(cursor.getColumnIndex(DbHelper.LOCATION_ID)),
					cursor.getLong(cursor.getColumnIndex(DbHelper.LOCATION_SESSION_ID)),
					LocalDateTime.ofEpochSecond(time / 1000000000, (time % 1000000000).toInt(), OffsetDateTime.now().offset),
					cursor.getDouble(cursor.getColumnIndex(DbHelper.LOCATION_LATITUDE)),
					cursor.getDouble(cursor.getColumnIndex(DbHelper.LOCATION_LONGITUDE)),
					cursor.getFloat(cursor.getColumnIndex(DbHelper.LOCATION_ACCURACY)),
					cursor.getDouble(cursor.getColumnIndex(DbHelper.LOCATION_ALTITUDE)),
					cursor.getFloat(cursor.getColumnIndex(DbHelper.LOCATION_VERTICAL_ACCURACY)),
					cursor.getInt(cursor.getColumnIndex(DbHelper.LOCATION_TYPE))
				)
			)
		}
		return locations
	}
	
	fun deleteSessionLocations(sessionId: Long) {
		db.delete(DbHelper.LOCATION_TABLE_NAME, DbHelper.LOCATION_SESSION_ID + "=?", arrayOf(sessionId.toString()))
	}
}
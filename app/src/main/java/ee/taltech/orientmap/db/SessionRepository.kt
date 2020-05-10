package ee.taltech.orientmap.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import ee.taltech.orientmap.poko.SessionModel
import org.threeten.bp.LocalDateTime
import org.threeten.bp.OffsetDateTime


class SessionRepository(val context: Context) {
	
	private lateinit var dbHelper: DbHelper
	private lateinit var db: SQLiteDatabase
	
	fun open(): SessionRepository {
		dbHelper = DbHelper(context)
		db = dbHelper.writableDatabase
		return this
	}
	
	fun close() {
		dbHelper.close()
	}
	
	fun add(session: SessionModel) {
		val contentValues = ContentValues()
		contentValues.put(DbHelper.SESSION_API_ID, session.apiId)
		contentValues.put(DbHelper.SESSION_NAME, session.name)
		contentValues.put(DbHelper.SESSION_START, session.start.toEpochSecond(OffsetDateTime.now().offset) * 1000000000 + session.start.nano)
		contentValues.put(DbHelper.SESSION_DISTANCE, session.distance)
		contentValues.put(DbHelper.SESSION_DURATION, session.duration)
		contentValues.put(DbHelper.SESSION_TIME_PER_KM, session.timePerKm)
		contentValues.put(DbHelper.SESSION_GRADIENT_SLOW_TIME, session.gradientSlowTime)
		contentValues.put(DbHelper.SESSION_GRADIENT_FAST_TIME, session.gradientFastTime)
		contentValues.put(DbHelper.SESSION_LOCATION_COUNT, session.locationCount)
		contentValues.put(DbHelper.SESSION_UPLOADED_LOCATION_COUNT, session.uploadedLocationCount)
		contentValues.put(DbHelper.SESSION_USER_EMAIL, session.userEmail)
		session.id = db.insert(DbHelper.SESSION_TABLE_NAME, null, contentValues)
	}
	
	private fun fetch(): Cursor {
		val columns = arrayOf(
			DbHelper.SESSION_ID,
			DbHelper.SESSION_API_ID,
			DbHelper.SESSION_NAME,
			DbHelper.SESSION_START,
			DbHelper.SESSION_DISTANCE,
			DbHelper.SESSION_DURATION,
			DbHelper.SESSION_TIME_PER_KM,
			DbHelper.SESSION_GRADIENT_SLOW_TIME,
			DbHelper.SESSION_GRADIENT_FAST_TIME,
			DbHelper.SESSION_LOCATION_COUNT,
			DbHelper.SESSION_UPLOADED_LOCATION_COUNT,
			DbHelper.SESSION_USER_EMAIL
		)
		
		val orderBy = DbHelper.SESSION_START
		
		return db.query(DbHelper.SESSION_TABLE_NAME, columns, null, null, null, null, orderBy)
	}
	
	fun getAll(): List<SessionModel> {
		val sessions = ArrayList<SessionModel>()
		val cursor = fetch()
		while (cursor.moveToNext()) {
			val time = cursor.getLong(cursor.getColumnIndex(DbHelper.SESSION_START))
			
			sessions.add(
				SessionModel(
					cursor.getLong(cursor.getColumnIndex(DbHelper.SESSION_ID)),
					cursor.getString(cursor.getColumnIndex(DbHelper.SESSION_API_ID)),
					cursor.getString(cursor.getColumnIndex(DbHelper.SESSION_NAME)),
					LocalDateTime.ofEpochSecond(time / 1000000000, (time % 1000000000).toInt(), OffsetDateTime.now().offset),
					cursor.getInt(cursor.getColumnIndex(DbHelper.SESSION_DISTANCE)),
					cursor.getInt(cursor.getColumnIndex(DbHelper.SESSION_DURATION)),
					cursor.getInt(cursor.getColumnIndex(DbHelper.SESSION_TIME_PER_KM)),
					cursor.getInt(cursor.getColumnIndex(DbHelper.SESSION_GRADIENT_FAST_TIME)),
					cursor.getInt(cursor.getColumnIndex(DbHelper.SESSION_GRADIENT_SLOW_TIME)),
					cursor.getInt(cursor.getColumnIndex(DbHelper.SESSION_LOCATION_COUNT)),
					cursor.getInt(cursor.getColumnIndex(DbHelper.SESSION_UPLOADED_LOCATION_COUNT)),
					cursor.getString(cursor.getColumnIndex(DbHelper.SESSION_USER_EMAIL))
				)
			)
		}
		
		return sessions
	}
	
	fun update(session: SessionModel) {
		val contentValues = ContentValues()
		contentValues.put(DbHelper.SESSION_API_ID, session.apiId)
		contentValues.put(DbHelper.SESSION_NAME, session.name)
		contentValues.put(DbHelper.SESSION_START, session.start.toEpochSecond(OffsetDateTime.now().offset) * 1000000000 + session.start.nano)
		contentValues.put(DbHelper.SESSION_DISTANCE, session.distance)
		contentValues.put(DbHelper.SESSION_DURATION, session.duration)
		contentValues.put(DbHelper.SESSION_TIME_PER_KM, session.timePerKm)
		contentValues.put(DbHelper.SESSION_GRADIENT_SLOW_TIME, session.gradientSlowTime)
		contentValues.put(DbHelper.SESSION_GRADIENT_FAST_TIME, session.gradientFastTime)
		contentValues.put(DbHelper.SESSION_LOCATION_COUNT, session.locationCount)
		contentValues.put(DbHelper.SESSION_UPLOADED_LOCATION_COUNT, session.uploadedLocationCount)
		contentValues.put(DbHelper.SESSION_USER_EMAIL, session.userEmail)
		db.update(DbHelper.SESSION_TABLE_NAME, contentValues, DbHelper.SESSION_ID + "=?", arrayOf(session.id.toString()))
	}
	
	fun delete(session: SessionModel) {
		db.delete(DbHelper.SESSION_TABLE_NAME, DbHelper.SESSION_ID + "=?", arrayOf(session.id.toString()))
	}
}
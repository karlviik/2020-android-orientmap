package ee.taltech.orientmap.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.threeten.bp.LocalDateTime

class DbHelper(context: Context) : SQLiteOpenHelper(
	context,
	DATABASE_NAME, null,
	DATABASE_VERSION
) {
	
	companion object {
		const val DATABASE_NAME = "orientmap.db"
		const val DATABASE_VERSION = 2
		
		const val SESSION_TABLE_NAME = "SESSIONS"
		
		const val SESSION_ID = "_id"
		const val SESSION_API_ID = "api_id"
		const val SESSION_NAME = "name"
		const val SESSION_START = "start"
		const val SESSION_DISTANCE = "distance"
		const val SESSION_DURATION = "duration"
		const val SESSION_TIME_PER_KM = "time_per_km"
		const val SESSION_GRADIENT_SLOW_TIME = "slow_gradient_time"
		const val SESSION_GRADIENT_FAST_TIME = "fast_gradient_time"
		
		const val SQL_SESSION_CREATE_TABLE =
			"create table $SESSION_TABLE_NAME (" +
					"$SESSION_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
					"$SESSION_API_ID TEXT NOT NULL, " +
					"$SESSION_NAME TEXT NOT NULL, " +
					"$SESSION_START INTEGER NOT NULL, " +
					"$SESSION_DISTANCE INTEGER NOT NULL, " +
					"$SESSION_DURATION INTEGER NOT NULL, " +
					"$SESSION_TIME_PER_KM INTEGER NOT NULL, " +
					"$SESSION_GRADIENT_SLOW_TIME INTEGER NOT NULL, " +
					"$SESSION_GRADIENT_FAST_TIME INTEGER NOT NULL" +
					");"
		
		
		const val SQL_DELETE_SESSION_TABLE = "DROP TABLE IF EXISTS $SESSION_TABLE_NAME;"
		
		const val LOCATION_TABLE_NAME = "locations"
		
		const val LOCATION_ID = "_id"
		const val LOCATION_SESSION_ID = "session_id"
		const val LOCATION_RECORDED_AT = "recorded_at"
		const val LOCATION_LATITUDE = "latitude"
		const val LOCATION_LONGITUDE = "longitude"
		const val LOCATION_ACCURACY = "accuracy"
		const val LOCATION_ALTITUDE = "altitude"
		const val LOCATION_VERTICAL_ACCURACY = "vertical_accuracy"
		const val LOCATION_TYPE = "type"
		
		const val SQL_LOCATION_CREATE_TABLE =
			"create table $LOCATION_TABLE_NAME (" +
					"$LOCATION_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
					"$LOCATION_SESSION_ID INTEGER NOT NULL, " +
					"$LOCATION_RECORDED_AT INTEGER NOT NULL, " +
					"$LOCATION_LATITUDE REAL NOT NULL, " +
					"$LOCATION_LONGITUDE REAL NOT NULL, " +
					"$LOCATION_ACCURACY REAL NOT NULL, " +
					"$LOCATION_ALTITUDE REAL NOT NULL, " +
					"$LOCATION_VERTICAL_ACCURACY REAL NOT NULL, " +
					"$LOCATION_TYPE INTEGER NOT NULL" +
					");"
		
		const val SQL_DELETE_LOCATION_TABLE = "DROP TABLE IF EXISTS $LOCATION_TABLE_NAME;"
		
		
	}
	
	override fun onCreate(db: SQLiteDatabase?) {
		db?.execSQL(SQL_SESSION_CREATE_TABLE)
		db?.execSQL(SQL_LOCATION_CREATE_TABLE)
	}
	
	override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
		db?.execSQL(SQL_DELETE_SESSION_TABLE)
		db?.execSQL(SQL_DELETE_LOCATION_TABLE)
		onCreate(db)
	}
}
package ee.taltech.orientmap

import android.graphics.Color

class C {
	companion object {
		const val SLOW_COLOR = Color.RED
		const val FAST_COLOR = Color.GREEN
		const val DEFAULT_SLOW_SPEED = 600
		const val DEFAULT_FAST_SPEED = 300
		const val DEFAULT_GPS_UPDATE_INTERVAL_MILLISECONDS: Long = 2000
		const val DEFAULT_SYNC_INTERVAL_SECONDS = 0
		
		const val NOTIFICATION_CHANNEL = "default_channel"
		
		const val NOTIFICATION_WP_ADD_TO_CURRENT = "ee.taltech.kaviik.orientmap.notification.wp"
		const val NOTIFICATION_CP_ADD_TO_CURRENT = "ee.taltech.kaviik.orientmap.notification.cp"
		const val WP_REMOVE = "ee.taltech.kaviik.orientmap.wp.remove"
		const val WP_ADD_TO_CURRENT = "ee.taltech.kaviik.orientmap.wp"
		const val CP_ADD_TO_CURRENT = "ee.taltech.kaviik.orientmap.cp"
		const val REQUEST_WP_LOCATION = "ee.taltech.kaviik.orientmap.wp.location"
		const val REPLY_WP_LOCATION = "ee.taltech.kaviik.orientmap.wp.location.response"
		const val REQUEST_CP_LOCATIONS = "ee.taltech.kaviik.orientmap.cp.locations"
		const val REPLY_CP_LOCATIONS = "ee.taltech.kaviik.orientmap.cp.locations.response"
		const val REQUEST_POINTS_LOCATIONS = "ee.taltech.kaviik.orientmap.points.locations"
		const val REPLY_POINTS_LOCATIONS = "ee.taltech.kaviik.orientmap.points.locations.response"
		const val GENERAL_LOCATION = "ee.taltech.kaviik.orientmap.randos.location"
		const val GENERAL_LOCATIONS = "ee.taltech.kaviik.orientmap.randos.location"
		const val GENERAL_COLORS = "ee.taltech.kaviik.orientmap.randos.colors"
		
		const val CLEAR_MAP = "ee.taltech.kaviik.orientmap.clear_map"
		
		const val LOCATION_UPDATE_ACTION = "ee.taltech.kaviik.orientmap.location_update"
		const val LOCATION_UPDATE_MOVEMENT_BEARING = "ee.taltech.kaviik.orientmap.location_update.movement_bearing"
		
		const val LOCATION_UPDATE_ACTION_HAS_LOCATION = "ee.taltech.kaviik.orientmap.location_update.has_location"
		const val LOCATION_UPDATE_ACTION_LATITUDE = "ee.taltech.kaviik.orientmap.location_update.latitude"
		const val LOCATION_UPDATE_ACTION_PREV_LATITUDE = "ee.taltech.kaviik.orientmap.location_update.prev_latitude"
		const val LOCATION_UPDATE_ACTION_LONGITUDE = "ee.taltech.kaviik.orientmap.location_update.longitude"
		const val LOCATION_UPDATE_ACTION_PREV_LONGITUDE = "ee.taltech.kaviik.orientmap.location_update.prev_longitude"
		const val LOCATION_UPDATE_ACTION_COLOR = "ee.taltech.kaviik.orientmap.location_update.color"
		const val LOCATION_UPDATE_ACTION_OVERALL_DISTANCE = "ee.taltech.kaviik.orientmap.location_update.overall.distance"
		const val LOCATION_UPDATE_ACTION_OVERALL_TIME = "ee.taltech.kaviik.orientmap.location_update.overall.time"
		const val LOCATION_UPDATE_ACTION_OVERALL_PACE = "ee.taltech.kaviik.orientmap.location_update.overall.pace"
		const val LOCATION_UPDATE_ACTION_CP_TOTAL_DISTANCE = "ee.taltech.kaviik.orientmap.location_update.cp.total_distance"
		const val LOCATION_UPDATE_ACTION_CP_DIRECT_DISTANCE = "ee.taltech.kaviik.orientmap.location_update.cp.direct_distance"
		const val LOCATION_UPDATE_ACTION_CP_PACE = "ee.taltech.kaviik.orientmap.location_update.cp.pace"
		const val LOCATION_UPDATE_ACTION_WP_TOTAL_DISTANCE = "ee.taltech.kaviik.orientmap.location_update.wp.total_distance"
		const val LOCATION_UPDATE_ACTION_WP_DIRECT_DISTANCE = "ee.taltech.kaviik.orientmap.location_update.wp.direct_distance"
		const val LOCATION_UPDATE_ACTION_WP_PACE = "ee.taltech.kaviik.orientmap.location_update.wp.pace"
		
		
		const val NOTIFICATION_ID = 4321
		const val REQUEST_PERMISSIONS_REQUEST_CODE = 34
		
		
		const val API_LOCATION_ID = "00000000-0000-0000-0000-000000000001"
		const val API_WP_ID = "00000000-0000-0000-0000-000000000002"
		const val API_CP_ID = "00000000-0000-0000-0000-000000000003"
	}
}
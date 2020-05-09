package ee.taltech.orientmap.poko

import org.threeten.bp.LocalDateTime

class SessionModel {
	
	var id: Long = 0
	var apiId: String = ""
	var name: String = "Session"
	var start: LocalDateTime = LocalDateTime.now()
	var distance: Int = 0
	var duration: Int = 0
	var timePerKm: Int = 0
	var gradientFastTime: Int = 240  // speed per km min, 300 seconds
	var gradientSlowTime: Int = 480  // speed per km max, 800 seconds
	
	constructor(apiId: String, name: String, start: LocalDateTime, distance: Int, duration: Int, timePerKm: Int, gradientFastTime: Int, gradientSlowTime: Int) {
		this.apiId = apiId
		this.name = name
		this.start = start
		this.distance = distance
		this.duration = duration
		this.timePerKm = timePerKm
		this.gradientFastTime = gradientFastTime
		this.gradientSlowTime = gradientSlowTime
	}
	
	constructor(id: Long, apiId: String, name: String, start: LocalDateTime, distance: Int, duration: Int, timePerKm: Int, gradientFastTime: Int, gradientSlowTime: Int) {
		this.id = id
		this.apiId = apiId
		this.name = name
		this.start = start
		this.distance = distance
		this.duration = duration
		this.timePerKm = timePerKm
		this.gradientFastTime = gradientFastTime
		this.gradientSlowTime = gradientSlowTime
	}
}
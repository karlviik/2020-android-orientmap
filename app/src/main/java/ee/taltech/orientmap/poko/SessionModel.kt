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
	var gradientFastColor: Int = 0   // color for fastest aka min time
	var gradientSlowTime: Int = 480  // speed per km max, 800 seconds
	var gradientSlowColor: Int = 0   // color for slowest aka max time
	
	constructor(apiId: String, name: String, start: LocalDateTime, distance: Int, duration: Int, timePerKm: Int, gradientFastTime: Int, gradientFastColor: Int, gradientSlowTime: Int, gradientSlowColor: Int) {
		this.apiId = apiId
		this.name = name
		this.start = start
		this.distance = distance
		this.duration = duration
		this.timePerKm = timePerKm
		this.gradientFastTime = gradientFastTime
		this.gradientFastColor = gradientFastColor
		this.gradientSlowTime = gradientSlowTime
		this.gradientSlowColor = gradientSlowColor
	}
	
	constructor(id: Long, apiId: String, name: String, start: LocalDateTime, distance: Int, duration: Int, timePerKm: Int, gradientFastTime: Int, gradientFastColor: Int, gradientSlowTime: Int, gradientSlowColor: Int) {
		this.id = id
		this.apiId = apiId
		this.name = name
		this.start = start
		this.distance = distance
		this.duration = duration
		this.timePerKm = timePerKm
		this.gradientFastTime = gradientFastTime
		this.gradientFastColor = gradientFastColor
		this.gradientSlowTime = gradientSlowTime
		this.gradientSlowColor = gradientSlowColor
	}
}
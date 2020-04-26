package ee.taltech.orientmap.poko.api

class SessionResponse {
	var name: String = ""
	var description: String = ""
	var recordedAt: String = ""
	var duration: Int = 0
	var speed: Int = 0
	var distance: Int = 0
	var climb: Int = 0
	var descent: Int = 0
	var appUserId: String = ""  // with token I guess
	var id: String = "" // session id, used for posting stuff
}
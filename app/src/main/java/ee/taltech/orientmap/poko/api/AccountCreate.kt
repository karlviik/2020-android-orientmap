package ee.taltech.orientmap.poko.api

class AccountCreate {
	var email: String = ""
	var password: String = ""
	var lastName: String = ""
	var firstName: String = ""
	
	constructor(email: String, password: String, lastName: String, firstName: String) {
		this.email = email
		this.password = password
		this.lastName = lastName
		this.firstName = firstName
	}
}
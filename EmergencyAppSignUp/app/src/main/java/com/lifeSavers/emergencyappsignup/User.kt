package com.lifeSavers.emergencyappsignup

data class User(
    val name: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val birthDate: String? = null
) {

}

/*class User {
    private var name = ""
    private var email = ""
    private var phoneNumber = ""
    private var birthDate = ""
    private var password = ""

    constructor(
        name: String,
        email: String,
        phoneNumber: String,
        birthDate: String,
        password: String
    ) {
        this.name = name
        this.email = email
        this.phoneNumber = phoneNumber
        this.birthDate = birthDate
        this.password = password
    }
// private var confirmedPassword = ""
}*/
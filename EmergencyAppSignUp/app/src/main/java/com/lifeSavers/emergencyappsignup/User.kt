package com.lifeSavers.emergencyappsignup

import android.net.Uri

data class User(
    val name: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val birthDate: String? = null,
    val userType: Long? = null,
    val profileImage: String? = null
) {

}
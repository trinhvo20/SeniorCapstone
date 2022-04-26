package com.example.itin.classes

import java.io.Serializable

data class User (
    var uid: String,
    var fullName : String,
    var username : String,
    var email : String,
    var phone : String,
): Serializable
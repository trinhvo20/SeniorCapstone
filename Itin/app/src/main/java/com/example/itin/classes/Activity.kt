package com.example.itin.classes

import java.io.Serializable

class Activity(
    var name: String,
    var time: String,
    var location: String,
    var cost: String = "$0",
    var notes: String = "",
    var tripID: Int,
    var actID: Int,

    ): Serializable

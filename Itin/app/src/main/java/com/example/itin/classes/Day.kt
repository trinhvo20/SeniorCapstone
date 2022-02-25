package com.example.itin.classes

import com.example.itin.classes.Activity
import java.io.Serializable

class Day (
    var daynumber : String,
    var activities : MutableList<Activity?> = mutableListOf(),
    var dayInt : Int,
    var tripID : Int,
): Serializable {
    operator fun get(position: Int): Activity? {
        return activities[position]
    }
}
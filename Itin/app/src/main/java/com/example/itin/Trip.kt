package com.example.itin

import android.location.Location
import java.io.Serializable
import java.util.*

class Trip (
    var name : String,
    var location : String,
    var startDate: String,
    var endDate : String,
) : Serializable // objects of this class can be transferred between activities
// need to implement a function to calculate countdown from startDate and endDate.
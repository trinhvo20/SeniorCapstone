package com.example.itin

import android.location.Location
import java.io.Serializable
import java.util.*

class Trip (
    val name : String,
    val location : String,
    val startDate: String,
    val endDate : String,
) : Serializable // objects of this class can be transferred between activities
// need to implement a function to calculate countdown from startDate and endDate.
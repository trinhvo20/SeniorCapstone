package com.example.itin

import android.location.Location
import java.util.*

data class Trip (
    val name : String,
    val location : String,
    val startDate: String,
    val endDate : String,
    val countdown : Int
)
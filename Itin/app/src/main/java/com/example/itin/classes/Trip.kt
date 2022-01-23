package com.example.itin.classes

import com.example.itin.classes.Day
import java.io.Serializable

class Trip (
    var name : String,
    var location : String,
    var startDate: String,
    var endDate : String,
    var days : MutableList<Day> = mutableListOf(),
) : Serializable // objects of this class can be transferred between activities
// need to implement a function to calculate countdown from NOW to startDate
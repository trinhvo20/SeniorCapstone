package com.example.itin.classes

import com.example.itin.classes.Activity

class Day (
    var daynumber : String,
    var activities : MutableList<Activity?> = mutableListOf(),
)
package com.example.itin.classes

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.itin.classes.Day
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.io.Serializable

class Trip (
    var name : String,
    var location : String,
    var startDate: String,
    var endDate : String,
    var deleted : Boolean,
    var active : Boolean,
    var tripID : Int,
    var days : MutableList<Day> = mutableListOf(),
    var viewers : MutableMap<String, Int> = mutableMapOf(),
    var epochStart : Long = 0,
    var epochEnd : Long = 0
    var pending : Int = 0
): Serializable
// objects of this class can be transferred between activities
// need to implement a function to calculate countdown from NOW to startDate
{
    fun delByName(str : String){
        if(str == this.name){
           this.deleted = true
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun sendToDB(){
        val masterTripList = FirebaseDatabase.getInstance().getReference("masterTripList")
        val tripInstance = masterTripList.child(this.tripID.toString())

        tripInstance.child("Name").setValue(name)
        tripInstance.child("Location").setValue(location)
        tripInstance.child("Start Date").setValue(startDate)
        tripInstance.child("End Date").setValue(endDate)
        tripInstance.child("Deleted").setValue(deleted)
        tripInstance.child("Active").setValue(active)
    }
}



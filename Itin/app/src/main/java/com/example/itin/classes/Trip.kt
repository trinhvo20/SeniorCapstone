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
        val firebaseAuth = FirebaseAuth.getInstance()
        val uid = firebaseAuth.currentUser?.uid.toString()
        val curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
        val curTrip = curUser.child("trips")
        val tripInstance = curTrip.child(this.tripID.toString())

        tripInstance.child("Name").setValue(name)
        tripInstance.child("Location").setValue(location)
        tripInstance.child("Start Date").setValue(startDate)
        tripInstance.child("End Date").setValue(endDate)
        tripInstance.child("Deleted").setValue(deleted)
        tripInstance.child("Active").setValue(active)
    }
}



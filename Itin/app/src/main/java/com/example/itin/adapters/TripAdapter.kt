package com.example.itin.adapters

import android.app.AlertDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.itin.R
import com.example.itin.classes.Activity
import com.example.itin.classes.Day
import com.example.itin.classes.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.trip_item.view.*
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*


class TripAdapter(
    private val context: Context,
    private val trips: MutableList<Trip>, // parameter: a mutable list of trip items
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    private lateinit var firebaseAuth: FirebaseAuth
    // create a view holder: holds a layout of a specific item
    @RequiresApi(Build.VERSION_CODES.O)
    inner class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivMenu: ImageView = itemView.findViewById(R.id.ivMenu)
        private lateinit var masterTripList: DatabaseReference
        private var formatter : DateTimeFormatter = DateTimeFormatter.ofPattern("M/d/yyyy")
        private lateinit var startDateObj : LocalDate

        init {
            ivMenu.setOnClickListener { popupMenu(it) }
        }

        // this function handles the popup menu for each item in the trips list
        @RequiresApi(Build.VERSION_CODES.O)
        private fun popupMenu(view: View) {
            val curTrip = trips[adapterPosition]
            firebaseAuth = FirebaseAuth.getInstance()
            val firebaseUser = firebaseAuth.currentUser
            val uid = firebaseUser!!.uid

            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.show_menu)
            popupMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    // for Delete button
                    R.id.delete -> {
                        if (curTrip.viewers[uid] == 1) {
                            val dialog = AlertDialog.Builder(context)
                            dialog.setTitle("Delete")
                                .setIcon(R.drawable.ic_warning)
                                .setMessage("Are you sure you want to delete this trip?")
                                .setPositiveButton("Yes") { dialog, _ ->
                                    curTrip.delByName(curTrip.name)
                                    curTrip.sendToDB()
                                    trips.removeAt(adapterPosition)
                                    tripsort(trips)
                                    notifyDataSetChanged()

                                    Toast.makeText(
                                        context,
                                        "Trip Successfully Deleted",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                    dialog.dismiss()
                                }
                                .setNegativeButton("No") { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .create()
                                .show()
                        }

                        else {
                            Toast.makeText(context, "You do not have permission to preform this action", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }

                    R.id.copy -> {
                        dupetrip(curTrip)
                        Toast.makeText(context, "Duplicated", Toast.LENGTH_SHORT).show()
                        true
                    }

                    R.id.leave -> {
                        val dialog1 = AlertDialog.Builder(context)
                        val dialog2 = AlertDialog.Builder(context)
                        dialog1.setTitle("Leave Trip")
                            .setIcon(R.drawable.ic_alert)
                            .setMessage("Are you sure you want to leave this trip?")
                            .setPositiveButton("Yes") { dialog, _ ->
                                if (curTrip.viewers.size > 1) {
                                    leavetrip(curTrip.tripID,curTrip.viewers)
                                    trips.removeAt(adapterPosition)
                                    tripsort(trips)
                                    notifyDataSetChanged()
                                    Toast.makeText(context, "Trip Successfully Left", Toast.LENGTH_SHORT)
                                        .show()
                                    dialog.dismiss()
                                }
                                else{
                                    dialog2.setTitle("You are the only person in this trip!")
                                        .setIcon(R.drawable.ic_warning)
                                        .setMessage("By leaving this trip the trip will be deleted.\nAre you sure you want to delete this trip?")
                                        .setPositiveButton("Yes") { dialog, _ ->
                                            curTrip.delByName(curTrip.name)
                                            curTrip.sendToDB()
                                            trips.removeAt(adapterPosition)
                                            tripsort(trips)
                                            notifyDataSetChanged()

                                            Toast.makeText(context, "Trip Successfully Deleted", Toast.LENGTH_SHORT)
                                                .show()
                                            dialog.dismiss()
                                        }
                                        .setNegativeButton("No") { dialog, _ ->
                                            dialog.dismiss()
                                        }
                                        .create()
                                        .show()
                                }
                            }
                            .setNegativeButton("No") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .create()
                            .show()
                        true
                    }

                    else -> true
                }
            }
            popupMenu.show()
            val popup = PopupMenu::class.java.getDeclaredField("mPopup")
            popup.isAccessible = true
            val menu = popup.get(popupMenu)
            menu.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                .invoke(menu, true)
        }

        private fun leavetrip(tripID: Int, viewers: MutableMap<String, Int>) {
            val firebaseAuth = FirebaseAuth.getInstance()
            val firebaseUser = firebaseAuth.currentUser
            var curUserTrips: DatabaseReference? = null

            // If the user is not current logged in:
            if (firebaseUser == null) { }
            else {
                val uid = firebaseUser.uid
                if(viewers[uid] == 1){
                    Log.d("Perm Leave", "leavetrip: owner left")
                    if (viewers.filterValues { it == 1 }.isNotEmpty()){
                        Log.d("Perm Leave", "leavetrip: no other owners")
                        viewers.remove(uid)
                        val newowner = viewers.entries.elementAt(0).key
                        FirebaseDatabase.getInstance().getReference("masterTripList").child(tripID.toString()).child("Viewers").child(newowner).child("Perm").setValue(1)

                    }
                }
                val curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
                val curTrip = FirebaseDatabase.getInstance().getReference("masterTripList").child(tripID.toString())

                curUserTrips = curUser.child("trips")
                curUserTrips.child("Trip $tripID").removeValue()

                curTrip.child("Viewers").child(uid).removeValue()
            }
        }


        private fun dupetrip(curTrip: Trip) {
            masterTripList = FirebaseDatabase.getInstance().getReference("masterTripList")
            masterTripList.get().addOnSuccessListener {
                if (it.exists()) {
                    var tripCount = it.child("tripCount").value.toString().toInt()

                    val dupeTrip = Trip(
                        "Copy of " + curTrip.name,
                        curTrip.location,
                        curTrip.startDate,
                        curTrip.endDate,
                        deleted = curTrip.deleted,
                        active = curTrip.active,
                        tripID = tripCount,
                        curTrip.days
                    )
                    // when reading from DB, it does not correctly make the days list
                    // check that the heck out
                    val size = dupeTrip.days.size//[i.toInt()].activities.size

                    sendToDB(dupeTrip, tripCount)
                    tripCount += 1
                    masterTripList.child("tripCount").setValue(tripCount)

                    if (dupeTrip.active) {
                        trips.add(dupeTrip)
                        tripsort(trips)
                        notifyDataSetChanged()
                    }

                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun sendToDB(trip: Trip, id: Int) {
            val firebaseAuth = FirebaseAuth.getInstance()
            val firebaseUser = firebaseAuth.currentUser
            var curTrips: DatabaseReference? = null

            // If the use is not current logged in:
            if (firebaseUser == null) {

            } else {
                val uid = firebaseUser.uid
                val curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
                curTrips = curUser.child("trips")
            }

            // Navigates to the correct directory (masterTripList)
            val tripInstance = masterTripList.child(id.toString())

            tripInstance.child("Name").setValue(trip.name)
            tripInstance.child("Location").setValue(trip.location)
            tripInstance.child("Start Date").setValue(trip.startDate)
            tripInstance.child("End Date").setValue(trip.endDate)
            tripInstance.child("Deleted").setValue(trip.deleted)
            tripInstance.child("Active").setValue(trip.active)
            tripInstance.child("ID").setValue(trip.tripID)

            // create days folder
            // will be accessed later in itinerary activity
            val itineraryInstance = tripInstance.child("Days")
            var formatter = DateTimeFormatter.ofPattern("M/d/yyyy")
            var startdate = LocalDate.parse(trip.startDate, formatter)
            var enddate = LocalDate.parse(trip.endDate, formatter)
            val dayNum = ChronoUnit.DAYS.between(startdate, enddate)
            for (i in 0 until dayNum + 1) {
                makeDayInstance(itineraryInstance,i.toInt(), trip)
            }

            // Record trips in the individual user
            if (curTrips != null) {
                curTrips.child("Trip $id").setValue(id)
            }
        }

        private fun makeDayInstance(itineraryInstance: DatabaseReference, dayNum: Int, trip: Trip) {
            // log the day Count
            itineraryInstance.child("DayCount").setValue(dayNum + 1)
            val dayInstance = itineraryInstance.child(dayNum.toString())
            dayInstance.child("Day Number").setValue(dayNum + 1)
            dayInstance.child("TripID").setValue(trip.tripID)
            dayInstance.child("ActivityCount").setValue(0)
            // iterate through the activities on a certain day
            for(i in 0 until trip.days[dayNum].activities.size) {

                val activity = trip.days[dayNum].activities[i]
                if (activity != null) {
                    sendActivityToDB(trip.days[dayNum], activity)
                }
            }
        }

        private fun sendActivityToDB(curDay: Day, activity: Activity) {
            val dayInstance = FirebaseDatabase.getInstance().getReference("masterTripList")
                .child(curDay.tripID.toString()).child("Days").child((curDay.dayInt-1).toString())
            dayInstance.child("ActivityCount").setValue(curDay.activities.size)

            val activityInstance = dayInstance.push()
            if (activity != null) {
                activityInstance.setValue(activity)
            }
        }
    }



    @RequiresApi(Build.VERSION_CODES.O)
    // function to sort the activities on each of the day, it is a modified Insertion sort
    private fun tripsort(trips: MutableList<Trip>) {
        var formatter = DateTimeFormatter.ofPattern("M/d/yyyy")

        for (i in 0 until trips.size) {
            val key = trips[i]
            var j = i - 1

            if (key != null) {
                while (j >= 0 && LocalDate.parse(trips[j].startDate, formatter).isAfter(
                        LocalDate.parse(key.startDate, formatter)
                    )
                ) {
                    trips[j + 1] = trips[j]
                    j--
                }
            }
            trips[j + 1] = key
        }
    }

    // Ctrl + I
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        // LayoutInflater will take trip_item.xml code and convert it to view we can work it in kotlin
        val view = LayoutInflater.from(parent.context).inflate(R.layout.trip_item, parent, false)

        // need to return a TodoViewHolder
        return TripViewHolder(view)
    }

    // take the data from our trips list and set it to the corresponding view (trip_item.xml)
    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val curTrip = trips[position]

        // access to trip_item.xml
        holder.itemView.apply {
            // get the data from our trips list and put them in the corresponding TextView in trip_item.xml
            tvName.text = curTrip.name
            tvCost.text = curTrip.startDate
            tvEndDate.text = curTrip.endDate

            // display trips images
            val tripId = curTrip.tripID.toString()
            var storageReferenceTrip = FirebaseStorage.getInstance().getReference("Trips/$tripId.jpg")
            val localFile = File.createTempFile("tempImage_$tripId","jpg")
            storageReferenceTrip.getFile(localFile).addOnSuccessListener {
                val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                tripImage.setImageBitmap(bitmap)
            }.addOnFailureListener {
                tripImage.setImageResource(R.drawable.beach)
                Log.d("ItineraryImage","Failed to retrieve image")
            }

            // display viewers images
            if (curTrip.viewers.size > 1) {
                if (curTrip.viewers.size == 3) {
                    var viewerlist = curTrip.viewers.keys.toList()
                    var uid1 = viewerlist[0]
                    var uid2 = viewerlist[1]
                    var uid3 = viewerlist[2]

                    var storageReference =
                        FirebaseStorage.getInstance().getReference("Users/$uid3.jpg")
                    val localFileV1 =
                        File.createTempFile("tempImage_${curTrip.tripID}_viewer1", "jpg")
                    storageReference.getFile(localFileV1).addOnSuccessListener {
                        val bitmap = BitmapFactory.decodeFile(localFileV1.absolutePath)
                        ivViewers1.setImageBitmap(bitmap)
                    }.addOnFailureListener {
                        ivViewers1.setImageResource(R.drawable.profile)
                    }

                    storageReference = FirebaseStorage.getInstance().getReference("Users/$uid2.jpg")
                    val localFileV2 =
                        File.createTempFile("tempImage_${curTrip.tripID}_viewer2", "jpg")
                    storageReference.getFile(localFileV2).addOnSuccessListener {
                        val bitmap = BitmapFactory.decodeFile(localFileV2.absolutePath)
                        ivViewers2.setImageBitmap(bitmap)
                    }.addOnFailureListener {
                        ivViewers2.setImageResource(R.drawable.profile)
                    }

                    storageReference = FirebaseStorage.getInstance().getReference("Users/$uid1.jpg")
                    val localFileV3 =
                        File.createTempFile("tempImage_${curTrip.tripID}_viewer3", "jpg")
                    storageReference.getFile(localFileV3).addOnSuccessListener {
                        val bitmap = BitmapFactory.decodeFile(localFileV3.absolutePath)
                        ivViewers3.setImageBitmap(bitmap)
                    }.addOnFailureListener {
                        ivViewers3.setImageResource(R.drawable.profile)
                    }
                    ivViewers1.visibility = View.VISIBLE
                    ivViewers2.visibility = View.VISIBLE
                    ivViewers3.visibility = View.VISIBLE
                }

                if (curTrip.viewers.size == 2) {
                    var viewerlist = curTrip.viewers.keys.toList()
                    var uid1 = viewerlist[0]
                    var uid2 = viewerlist[1]

                    var storageReference =
                        FirebaseStorage.getInstance().getReference("Users/$uid2.jpg")
                    val localFileV1 =
                        File.createTempFile("tempImage_${curTrip.tripID}_viewer1", "jpg")
                    storageReference.getFile(localFileV1).addOnSuccessListener {
                        val bitmap = BitmapFactory.decodeFile(localFileV1.absolutePath)
                        ivViewers1.setImageBitmap(bitmap)
                    }.addOnFailureListener {
                        ivViewers1.setImageResource(R.drawable.profile)
                    }

                    storageReference = FirebaseStorage.getInstance().getReference("Users/$uid1.jpg")
                    val localFileV2 =
                        File.createTempFile("tempImage_${curTrip.tripID}_viewer2", "jpg")
                    storageReference.getFile(localFileV2).addOnSuccessListener {
                        val bitmap = BitmapFactory.decodeFile(localFileV2.absolutePath)
                        ivViewers2.setImageBitmap(bitmap)
                    }.addOnFailureListener {
                        ivViewers2.setImageResource(R.drawable.profile)
                    }
                    ivViewers1.visibility = View.VISIBLE
                    ivViewers2.visibility = View.VISIBLE
                }

                else{
                    var viewerlist = curTrip.viewers.keys.toList()
                    var uid1 = viewerlist[0]
                    var uid2 = viewerlist[1]
                    var uid3 = viewerlist[2]


                    var storageReference =
                        FirebaseStorage.getInstance().getReference("Users/$uid3.jpg")
                    val localFileV1 =
                        File.createTempFile("tempImage_${curTrip.tripID}_viewer1", "jpg")
                    storageReference.getFile(localFileV1).addOnSuccessListener {
                        val bitmap = BitmapFactory.decodeFile(localFileV1.absolutePath)
                        ivViewers1.setImageBitmap(bitmap)
                    }.addOnFailureListener {
                        ivViewers1.setImageResource(R.drawable.profile)
                    }

                    storageReference = FirebaseStorage.getInstance().getReference("Users/$uid2.jpg")
                    val localFileV2 =
                        File.createTempFile("tempImage_${curTrip.tripID}_viewer2", "jpg")
                    storageReference.getFile(localFileV2).addOnSuccessListener {
                        val bitmap = BitmapFactory.decodeFile(localFileV2.absolutePath)
                        ivViewers2.setImageBitmap(bitmap)
                    }.addOnFailureListener {
                        ivViewers2.setImageResource(R.drawable.profile)
                    }

                    storageReference = FirebaseStorage.getInstance().getReference("Users/$uid1.jpg")
                    val localFileV3 =
                        File.createTempFile("tempImage_${curTrip.tripID}_viewer3", "jpg")
                    storageReference.getFile(localFileV3).addOnSuccessListener {
                        val bitmap = BitmapFactory.decodeFile(localFileV3.absolutePath)
                        ivViewers3.setImageBitmap(bitmap)
                    }.addOnFailureListener {
                        ivViewers3.setImageResource(R.drawable.profile)
                    }
                    ivViewers1.visibility = View.VISIBLE
                    ivViewers2.visibility = View.VISIBLE
                    ivViewers3.visibility = View.VISIBLE
                    tvViewersE.text = "+${curTrip.viewers.size - 3}"
                    tvViewersE.visibility = View.VISIBLE
                }
            }
        }

        // handle RecyclerView clickable
        holder.itemView.setOnClickListener {
            listener.onItemClick(position)
        }

    }

    override fun getItemCount(): Int {
        return trips.size
    }

    fun clear() {
        trips.clear()
        notifyDataSetChanged()
    }

    // this interface will handle the RecyclerView clickable
    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }
}
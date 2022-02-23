package com.example.itin.adapters

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.itin.R
import com.example.itin.classes.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.trip_item.view.*
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import com.google.firebase.storage.FirebaseStorage

class TripAdapter(
    private val context: Context,
    private val trips: MutableList<Trip>, // parameter: a mutable list of trip items
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    // create a view holder: holds a layout of a specific item
    @RequiresApi(Build.VERSION_CODES.O)
    inner class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivMenu: ImageView = itemView.findViewById(R.id.ivMenu)
        private lateinit var masterTripList: DatabaseReference

        init {
            ivMenu.setOnClickListener { popupMenu(it) }
        }

        // this function handles the popup menu for each item in the trips list
        @RequiresApi(Build.VERSION_CODES.O)
        private fun popupMenu(view: View) {
            val curTrip = trips[adapterPosition]

            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.show_menu)
            popupMenu.setOnMenuItemClickListener {
                when(it.itemId) {
                    // for Edit button
                    R.id.edit -> {
                        val view = LayoutInflater.from(context).inflate(R.layout.create_trip, null)

                        val etName = view.findViewById<EditText>(R.id.etName)
                        val etLocation = view.findViewById<EditText>(R.id.etLocation)
                        val etStartDate = view.findViewById<TextView>(R.id.etStartDate)
                        val etEndDate = view.findViewById<TextView>(R.id.etEndDate)

                        val c = Calendar.getInstance()
                        val year = c.get(Calendar.YEAR)
                        val month = c.get(Calendar.MONTH)
                        val day = c.get(Calendar.DAY_OF_MONTH)

                        val ivPickStartDate = view.findViewById<ImageView>(R.id.ivPickStartDate)
                        val ivPickEndDate = view.findViewById<ImageView>(R.id.ivPickEndDate)

                        ivPickStartDate.setOnClickListener {
                            val datePickerDialog = DatePickerDialog(context, DatePickerDialog.OnDateSetListener{ _, mYear, mMonth, mDay ->
                                etStartDate.text = ""+(mMonth+1)+"/"+mDay+"/"+mYear
                            }, year, month, day)
                            datePickerDialog.show()
                        }

                        ivPickEndDate.setOnClickListener {
                            val datePickerDialog = DatePickerDialog(context, DatePickerDialog.OnDateSetListener{ _, mYear, mMonth, mDay ->
                                etEndDate.text = ""+(mMonth+1)+"/"+mDay+"/"+mYear
                            }, year, month, day)
                            datePickerDialog.show()
                        }

                        val dialog = AlertDialog.Builder(context)
                        dialog.setView(view)
                            .setPositiveButton("OK") {dialog,_ ->
                                val name = etName.text.toString()
                                val location = etLocation.text.toString()
                                val startDate = etStartDate.text.toString()
                                val endDate = etEndDate.text.toString()

                                if (name.isBlank()){
                                    if (location.isNotBlank()) {
                                        curTrip.name = "Trip to $location"
                                    }
                                } else {
                                    curTrip.name = name
                                }
                                if (location.isNotBlank()){
                                    curTrip.location = location
                                }
                                if (startDate.isNotBlank()){
                                    curTrip.startDate = startDate
                                }
                                if (endDate.isNotBlank()){
                                    curTrip.endDate = endDate
                                    // check for dayInterval to set the trip 'active' status
                                    var formatter = DateTimeFormatter.ofPattern("M/d/yyyy")
                                    val today = LocalDate.now()
                                    val endDateObj = LocalDate.parse(endDate, formatter)
                                    val dayInterval = ChronoUnit.DAYS.between(endDateObj, today).toInt()
                                    curTrip.active = dayInterval <= 0
                                }

                                curTrip.sendToDB()
                                if (!curTrip.active) {trips.removeAt(adapterPosition)}
                                notifyDataSetChanged()
                                Toast.makeText(context, "Successfully Edited", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            }
                            .setNegativeButton("Cancel") {dialog,_ ->
                                dialog.dismiss()
                            }
                            .create()
                            .show()
                        true
                    }

                    // for Delete button
                    R.id.delete -> {
                        val dialog = AlertDialog.Builder(context)
                        dialog.setTitle("Delete")
                            .setIcon(R.drawable.ic_warning)
                            .setMessage("Are you sure delete this trip?")
                            .setPositiveButton("Yes") {dialog,_ ->
                                curTrip.delByName(curTrip.name)
                                curTrip.sendToDB()
                                trips.removeAt(adapterPosition)
                                tripsort(trips)
                                notifyDataSetChanged()

                                Toast.makeText(context, "Successfully Deleted", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            }
                            .setNegativeButton("No") {dialog,_ ->
                                dialog.dismiss()
                            }
                            .create()
                            .show()
                        true
                    }

                    R.id.copy -> {
                        dupetrip(curTrip)
                        Toast.makeText(context, "Duplicated", Toast.LENGTH_SHORT).show()
                        true
                    }
                    else -> true
                }
            }
            popupMenu.show()
            val popup = PopupMenu::class.java.getDeclaredField("mPopup")
            popup.isAccessible = true
            val menu = popup.get(popupMenu)
            menu.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java).invoke(menu, true)
        }

        private fun dupetrip(curTrip : Trip){
            masterTripList = FirebaseDatabase.getInstance().getReference("masterTripList")
            masterTripList.get().addOnSuccessListener {
                if (it.exists()) {
                    var tripCount = it.child("tripCount").value.toString().toInt()

                    val dupetrip = Trip(
                        "Copy of " + curTrip.name,
                        curTrip.location,
                        curTrip.startDate,
                        curTrip.endDate,
                        deleted = curTrip.deleted,
                        active = curTrip.active,
                        tripID = tripCount
                    )

                    sendToDB(dupetrip,tripCount)
                    tripCount += 1
                    Log.d("TripActivity", "tripCount updated: $tripCount")
                    masterTripList.child("tripCount").setValue(tripCount)

                    if (dupetrip.active) {
                        trips.add(dupetrip)
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

            // Record trips in the individual user
            if (curTrips != null) {
                curTrips.child("Trip $id").setValue(id)
            }

        }

        @RequiresApi(Build.VERSION_CODES.O)
        // function to sort the activities on each of the day, it is a modified Insertion sort
        private fun tripsort (trips: MutableList<Trip>){
            var formatter = DateTimeFormatter.ofPattern("M/d/yyyy")

            for (i in 0 until trips.size) {
                val key = trips[i]

                if (key != null) {
                    println(key.startDate)
                }

                var j = i - 1

                if (key != null) {
                    while (j >= 0 && LocalDate.parse(trips[j].startDate, formatter).isAfter(
                            LocalDate.parse(key.startDate, formatter))){
                        trips[j + 1] = trips[j]
                        j--
                    }
                }
                trips[j + 1] = key
            }
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
        holder.itemView.apply{
            // get the data from our trips list and put them in the corresponding TextView in trip_item.xml
            tvName.text = curTrip.name
            tvStartDate.text = curTrip.startDate
            tvEndDate.text = curTrip.endDate

            if (curTrip.viewers.size > 0) {
                if (curTrip.viewers.size == 3) {
                    var uid1 = curTrip.viewers[0]
                    var uid2 = curTrip.viewers[1]
                    var uid3 = curTrip.viewers[2]

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
                    var uid1 = curTrip.viewers[0]
                    var uid2 = curTrip.viewers[1]

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

                if (curTrip.viewers.size == 1) {
                    var uid = curTrip.viewers[0]

                    var storageReference =
                        FirebaseStorage.getInstance().getReference("Users/$uid.jpg")
                    val localFileV1 =
                        File.createTempFile("tempImage_${curTrip.tripID}_viewer1", "jpg")
                    storageReference.getFile(localFileV1).addOnSuccessListener {
                        val bitmap = BitmapFactory.decodeFile(localFileV1.absolutePath)
                        ivViewers1.setImageBitmap(bitmap)
                    }.addOnFailureListener {
                        ivViewers1.setImageResource(R.drawable.profile)
                    }
                    ivViewers1.visibility = View.VISIBLE
                }

                else{
                    var uid1 = curTrip.viewers[0]
                    var uid2 = curTrip.viewers[1]
                    var uid3 = curTrip.viewers[2]


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

    // this interface will handle the RecyclerView clickable
    interface  OnItemClickListener {
        fun onItemClick(position: Int)
    }
    
}
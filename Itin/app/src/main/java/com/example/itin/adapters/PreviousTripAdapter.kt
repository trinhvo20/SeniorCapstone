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
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.itin.R
import com.example.itin.classes.Activity
import com.example.itin.classes.Day
import com.example.itin.classes.Trip
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.trip_item.view.*
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.util.*

class PreviousTripAdapter(

    private val context: Context,
    private val previousTrips: MutableList<Trip>,
    private val listener: OnItemClickListener

) : RecyclerView.Adapter<PreviousTripAdapter.PreviousTripViewHolder>() {

    private lateinit var firebaseAuth: FirebaseAuth
    @RequiresApi(Build.VERSION_CODES.O)
    inner class PreviousTripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivMenu: ImageView = itemView.findViewById(R.id.ivMenu)
        private var formatter : DateTimeFormatter = DateTimeFormatter.ofPattern("M/d/yyyy")
        private lateinit var startDateObj : LocalDate
        private var masterTripList: DatabaseReference = FirebaseDatabase.getInstance().getReference("masterTripList")

        init {
            ivMenu.setOnClickListener { popupMenu(it) }
        }

        // this function handles the popup menu for each item in the trips list
        @RequiresApi(Build.VERSION_CODES.O)
        private fun popupMenu(view: View) {
            val curTrip = previousTrips[adapterPosition]

            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.previous_show_menu)
            popupMenu.setOnMenuItemClickListener {
                when(it.itemId) {

                    // for Delete button
                    R.id.delete -> {
                        val dialog = AlertDialog.Builder(context)
                        dialog.setTitle("Delete")
                            .setIcon(R.drawable.ic_warning)
                            .setMessage("Are you sure delete this trip?")
                            .setPositiveButton("Yes") {dialog,_ ->
                                curTrip.delByName(previousTrips[adapterPosition].name)
                                curTrip.sendToDB()
                                previousTrips.removeAt(adapterPosition)
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

                    else -> true
                }
            }
            popupMenu.show()
            val popup = PopupMenu::class.java.getDeclaredField("mPopup")
            popup.isAccessible = true
            val menu = popup.get(popupMenu)
            menu.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java).invoke(menu, true)
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviousTripViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.trip_item, parent, false)
        return PreviousTripViewHolder(view)
    }

    override fun onBindViewHolder(holder: PreviousTripViewHolder, position: Int) {
        val curTrip = previousTrips[position]

        // access to trip_item.xml
        holder.itemView.apply {
            firebaseAuth = FirebaseAuth.getInstance()
            val firebaseUser = firebaseAuth.currentUser
            val uid = firebaseUser!!.uid
            val curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)

            tvName.text = curTrip.name
            tvStartDate.text = curTrip.startDate
            tvEndDate.text = curTrip.endDate
            tvCountdown.text = countdown(curTrip.epochEnd)

            tvStartDate.visibility = View.VISIBLE
            tvEndDate.visibility = View.VISIBLE
            tvCountdown.visibility = View.VISIBLE
            ivMenu.visibility = View.VISIBLE
            ivMenu.isClickable = true

            // display trips images
            val tripId = curTrip.tripID.toString()
            var storageReferenceTrip = FirebaseStorage.getInstance().getReference("Trips/$tripId.jpg")
            val localFile = File.createTempFile("tempImage","jpg")
            storageReferenceTrip.getFile(localFile).addOnSuccessListener {
                val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                tripImage.setImageBitmap(bitmap)
            }.addOnFailureListener {
                Log.d("ItineraryImage","Failed to retrieve image")
            }
        }

        // handle RecyclerView clickable
        holder.itemView.setOnClickListener {
            listener.onItemClick(position)
        }
    }

    override fun getItemCount(): Int {
        return previousTrips.size
    }

    // this interface will handle the RecyclerView clickable
    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun clear() {
        previousTrips.clear()
        notifyDataSetChanged()
    }

    // calculates how many days since the trip ended
    private fun countdown(endTime: Long): String{
        val curTime = Calendar.getInstance().timeInMillis
        val dif = curTime - endTime
        val days = kotlin.math.floor((dif/86400000).toDouble())
        Log.d("CUR: ", curTime.toString())
        Log.d("END: ",endTime.toString())
        Log.d("DIF: ",dif.toString())
        if(days <= 0){
        }
        else if(days.toInt() == 1){
            return "Your trip ended yesterday."
        }
        return "The trip is ${days.toInt()} days past."
    }
}
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
import com.example.itin.classes.Trip
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.trip_item.view.*
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

class PreviousTripAdapter(

    private val context: Context,
    private val previousTrips: MutableList<Trip>,
    private val listener: OnItemClickListener

) : RecyclerView.Adapter<PreviousTripAdapter.PreviousTripViewHolder>() {

    @RequiresApi(Build.VERSION_CODES.O)
    inner class PreviousTripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivMenu: ImageView = itemView.findViewById(R.id.ivMenu)
        private var formatter : DateTimeFormatter = DateTimeFormatter.ofPattern("M/d/yyyy")
        private lateinit var startDateObj : LocalDate

        init {
            ivMenu.setOnClickListener { popupMenu(it) }
        }

        // this function handles the popup menu for each item in the trips list
        @RequiresApi(Build.VERSION_CODES.O)
        private fun popupMenu(view: View) {
            val curTrip = previousTrips[adapterPosition]

            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.show_menu)
            popupMenu.setOnMenuItemClickListener {
                when(it.itemId) {
                    // for Edit button
                    R.id.edit -> {
                        val view = LayoutInflater.from(context).inflate(R.layout.edit_trip, null)

                        val etName = view.findViewById<EditText>(R.id.etName)
                        val etStartDate = view.findViewById<TextView>(R.id.etStartDate)
                        val etEndDate = view.findViewById<TextView>(R.id.etEndDate)

                        var location = curTrip.location
                        etName.setText(curTrip.name)
                        etStartDate.text = curTrip.startDate
                        etEndDate.text = curTrip.endDate
                        startDateObj = LocalDate.parse(curTrip.startDate, formatter)

                        // Handle AutoComplete Places Search from GoogleAPI
                        if (!Places.isInitialized()) {
                            Places.initialize(context, context.getString(R.string.API_KEY))
                        }
                        val placesClient = Places.createClient(context)
                        val autocompleteFragment =
                            (context as AppCompatActivity).supportFragmentManager.findFragmentById(R.id.etLocation2) as AutocompleteSupportFragment
                        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS))
                        autocompleteFragment.setText(location)
                        autocompleteFragment.setOnPlaceSelectedListener(object :
                            PlaceSelectionListener {
                            override fun onPlaceSelected(place: Place) {
                                location = place.name
                                Log.i("Places", "Place: ${place.address}, ${place.id}")
                            }
                            override fun onError(status: Status) {
                                Log.i("Places", "An error occurred: $status")
                            }
                        })

                        val c = Calendar.getInstance()
                        val year = c.get(Calendar.YEAR)
                        val month = c.get(Calendar.MONTH)
                        val day = c.get(Calendar.DAY_OF_MONTH)

                        val ivPickStartDate = view.findViewById<ImageView>(R.id.ivPickStartDate)
                        val ivPickEndDate = view.findViewById<ImageView>(R.id.ivPickEndDate)

                        ivPickStartDate.setOnClickListener {
                            val datePickerDialog = DatePickerDialog(
                                context,
                                { _, mYear, mMonth, mDay ->
                                    etStartDate.text = "" + (mMonth + 1) + "/" + mDay + "/" + mYear
                                    startDateObj = LocalDate.parse(etStartDate.text.toString(), formatter)
                                }, year, month, day
                            )
                            datePickerDialog.datePicker.minDate = c.timeInMillis
                            datePickerDialog.show()
                        }

                        ivPickEndDate.setOnClickListener {
                            val datePickerDialog = DatePickerDialog(
                                context,
                                { _, mYear, mMonth, mDay ->
                                    etEndDate.text = "" + (mMonth + 1) + "/" + mDay + "/" + mYear
                                }, year, month, day
                            )
                            datePickerDialog.datePicker.minDate = startDateObj.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            datePickerDialog.show()
                        }

                        val dialog = AlertDialog.Builder(context)
                        dialog.setView(view)
                            .setPositiveButton("OK") {dialog,_ ->
                                val name = etName.text.toString()
                                val startDate = etStartDate.text.toString()
                                val endDate = etEndDate.text.toString()

                                if (name.isBlank()) {
                                    if (location != curTrip.location) {
                                        curTrip.name = "Trip to $location"
                                    }
                                } else {
                                    curTrip.name = name
                                }
                                if (location != curTrip.location) {
                                    curTrip.location = location
                                }
                                if (startDate != curTrip.startDate) {
                                    curTrip.startDate = startDate
                                }
                                if (endDate != curTrip.endDate) {
                                    curTrip.endDate = endDate
                                    // check for dayInterval to set the trip 'active' status
                                    var formatter = DateTimeFormatter.ofPattern("M/d/yyyy")
                                    val today = LocalDate.now()
                                    val endDateObj = LocalDate.parse(endDate, formatter)
                                    val dayInterval = ChronoUnit.DAYS.between(endDateObj, today).toInt()
                                    curTrip.active = dayInterval <= 0
                                }

                                curTrip.sendToDB()
                                if (curTrip.active) {previousTrips.removeAt(adapterPosition)}
                                notifyDataSetChanged()
                                context.supportFragmentManager.beginTransaction().remove(autocompleteFragment).commit()
                                Toast.makeText(context, "Successfully Edited", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            }
                            .setNegativeButton("Cancel") {dialog,_ ->
                                context.supportFragmentManager.beginTransaction().remove(autocompleteFragment).commit()
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
            tvName.text = curTrip.name
            tvCost.text = curTrip.startDate
            tvEndDate.text = curTrip.endDate

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
}
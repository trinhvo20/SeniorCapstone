package com.example.itin.adapters

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.itin.R
import com.example.itin.classes.Trip
import kotlinx.android.synthetic.main.trip_item.view.*
import java.time.LocalDate
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
                                if (curTrip.active) {previousTrips.removeAt(adapterPosition)}
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
            tvStartDate.text = curTrip.startDate
            tvEndDate.text = curTrip.endDate
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
}
package com.example.itin.adapters

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.itin.R
import com.example.itin.classes.Trip
import kotlinx.android.synthetic.main.trip_item.view.*
import java.util.*

class TripAdapter(
    private val context: Context,
    private val trips: MutableList<Trip>, // parameter: a mutable list of trip items
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    // create a view holder: holds a layout of a specific item
    inner class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivMenu: ImageView = itemView.findViewById(R.id.ivMenu)

        init {
            ivMenu.setOnClickListener { popupMenu(it) }
        }

        // this function handles the popup menu for each item in the trips list
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
                                if (etName.text.toString().isBlank()){
                                    if (etLocation.text.toString().isNotBlank()) {
                                        curTrip.name = "Trip to " + etLocation.text.toString()
                                    }
                                } else {
                                    curTrip.name = etName.text.toString()
                                }
                                if (etLocation.text.toString().isNotBlank()){
                                    curTrip.location =etLocation.text.toString()
                                }
                                if (etStartDate.text.toString().isNotBlank()){
                                    curTrip.startDate =etStartDate.text.toString()
                                }
                                if (etStartDate.text.toString().isNotBlank()){
                                    curTrip.endDate =etEndDate.text.toString()
                                }
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
                                trips[adapterPosition].delByName(trips[adapterPosition].name)
                                trips[adapterPosition].sendToDB()
                                trips.removeAt(adapterPosition)
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

    // Ctrl + I
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
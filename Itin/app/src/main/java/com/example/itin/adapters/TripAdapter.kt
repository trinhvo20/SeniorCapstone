package com.example.itin.adapters

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.itin.R
import com.example.itin.classes.Trip
import kotlinx.android.synthetic.main.trip_item.view.*

class TripAdapter(
    private val context: Context,
    private val trips: MutableList<Trip>, // parameter: a mutable list of trip items
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    // create a view holder: holds a layout of a specific item
    inner class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivMenu: ImageView = itemView.findViewById<ImageView>(R.id.ivMenu)

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
                        val etStartDate = view.findViewById<EditText>(R.id.etStartDate)
                        val etEndDate = view.findViewById<EditText>(R.id.etEndDate)

                        val dialog = AlertDialog.Builder(context)
                        dialog.setView(view)
                            .setPositiveButton("OK") {dialog,_ ->
                                if (etName.text.toString().isEmpty()){
                                    if (etLocation.text.toString().isNotEmpty()) {
                                        curTrip.name = "Trip to " + etLocation.text.toString()
                                    }
                                } else {
                                    curTrip.name = etName.text.toString()
                                }
                                if (etLocation.text.toString().isNotEmpty()){
                                    curTrip.location =etLocation.text.toString()
                                }
                                if (etStartDate.text.toString().isNotEmpty()){
                                    curTrip.startDate =etStartDate.text.toString()
                                }
                                if (etStartDate.text.toString().isNotEmpty()){
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
            tvCost.text = curTrip.startDate
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
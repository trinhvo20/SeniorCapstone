package com.example.itin.adapters

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.itin.R
import com.example.itin.classes.Trip
import kotlinx.android.synthetic.main.trip_item.view.*

class PreviousTripAdapter(

    private val context: Context,
    private val previousTrips: MutableList<Trip>,
    private val listener: OnItemClickListener

) : RecyclerView.Adapter<PreviousTripAdapter.PreviousTripViewHolder>() {

    inner class PreviousTripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivMenu: ImageView = itemView.findViewById(R.id.ivMenu)

        init {
            ivMenu.setOnClickListener { popupMenu(it) }
        }

        // this function handles the popup menu for each item in the trips list
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
                        val etStartDate = view.findViewById<EditText>(R.id.etStartDate)
                        val etEndDate = view.findViewById<EditText>(R.id.etEndDate)

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
                                //trips[adapterPosition].delByName(trips[adapterPosition].name)
                                //trips[adapterPosition].sendToDB()
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
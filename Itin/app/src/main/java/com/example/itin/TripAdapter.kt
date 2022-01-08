package com.example.itin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.trip_item.view.*

class TripAdapter(
    private val trips: MutableList<Trip> // parameter: a mutable list of trip items
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    // create a view holder: holds a layout of a specific item
    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

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
            tvLocation.text = curTrip.location
            tvStartDate.text = curTrip.startDate
            tvEndDate.text = curTrip.endDate
        }
    }

    override fun getItemCount(): Int {
        return trips.size
    }

}
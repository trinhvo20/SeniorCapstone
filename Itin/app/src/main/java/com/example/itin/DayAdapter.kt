package com.example.itin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.trip_item.view.*

class DayAdapter(
    private val days: List<Day>, // parameter: a mutable list of trip items
) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

    // create a view holder: holds a layout of a specific item
    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v  = inflater.inflate(R.layout.trip_day_item,parent,false)
        return DayViewHolder(v)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val curday = days[position]
        holder.itemView.apply{
            // get the data from our trips list and put them in the corresponding TextView in trip_item.xml
            tvName.text = curday.daynumber
        }
    }

    override fun getItemCount(): Int {
        return  days.size
    }
}


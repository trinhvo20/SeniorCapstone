package com.example.itin

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.itin.classes.Day
import kotlinx.android.synthetic.main.trip_day_item.view.*
import kotlinx.android.synthetic.main.trip_item.view.tvName

class DayAdapter(
    private val context: Context,
    private val days: List<Day>, // parameter: a mutable list of day items
    private val listener: ActivityAdapter.OnItemClickListener
) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

    // create a view holder: holds a layout of a specific item
    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val recyclerView : RecyclerView = itemView.rvActivities
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v  = inflater.inflate(R.layout.trip_day_item,parent,false)
        return DayViewHolder(v)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val curDay = days[position]

        holder.itemView.apply{
            // get the data from our days list and put them in the corresponding TextView in trip_day_item.xml
            tvName.text = "Day " + curDay.daynumber
        }

        // makes the sub recyclerview work, no sure why but it does
        holder.recyclerView.apply{
            layoutManager = LinearLayoutManager(holder.recyclerView.context,RecyclerView.VERTICAL,false)
            adapter = ActivityAdapter(context,curDay.activities,listener)
        }


    }

    override fun getItemCount(): Int {
        return  days.size
    }


}
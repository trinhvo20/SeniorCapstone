package com.example.itin

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.itin.classes.Activity
import com.example.itin.classes.Day
import kotlinx.android.synthetic.main.trip_day_item.view.*
import kotlinx.android.synthetic.main.trip_item.view.tvName

class DayAdapter(
    private val incontext: Context,
    private val indays: List<Day>, // parameter: a mutable list of day items
    private val listener: ActivityAdapter.OnItemClickListener
) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {
    val context = incontext
    val days = indays

    // create a view holder: holds a layout of a specific item
    class DayViewHolder(itemView: View, context: Context, days : List<Day>) : RecyclerView.ViewHolder(itemView){
        val recyclerView : RecyclerView = itemView.rvActivities
        val ivAdd: ImageView

        init {
            ivAdd = itemView.findViewById<ImageView>(R.id.ivAdd)
            ivAdd.setOnClickListener { addAnActivity(it, context, days) }
        }

        private fun addAnActivity(view : View, context : Context, days : List<Day>) {

            (this as ItineraryActivity).addActivity(view, context, days[adapterPosition])

        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v  = inflater.inflate(R.layout.trip_day_item,parent,false)
        return DayViewHolder(v,context,days)
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
            adapter = ActivityAdapter(context,curDay.activities,listener,position)
        }


    }

    override fun getItemCount(): Int {
        return  days.size
    }


}
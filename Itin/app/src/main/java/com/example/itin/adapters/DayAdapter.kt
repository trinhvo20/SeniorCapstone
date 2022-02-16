package com.example.itin

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.itin.R
import com.example.itin.classes.Activity
import com.example.itin.classes.Day
import kotlinx.android.synthetic.main.trip_day_item.view.*
import kotlinx.android.synthetic.main.trip_item.view.tvName
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class DayAdapter(
    private val context: Context,
    private val days: MutableList<Day>, // parameter: a mutable list of day items
    private val listener: ActivityAdapter.OnItemClickListener
) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {


    // create a view holder: holds a layout of a specific item
    //-----> Needs to be inner class for shit to work Remember that if you copy this code for later <-----
    @RequiresApi(Build.VERSION_CODES.O)
    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val recyclerView : RecyclerView = itemView.rvActivities
        private val ivAdd: ImageView = itemView.findViewById<ImageView>(R.id.ivAdd)

        init {
            ivAdd = itemView.findViewById<ImageView>(R.id.ivAdd)
            ivAdd.setOnClickListener { addAnActivity(it) }
        }

        // function to add an activity to a day
        @RequiresApi(Build.VERSION_CODES.O)
        private fun addAnActivity(view : View) {
            val curDay = days[adapterPosition]


            val view = LayoutInflater.from(context).inflate(R.layout.add_activity, null)

            val etName = view.findViewById<EditText>(R.id.etName)
            val etTime = view.findViewById<EditText>(R.id.etTime)
            val etLocation = view.findViewById<EditText>(R.id.etLocation)
            val etCost = view.findViewById<EditText>(R.id.etCost)
            val etNotes = view.findViewById<EditText>(R.id.etNotes)


            val newDialog = AlertDialog.Builder(context)
            newDialog.setView(view)

            newDialog.setPositiveButton("Add") { dialog, _ ->
                val location = etLocation.text.toString()
                val cost = etCost.text.toString()
                val notes = etNotes.text.toString()
                val time = etTime.text.toString()

                val name = if (etName.text.toString().isEmpty()) {
                    "$location"
                } else {
                    etName.text.toString()
                }

                val activity = Activity(name, time, location, cost, notes)
                curDay.activities.add(activity)

                activitysort(curDay)
                notifyDataSetChanged()
                Toast.makeText(context, "Activity Added", Toast.LENGTH_SHORT).show()
                dialog.dismiss()

            }

            newDialog.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(context, "Canceled", Toast.LENGTH_SHORT).show()
            }

            newDialog.create()
            newDialog.show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    // function to sort the activities on each of the day, it is a modified Insertion sort
    private fun activitysort (curday: Day){
        var formatter = DateTimeFormatter.ofPattern("h:ma")

            for (i in 0 until curday.activities.size) {
                val key = curday.activities[i]

//                if (key != null) {
//                    println(key.time)
//                }

                var j = i - 1

                if (key != null) {
                    while (j >= 0 && LocalTime.parse(curday.activities[j]?.time, formatter).isAfter(LocalTime.parse(key.time, formatter))){
                        curday.activities[j + 1] = curday.activities[j]
                        j--
                    }
                }
                curday.activities[j + 1] = key
            }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v  = inflater.inflate(R.layout.trip_day_item,parent,false)
        return DayViewHolder(v)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val curDay = days[position]

        holder.itemView.apply{
            // get the data from our days list and put them in the corresponding TextView in trip_day_item.xml
            tvName.text = "Day " + curDay.daynumber
        }

        // makes the sub recyclerview work, not sure why but it does
        holder.recyclerView.apply{
            layoutManager = LinearLayoutManager(holder.recyclerView.context,RecyclerView.VERTICAL,false)
            adapter = ActivityAdapter(context,curDay.activities,listener,position)
        }


    }

    override fun getItemCount(): Int {
        return  days.size
    }


}
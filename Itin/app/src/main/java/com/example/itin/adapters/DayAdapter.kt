package com.example.itin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Build
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.itin.R
import com.example.itin.adapters.ActivityAdapter
import com.example.itin.classes.Activity
import com.example.itin.classes.Day
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.trip_day_item.view.*
import kotlinx.android.synthetic.main.trip_item.view.tvName
import org.w3c.dom.Text
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class DayAdapter(
    private val context: Context,
    private val days: MutableList<Day>, // parameter: a mutable list of day items
    private val listener: ActivityAdapter.OnItemClickListener,
) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {


    // create a view holder: holds a layout of a specific item
    //-----> Needs to be inner class for shit to work Remember that if you copy this code for later <-----
    @RequiresApi(Build.VERSION_CODES.O)
    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val recyclerView : RecyclerView = itemView.rvActivities
        private var ivAdd: ImageView = itemView.findViewById(R.id.ivAdd)

        init {
            ivAdd.setOnClickListener { addAnActivity(it) }
        }

        // function to add an activity to a day
        @RequiresApi(Build.VERSION_CODES.O)
        private fun addAnActivity(view : View) {
            val firebaseAuth = FirebaseAuth.getInstance()
            val masterTripList = FirebaseDatabase.getInstance().getReference("masterTripList")

            val curDay = days[adapterPosition]
            val tripInstance = masterTripList.child(curDay.tripID.toString())
            val dayInstance = tripInstance.child("Days").child((curDay.dayInt-1).toString())


            val view = LayoutInflater.from(context).inflate(R.layout.add_activity, null)

            val etName = view.findViewById<EditText>(R.id.etName)
            val tvTime = view.findViewById<TextView>(R.id.tvTime)
            val etLocation = view.findViewById<EditText>(R.id.etLocation)
            val etCost = view.findViewById<EditText>(R.id.etCost)
            val etNotes = view.findViewById<EditText>(R.id.etNotes)

            val ibTimePicker = view.findViewById<View>(R.id.ibTimePick)
            ibTimePicker.setOnClickListener{
                val hour = 11
                val minute = 59

                val tpd = TimePickerDialog(context,TimePickerDialog.OnTimeSetListener(function = { view, h, m ->

                    //Toast.makeText(context, h.toString() + " : " + m , Toast.LENGTH_LONG).show()
                    var input = h.toString() + ":" + m

                    val df = SimpleDateFormat("H:m")
                    val outputformat = SimpleDateFormat("h:ma")
                    tvTime.text = outputformat.format(df.parse(input))


                }),hour,minute,false)

                tpd.show()
            }


            val newDialog = AlertDialog.Builder(context)
            newDialog.setView(view)

            newDialog.setPositiveButton("Add") { dialog, _ ->
                val location = etLocation.text.toString()
                val cost = etCost.text.toString()
                val notes = etNotes.text.toString()
                val time = tvTime.text.toString()

                val name = if (etName.text.toString().isEmpty()) {
                    "$location"
                } else {
                    etName.text.toString()
                }

                val activity = Activity(name, time, location, cost, notes,curDay.tripID,"")

                curDay.activities.add(activity)
                sendActivityToDB(curDay,activity)

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
    /*
    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendActivityToDB(dayInstance: DatabaseReference, activity: Activity) {
        // increment the activity count by 1
        dayInstance.child("ActivityCount").setValue(activity.actID + 1)
        // navigate to the correct activity in the day
        val activityInstance = dayInstance.child(activity.actID.toString())
        if (activity != null) {
            activityInstance.child("Name").setValue(activity.name)
            activityInstance.child("Time").setValue(activity.time)
            activityInstance.child("Location").setValue(activity.location)
            activityInstance.child("Cost").setValue(activity.cost)
            activityInstance.child("Notes").setValue(activity.notes)
            activityInstance.child("ActivityID").setValue(activity.actID)
            activityInstance.child("TripID").setValue(activity.tripID)
        }
    }
    */

    private fun sendActivityToDB(curDay: Day, activity: Activity) {
        val dayInstance = FirebaseDatabase.getInstance().getReference("masterTripList")
            .child(curDay.tripID.toString()).child("Days").child((curDay.dayInt-1).toString())
        dayInstance.child("ActivityCount").setValue(curDay.activities.size)

        val activityInstance = dayInstance.push()

        if (activity != null) {
            activity.actID = activityInstance.key.toString()
            activityInstance.setValue(activity)
        }
    }
}

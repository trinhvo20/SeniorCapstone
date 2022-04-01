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
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.itin.R
import com.example.itin.adapters.ActivityAdapter
import com.example.itin.classes.Activity
import com.example.itin.classes.Day
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
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
            val masterTripList = FirebaseDatabase.getInstance().getReference("masterTripList")

            val curDay = days[adapterPosition]
            val tripInstance = masterTripList.child(curDay.tripID.toString())
            val dayInstance = tripInstance.child("Days").child((curDay.dayInt-1).toString())

            val view = LayoutInflater.from(context).inflate(R.layout.add_activity, null)

            var location = ""
            val etName = view.findViewById<EditText>(R.id.etName)
            val tvTime = view.findViewById<TextView>(R.id.tvTime)
            val etCost = view.findViewById<EditText>(R.id.etCost)
            val etNotes = view.findViewById<EditText>(R.id.etNotes)

            // Handle AutoComplete Places Search from GoogleAPI
            if (!Places.isInitialized()) {
                Places.initialize(context, context.getString(R.string.API_KEY))
            }
            val placesClient = Places.createClient(context)
            val autocompleteFragment =
                (context as AppCompatActivity).supportFragmentManager.findFragmentById(R.id.etActLocation1) as AutocompleteSupportFragment
            autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS))
            autocompleteFragment.setOnPlaceSelectedListener(object :
                PlaceSelectionListener {
                override fun onPlaceSelected(place: Place) {
                    location = "${place.name}\n${place.address}"
                    Log.i("Places", "Place: ${place.address}, ${place.id}")
                }

                override fun onError(status: Status) {
                    // TODO: Handle the error.
                    Log.i("Places", "An error occurred: $status")
                }
            })

            val ibTimePicker = view.findViewById<View>(R.id.ibTimePick)
            ibTimePicker.setOnClickListener{
                val hour = 11
                val minute = 59

                val tpd = TimePickerDialog(context,TimePickerDialog.OnTimeSetListener(function = { view, h, m ->

                    //Toast.makeText(context, h.toString() + " : " + m , Toast.LENGTH_LONG).show()
                    var input = "$h:$m"

                    val df = SimpleDateFormat("H:mm")
                    val outputformat = SimpleDateFormat("h:mm a")
                    tvTime.text = outputformat.format(df.parse(input))


                }),hour,minute,false)

                tpd.show()
            }
            val newDialog = AlertDialog.Builder(context)
            newDialog.setView(view)

            newDialog.setPositiveButton("Add") { dialog, _ ->
                val cost = etCost.text.toString()
                val notes = etNotes.text.toString()
                val time = tvTime.text.toString()

                val name = etName.text.toString().ifBlank {
                    location.substringBefore("\n")
                }

                val activity = Activity(name, time, location, cost, notes,curDay.tripID,"")

                curDay.activities.add(activity)
                sendActivityToDB(curDay,activity)

                activitysort(curDay)
                notifyDataSetChanged()
                context.supportFragmentManager.beginTransaction().remove(autocompleteFragment).commit()
                Toast.makeText(context, "Activity Added", Toast.LENGTH_SHORT).show()
                dialog.dismiss()

            }

            newDialog.setNegativeButton("Cancel") { dialog, _ ->
                context.supportFragmentManager.beginTransaction().remove(autocompleteFragment).commit()
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
        var formatter = DateTimeFormatter.ofPattern("h:mm a")

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

    // Clean all elements of the recycler
    fun clear() {
        days.clear()
        notifyDataSetChanged()
    }

    // Add a list of items -- change to type used
    fun addAll(dayList: MutableList<Day>) {
        days.addAll(dayList)
        notifyDataSetChanged()
    }
}

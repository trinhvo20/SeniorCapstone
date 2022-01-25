package com.example.itin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.itin.classes.Activity
import kotlinx.android.synthetic.main.day_activity_item.view.*

class ActivityAdapter(
    private val Activities: List<Activity?>, // parameter: a mutable list of trip items
) : RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder>() {

    // create a view holder: holds a layout of a specific item
    class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v  = inflater.inflate(R.layout.day_activity_item,parent,false)
        return ActivityViewHolder(v)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val curActivity = Activities[position]
        holder.itemView.apply{
            // get the data from our trips list and put them in the corresponding TextView in trip_item.xml
            if (curActivity != null) {
                tvActivityName.text = curActivity.name
                tvActivityTime.text = curActivity.time
            }

        }
    }

    override fun getItemCount(): Int {
        return  Activities.size
    }
}
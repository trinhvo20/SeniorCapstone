package com.example.itin.adapters

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.itin.R
import com.example.itin.classes.Activity
import com.example.itin.classes.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.day_activity_item.view.*

class ActivityAdapter(
    private val context: Context,
    private val Activities: MutableList<Activity?>, // parameter: a mutable list of Activity items
    private val listener: OnItemClickListener,
    private val dayPos: Int,
    private val curTrip: Trip,
) : RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder>() {

    private lateinit var firebaseAuth: FirebaseAuth
    private val viewers = curTrip.viewers

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v  = inflater.inflate(R.layout.day_activity_item,parent,false)
        return ActivityViewHolder(v)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val curActivity = Activities[position]
        holder.itemView.apply{
            firebaseAuth = FirebaseAuth.getInstance()
            val firebaseUser = firebaseAuth.currentUser
            val uid = firebaseUser!!.uid

            // get the data from our Activities list and put them in the corresponding TextView in day_activity_item.xml
            if (curActivity != null) {
                tvActivityName.text = curActivity.name
                tvActivityTime.text = curActivity.time
            }

            if (viewers[uid] == 3 || !curTrip.active){
                removeBtn.visibility = View.INVISIBLE
                removeBtn.isClickable = false
            }
        }

        // handle RecyclerView clickable
        holder.itemView.setOnClickListener {
            listener.onItemClick(position,dayPos)
        }
    }

    override fun getItemCount(): Int {
        return  Activities.size
    }

    // this interface will handle the RecyclerView clickable
    interface  OnItemClickListener {
        fun onItemClick(position: Int, dayPos: Int)
    }

    // create a view holder: holds a layout of a specific item
    inner class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val removeBtn: ImageView = itemView.findViewById(R.id.removeBtn)

        init {
            removeBtn.setOnClickListener {removeActivity()}
        }

        private fun removeActivity() {
            firebaseAuth = FirebaseAuth.getInstance()
            val firebaseUser = firebaseAuth.currentUser
            val uid = firebaseUser!!.uid

            if (viewers[uid] == 1 || viewers[uid] == 2){
                val curActivity = Activities[adapterPosition]

                val dialog = AlertDialog.Builder(context)
                dialog.setTitle("Delete")
                    .setIcon(R.drawable.ic_warning)
                    .setMessage("Are you sure delete this activity?")
                    .setPositiveButton("Yes") { dialog, _ ->
                        Activities.removeAt(adapterPosition)
                        notifyDataSetChanged()
                        if (curActivity != null) {
                            removeActivityFromDB(curActivity)
                        }

                        Toast.makeText(context, "Successfully Deleted", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
                }
            else{
                Toast.makeText(context, "You do not have permission to perform this action", Toast.LENGTH_SHORT).show()
            }
        }

        private fun removeActivityFromDB(curActivity: Activity) {
            val tripID = curActivity?.tripID.toString()
            val dayID = dayPos.toString()
            val curDay = FirebaseDatabase.getInstance().getReference("masterTripList")
                .child(tripID).child("Days").child(dayID)

            val activity = curDay.orderByChild("name").equalTo(curActivity.name)
            activity.addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (i in snapshot.children){
                        i.ref.removeValue()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
            //curDay.child(actID).removeValue()
            curDay.child("ActivityCount").setValue(Activities.size)
        }
    }
}
package com.example.itin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itin.adapters.ViewerAdapter
import com.example.itin.classes.Trip
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_viewer.*

class ViewerActivity : AppCompatActivity() {

    private lateinit var viewerList : MutableMap<String,Int>
    private lateinit var viewerAdapter: ViewerAdapter
    private lateinit var uid : String
    private lateinit var trip : Trip

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer)

        uid = intent.getStringExtra("uid").toString()
        trip = intent.getSerializableExtra("trip") as Trip

        viewerList = mutableMapOf()
        viewerAdapter = ViewerAdapter(viewerList)
        rvViewer.adapter = viewerAdapter
        rvViewer.layoutManager = LinearLayoutManager(this)
        loadViewerFromDB()

        viewerBackBtn.setOnClickListener { finish() }

        // This following codes handle Pull-to-Refresh the Days RecyclerView
        // It will clear the days list and load all days from the DB again
        viewerSwipeContainer.setOnRefreshListener {
            viewerAdapter.clear()
            loadViewerFromDB()
            viewerSwipeContainer.isRefreshing = false
        }
        // Configure the refreshing colors
        viewerSwipeContainer.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
    }

    private fun loadViewerFromDB() {
        val tripId = trip.tripID.toString()
        FirebaseDatabase.getInstance().reference.child("masterTripList").child(tripId).child("Viewers")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    viewerList.clear()
                    for (child in snapshot.children) {
                        var perm = 3
                        if(child.child("Perm").exists()){
                            perm = child.child("Perm").value.toString().toInt()
                        }
                        viewerList[child.key.toString()] = perm
                    }
                    viewerAdapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
    }
}
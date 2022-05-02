package com.example.itin.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.itin.R
import com.example.itin.ViewerActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.friend_share_item.view.*
import kotlinx.android.synthetic.main.viewer_item.view.*
import java.io.File

class ViewerAdapter(
    private val viewerList: MutableMap<String, Int>,
    private val tripID: Int,
    private val context: ViewerActivity
) : RecyclerView.Adapter<ViewerAdapter.ViewerViewHolder>() {
    inner class ViewerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewerViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v  = inflater.inflate(R.layout.viewer_item,parent,false)
        return ViewerViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewerViewHolder, position: Int) {
        var curViewer = viewerList.keys.elementAt(position)
        var open = false

        val rotateOpen: Animation by lazy { AnimationUtils.loadAnimation(context, R.anim.rotate_open_anim) }
        val rotateClose: Animation by lazy { AnimationUtils.loadAnimation(context, R.anim.rotate_close_anim) }

        val firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUser = firebaseAuth.currentUser
        val uid = firebaseUser!!.uid

        holder.itemView.apply {

            if(viewerList[uid] == 1 && viewerList.keys.size > 1){
                ivSetPerm.visibility = View.VISIBLE
                ivSetPerm.isClickable = true
            }
            else{
                ivSetPerm.visibility = View.INVISIBLE
                ivSetPerm.isClickable = false
            }

            var curViewerPerm = viewerList[curViewer]
            clOP1.visibility = View.GONE
            clOP2.visibility = View.GONE
            clOP3.visibility = View.GONE
            removeFromTrip.visibility = View.GONE

            clOP1.isClickable = false
            clOP2.isClickable = false
            clOP3.isClickable = false
            removeFromTrip.isClickable = false

            ivSetPerm.setOnClickListener {
                //Toast.makeText(context,"perm $curViewerPerm", Toast.LENGTH_LONG).show()
                if(!open) {
                    ivSetPerm.startAnimation(rotateOpen)

                    if (curViewerPerm != 1 && (curViewerPerm == 2 || curViewerPerm == 3)) {
                        clOP1.visibility = View.VISIBLE
                        clOP1.isClickable = true
                    }

                    if (curViewerPerm != 2 && (curViewerPerm == 1 || curViewerPerm == 3)) {
                        clOP2.visibility = View.VISIBLE
                        clOP2.isClickable = true
                    }

                    if ((curViewerPerm != 3 && (curViewerPerm == 1 || curViewerPerm == 2))) {
                        clOP3.visibility = View.VISIBLE
                        clOP3.isClickable = true
                    }

                    if (curViewer == uid) {
                        removeFromTrip.visibility = View.GONE
                        removeFromTrip.isClickable = false
                        open = true
                    } else {
                        removeFromTrip.visibility = View.VISIBLE
                        removeFromTrip.isClickable = true
                        open = true
                    }
                }
                else{
                    ivSetPerm.startAnimation(rotateClose)
                    clOP1.visibility = View.GONE
                    clOP2.visibility = View.GONE
                    clOP3.visibility = View.GONE
                    removeFromTrip.visibility = View.GONE
                    clOP1.isClickable = false
                    clOP2.isClickable = false
                    clOP3.isClickable = false
                    removeFromTrip.isClickable = false
                    open = false
                }
            }

            clOP1.setOnClickListener {
                clOP1.visibility = View.GONE
                clOP2.visibility = View.GONE
                clOP3.visibility = View.GONE
                clOP1.isClickable = false
                clOP2.isClickable = false
                clOP3.isClickable = false
                open = false
                changeperm(curViewer,1)
                viewerList[curViewer] = 1
                Toast.makeText(context,"${viewerUsername.text} has been made an Owner", Toast.LENGTH_SHORT).show()
            }

            clOP2.setOnClickListener {
                clOP1.visibility = View.GONE
                clOP2.visibility = View.GONE
                clOP3.visibility = View.GONE
                clOP1.isClickable = false
                clOP2.isClickable = false
                clOP3.isClickable = false
                open = false
                changeperm(curViewer,2)
                viewerList[curViewer] = 2
                Toast.makeText(context,"${viewerUsername.text} has been made an Editor", Toast.LENGTH_SHORT).show()
            }

            clOP3.setOnClickListener {
                clOP1.visibility = View.GONE
                clOP2.visibility = View.GONE
                clOP3.visibility = View.GONE
                clOP1.isClickable = false
                clOP2.isClickable = false
                clOP3.isClickable = false
                open = false
                changeperm(curViewer,3)
                viewerList[curViewer] = 3
                Toast.makeText(context,"${viewerUsername.text} has been made a Viewer", Toast.LENGTH_SHORT).show()
            }

            removeFromTrip.setOnClickListener {
                clOP1.visibility = View.GONE
                clOP2.visibility = View.GONE
                clOP3.visibility = View.GONE
                clOP1.isClickable = false
                clOP2.isClickable = false
                clOP3.isClickable = false
                open = false

                FirebaseDatabase.getInstance().getReference("users").child(curViewer).child("trips").child("Trip $tripID").removeValue()
                FirebaseDatabase.getInstance().getReference("masterTripList").child(tripID.toString()).child("Viewers").child(curViewer).removeValue()

          }


            var storageReference = FirebaseStorage.getInstance().getReference("Users/$curViewer.jpg")
            val localFileV2 = File.createTempFile("tempImage", "jpg")
            storageReference.getFile(localFileV2).addOnSuccessListener {
                val bitmap = BitmapFactory.decodeFile(localFileV2.absolutePath)
                viewerPP.setImageBitmap(bitmap)
            }.addOnFailureListener {
                viewerPP.setImageResource(R.drawable.profile)
            }

            FirebaseDatabase.getInstance().reference.child("users")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (user in snapshot.children) {
                            if (user.key == curViewer) {

                                viewerPerm.visibility = View.INVISIBLE

                                val username = user.child("userInfo").child("username").value.toString()
                                val fullname = user.child("userInfo").child("fullName").value.toString()

                                if(viewerList[user.child("userInfo").child("uid").value.toString()] == 1){
                                    viewerPerm.text = "Owner"
                                    viewerPerm.visibility = View.VISIBLE
                                }

                                if(viewerList[user.child("userInfo").child("uid").value.toString()] == 2){
                                    viewerPerm.text = "Editor"
                                    viewerPerm.visibility = View.VISIBLE
                                }

                                if(fullname.length > 12){
                                    var shortname = fullname.substring(0..9)
                                    viewerFullName.text = shortname+"..."
                                }
                                else{
                                    viewerFullName.text = fullname
                                }

                                viewerUsername.text = username
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }
                })
        }
    }

    private fun changeperm(curViewer: String, newperm: Int) {
        val curTrip = FirebaseDatabase.getInstance().getReference("masterTripList").child("$tripID")
        curTrip.child("Viewers").child(curViewer).child("Perm").setValue(newperm)
    }

    override fun getItemCount(): Int {
        return viewerList.size
    }

    fun clear() {
        viewerList.clear()
        notifyDataSetChanged()
    }
}
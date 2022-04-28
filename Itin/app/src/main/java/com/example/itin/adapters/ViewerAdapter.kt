package com.example.itin.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.itin.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.viewer_item.view.*
import java.io.File

class ViewerAdapter(
    private val viewerList: MutableMap<String, Int>
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
        holder.itemView.apply {

            var curViewerPerm = viewerList[curViewer]
            clOP1.visibility = View.GONE
            clOP2.visibility = View.GONE
            clOP3.visibility = View.GONE

            ivSetPerm.setOnClickListener {
                //Toast.makeText(context,"perm $curViewerPerm", Toast.LENGTH_LONG).show()
                if(!open) {
                    if (curViewerPerm != 1 && (curViewerPerm == 2 || curViewerPerm == 3)) {
                        clOP1.visibility = View.VISIBLE
                    }

                    if (curViewerPerm != 2 && (curViewerPerm == 1 || curViewerPerm == 3)) {
                        clOP2.visibility = View.VISIBLE
                    }

                    if ((curViewerPerm != 3 && (curViewerPerm == 1 || curViewerPerm == 2)) || (curViewerPerm != 1 && curViewerPerm != 2 && curViewerPerm != 3)) {
                        clOP3.visibility = View.VISIBLE
                    }

                    open = true
                }
                else{
                    clOP1.visibility = View.GONE
                    clOP2.visibility = View.GONE
                    clOP3.visibility = View.GONE
                    open = false
                }
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
                                val username = user.child("userInfo").child("username").value.toString()
                                val fullname = user.child("userInfo").child("fullName").value.toString()
                                viewerUsername.text = username
                                viewerFullName.text = fullname
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }
                })
        }
    }

    override fun getItemCount(): Int {
        return viewerList.size
    }

    fun clear() {
        viewerList.clear()
        notifyDataSetChanged()
    }
}
package com.example.itin.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
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

class ViewerAdapter (
    private val viewerList : MutableList<String>
) : RecyclerView.Adapter<ViewerAdapter.ViewerViewHolder>() {
    inner class ViewerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private val ivSetPerm: ImageView = itemView.findViewById(R.id.ivSetPerm)
        private val clDropdown: ConstraintLayout = itemView.findViewById(R.id.clDropdown)
        private val clOP1: ConstraintLayout = itemView.findViewById(R.id.clOP1)
        private val clOP2: ConstraintLayout = itemView.findViewById(R.id.clOP2)
        private val clOP3: ConstraintLayout = itemView.findViewById(R.id.clOP3)
        private var open = false
        init {
            clDropdown.removeAllViews()
            ivSetPerm.setOnClickListener {
                toggledropdown()
            }
        }

        private fun toggledropdown() {
            var params = clDropdown.getLayoutParams()
            if(!open) {
//                params.height = -1
//                clDropdown.setLayoutParams(params)
//                open = true

                clDropdown.addView(clOP1)
                clDropdown.addView(clOP2)
                clDropdown.addView(clOP3)
                open = true
            }
            else{
//                params.height = 0
//                clDropdown.setLayoutParams(params)
//                open = false

                clDropdown.removeAllViews()
                open = false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewerViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v  = inflater.inflate(R.layout.viewer_item,parent,false)
        return ViewerViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewerViewHolder, position: Int) {
        var curViewer = viewerList[position]
        holder.itemView.apply {


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
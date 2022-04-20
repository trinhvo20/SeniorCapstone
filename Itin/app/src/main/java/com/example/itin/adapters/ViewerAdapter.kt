package com.example.itin.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    inner class ViewerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

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
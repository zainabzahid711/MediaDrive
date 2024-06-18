package com.example.filedrive

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.filedrive.ui.ImageAdapter
import com.example.filedrive.ui.UrlDataClass
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.storage


class Favourite : Fragment() {

    private var dbRefListener: ValueEventListener? = null

    //storing variables
    private lateinit var dbRef: DatabaseReference
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    //fetching declaration
    private lateinit var recyclerView: RecyclerView
    private lateinit var listImages: ArrayList<UrlDataClass>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_favourite, container, false)

        recyclerView = root.findViewById(R.id.recycler)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        listImages = arrayListOf()
        val imageLoader = root.findViewById<ProgressBar>(R.id.imageLoader)
        val noImage = root.findViewById<ImageView>(R.id.noImageFoundFav)

        // Initialize dbRef after Firebase initialization
        userId?.let {
            dbRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)
                .child("galleryImagesUrl")
        }



//        fetching  data
        dbRefListener = dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listImages.clear()
                if (snapshot.exists()){
                    noImage.visibility = View.GONE
                    for (image in snapshot.children){
                        val urlData  = image.getValue(UrlDataClass::class.java)
                        if (urlData  != null && urlData.deleteFlag == false && urlData.favFlag == true) {
                            listImages.add(urlData)
                        }
                    }

                    imageLoader.visibility= View.GONE
                    val adapter = ImageAdapter(requireActivity(), listImages, {}, {
                            imgObj ->

                        val builder = AlertDialog.Builder(requireContext())
                        builder.setTitle("Options")
                            .setItems(arrayOf("Remove from Favorites")) { _, which ->
                                when (which) {
                                    0 -> removeFav(imgObj)
                                }
                            }
                            .setNegativeButton("Cancel") { dialog, _ ->
                                dialog.dismiss()
                            }
                        builder.show()
                    })

                    recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
                    recyclerView.adapter = adapter
                }
                else{
                    imageLoader.visibility= View.GONE
                    noImage.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), error.toString(), Toast.LENGTH_SHORT).show()
            }

        })


        return root
    }


    private fun removeFav(imageData: UrlDataClass) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {

            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (imageSnapshot in snapshot.children) {
                        val urlData = imageSnapshot.getValue(UrlDataClass::class.java)
                        if (urlData != null && urlData.url == imageData.url) {
                            imageSnapshot.ref.child("favFlag").setValue(false)
                            Toast.makeText(requireContext(), "Removed", Toast.LENGTH_SHORT).show()
                        }
                    }
                    return
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), error.toString(), Toast.LENGTH_SHORT).show()
                }
            })
        }
    }


    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.showFab()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        dbRefListener?.let {
            dbRef.removeEventListener(it)
        }
    }

}
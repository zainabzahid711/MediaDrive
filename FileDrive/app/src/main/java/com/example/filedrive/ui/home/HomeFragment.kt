package com.example.filedrive.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.filedrive.databinding.FragmentHomeBinding
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.filedrive.MainActivity
import com.example.filedrive.R
import com.example.filedrive.ui.ImageAdapter
import com.example.filedrive.ui.UrlDataClass
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeFragment : Fragment() {

    private var dbRefListener: ValueEventListener? = null

    //storing variables
    private lateinit var dbRef: DatabaseReference

    //fetching declaration
    private lateinit var recyclerView: RecyclerView
    private lateinit var listImages: ArrayList<UrlDataClass>





    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root



        //=>  Code Start from here

        recyclerView= root.findViewById (R.id.recycler)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        listImages = arrayListOf()
        var imageLoader = root.findViewById<ProgressBar>(R.id.imageLoaderH)
        var noImage = root.findViewById<ImageView> (R.id.noImageFoundH)



        // Initialize dbRef after Firebase initialization
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            // If userId is not null, creating a reference to the database location
            // where the user's gallery images URLs are stored
            dbRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)
                .child("galleryImagesUrl")
        }


//        fetching  data
        dbRefListener = dbRef.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                listImages.clear()
                if (snapshot.exists()){
                    noImage.visibility = View.GONE
                    for (image in snapshot.children){
                        val urlData  = image.getValue(UrlDataClass::class.java)
                        if (urlData  != null && urlData.deleteFlag != true) {
                            listImages.add(urlData)
                        }
                    }

                    imageLoader.visibility= View.GONE
                    val adapter = ImageAdapter(requireActivity().applicationContext, listImages, { imageUrl ->
                    }, {
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

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.showFab()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        dbRefListener?.let {
            dbRef.removeEventListener(it)
        }
    }
}
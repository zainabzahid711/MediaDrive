package com.example.filedrive.ui.slideshow

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.filedrive.MainActivity
import com.example.filedrive.R
import com.example.filedrive.databinding.FragmentSlideshowBinding
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

class SlideshowFragment : Fragment() {

    private var dbRefListener: ValueEventListener? = null

    //storing variables
    private lateinit var dbRef: DatabaseReference
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    //fetching declaration
    private lateinit var recyclerView: RecyclerView
    private lateinit var listImages: ArrayList<UrlDataClass>




    private var _binding: FragmentSlideshowBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        val root: View = binding.root





        //=>  Code Start from here
        recyclerView= root.findViewById (R.id.recycler)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        listImages = arrayListOf()
        var imageLoader = root.findViewById<ProgressBar>(R.id.imageLoader)
        var noImage = root.findViewById<ImageView> (R.id.noImageFoundBin)



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
                        if (urlData  != null && urlData.deleteFlag == true) {
                            listImages.add(urlData)
                        }
                    }

                    imageLoader.visibility= View.GONE
                    val adapter = ImageAdapter(requireActivity(), listImages, {}, {
                     imgObj ->

                        val builder = AlertDialog.Builder(requireContext())
                        builder.setTitle("Options")
                            .setItems(arrayOf("Delete", "Empty RecycleBin","Restore", "Restore All")) { _, which ->
                                when (which) {
                                    0 -> confirmDelete(imgObj)
                                    1 -> deleteAll(imgObj)
                                    2 -> restoreImage(imgObj)
                                    3 -> restoreAllImages(imgObj)
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


    private fun confirmDelete(imageData: UrlDataClass) {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setMessage("Are you sure you want to delete this image?")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->

                val imageUrl = imageData.url

                val imageName = imageUrl?.substringAfterLast('F')
                val cleanImageName = imageName?.substringBefore('?')


                val storageRef = Firebase.storage.reference
                    .child("Gallery Images")
                    .child(userId.toString())
                    .child(cleanImageName.toString())

                storageRef.delete().addOnSuccessListener {

                    dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (imageSnapshot in snapshot.children) {
                                val urlData = imageSnapshot.getValue(UrlDataClass::class.java)
                                if (urlData != null && urlData.url == imageData.url && urlData.deleteFlag == true) {
                                    imageSnapshot.ref.removeValue()
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                requireContext(),
                                                "Image deleted successfully",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(
                                                requireContext(),
                                                "Failed to delete image: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    return
//                                break
                                }
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(requireContext(), error.toString(), Toast.LENGTH_SHORT).show()
                        }
                    })

                }

            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }

        val alert = dialogBuilder.create()
        alert.setTitle("Confirmation")
        alert.show()
    }

    private fun deleteAll(imageUrl: UrlDataClass) {
        // Implement logic for renaming the image
        // You can show a dialog or navigate to a screen for renaming the image
    }

    private fun restoreImage(imageData: UrlDataClass) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {

            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (imageSnapshot in snapshot.children) {
                        val urlData = imageSnapshot.getValue(UrlDataClass::class.java)
                        if (urlData != null && urlData.url == imageData.url) {
                            imageSnapshot.ref.child("deleteFlag").setValue(false)
                            Toast.makeText(requireContext(), "Image restored", Toast.LENGTH_SHORT).show()
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

    private fun restoreAllImages(imgObj: UrlDataClass) {

    }

    override fun onResume() {
        super.onResume()
        if (isAdded && isVisible && userVisibleHint) {
            (activity as? MainActivity)?.hideFab()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        dbRefListener?.let {
            dbRef.removeEventListener(it)
        }
    }
}
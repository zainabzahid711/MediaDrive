package com.example.filedrive.ui.gallery

import android.app.AlertDialog
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.filedrive.R
import com.example.filedrive.databinding.FragmentGalleryBinding
import com.example.filedrive.ui.ImageAdapter
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.storage
import androidx.activity.result.ActivityResultCallback
import androidx.recyclerview.widget.GridLayoutManager
import com.example.filedrive.MainActivity
import com.example.filedrive.ui.UrlDataClass
import java.io.File

class GalleryFragment : Fragment() {

    private var dbRefListener: ValueEventListener? = null

    //Declarations
    private lateinit var dbRef: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var listImages: ArrayList<UrlDataClass>



    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root


        //=>  Code Starting
        recyclerView= root.findViewById (R.id.recycler)
        listImages = arrayListOf()
        var imageLoader = root.findViewById<ProgressBar>(R.id.imageLoader)
        var noImage = root.findViewById<ImageView> (R.id.noImageFound)


        // Initialize dbRef after Firebase initialization
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            dbRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)
                .child("galleryImagesUrl")
        }


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
                    val adapter = ImageAdapter(requireActivity().applicationContext, listImages,
                        // Handle click event here
                     {},
                        // Handle long click event here
                    { imgObj ->
                        val builder = AlertDialog.Builder(requireContext())
                        builder.setTitle("Options")
                            .setItems(
                                if (imgObj.favFlag == true)
                                    arrayOf("Delete", "Download", "Remove from favorites")
                                else
                                    arrayOf("Delete", "Download", "Add to favorites")
                            ) { _, which ->
                                when (which) {
                                    0 -> confirmDelete(imgObj)
                                    1 -> downloadImage(imgObj)
                                    2 -> {
                                        if (imgObj.favFlag == true)
                                            removeFav(imgObj)
                                        else
                                            addFavorites(imgObj)
                                    }
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

                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {

                    dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (imageSnapshot in snapshot.children) {
                                val urlData = imageSnapshot.getValue(UrlDataClass::class.java)
                                if (urlData != null && urlData.url == imageData.url) {
                                    imageSnapshot.ref.child("deleteFlag").setValue(true)
                                    Toast.makeText(requireContext(), "image deleted", Toast.LENGTH_SHORT).show()
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
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }

        val alert = dialogBuilder.create()
        alert.setTitle("Confirmation")
        alert.show()
    }

    private fun downloadImage(imageUrl: UrlDataClass) {
//        image url be like in the format of
//        https://firebasestorage.googleapis.com/v0/b/filedrive-e5770.appspot.com/o/Gallery%20Images%2FopssM3K3xrQ3vUffvSJr5I04sdk2%2
//        F1704059319563?
//        alt=media&token=15e57842-1a15-4bd8-a9e4-0d8f5dacaf9b

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val imageUrl = imageUrl.url

        val imageName = imageUrl?.substringAfterLast('F')
        val cleanImageName = imageName?.substringBefore('?')


        // Construct the reference to the file in Firebase Storage
        val storageRef = Firebase.storage.reference
            .child("Gallery Images")
            .child(userId.toString())
            .child(cleanImageName.toString())

        val directoryName = "fileDriveImages"
        val directory = File(requireContext().getExternalFilesDir(null), directoryName)
        directory.mkdirs()
        val localFile = File(directory, "downloaded_image.jpg")

        //recode defected code
//        val directoryPath = "/fileDrive/Images"
//        val directory = File(Environment.getExternalStorageDirectory() ,directoryPath )
//        directory.mkdirs()
//        val localFile = File(directory, "downloaded_image.jpg")


        storageRef.getFile(localFile)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Image downloaded", Toast.LENGTH_SHORT).show()

                MediaStore.Images.Media.insertImage(
                    requireContext().contentResolver,
                    localFile.absolutePath,
                    cleanImageName,
                    "Image Description"
                )

                // Trigger media scanner to scan the newly added image
                MediaScannerConnection.scanFile(
                    requireContext(),
                    arrayOf(localFile.absolutePath),
                    arrayOf("image/jpeg"),
                    null
                )

            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), cleanImageName.toString()+"    " + exception.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun addFavorites(imageData: UrlDataClass) {

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {

            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (imageSnapshot in snapshot.children) {
                        val urlData = imageSnapshot.getValue(UrlDataClass::class.java)
                        if (urlData != null && urlData.url == imageData.url ) {
                            imageSnapshot.ref.child("favFlag").setValue(true)
                            Toast.makeText(requireContext(), "Added", Toast.LENGTH_SHORT).show()
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
        _binding = null
        dbRefListener?.let {
            dbRef.removeEventListener(it)
        }
    }
}
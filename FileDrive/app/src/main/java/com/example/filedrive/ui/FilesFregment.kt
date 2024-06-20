package com.example.filedrive.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.filedrive.R

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class FilesFregment : Fragment() {
    private lateinit var storageReference: StorageReference
    private var fileUri: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storageReference = FirebaseStorage.getInstance().reference
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_files_fregment, container, false)

        val buttonChooseFile: Button = view.findViewById(R.id.button_choose_file)
        val buttonUploadFile: Button = view.findViewById(R.id.button_upload_file)

        buttonChooseFile.setOnClickListener {
            chooseFile()
        }

        buttonUploadFile.setOnClickListener {
            uploadFile()
        }

        return view
    }


    private fun chooseFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        resultLauncher.launch(intent)
    }

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fileUri = result.data?.data
            view?.findViewById<Button>(R.id.button_upload_file)?.visibility = View.VISIBLE
        }
    }

    private fun uploadFile() {
        fileUri?.let {
            val fileName = UUID.randomUUID().toString()
            val fileRef = storageReference.child("uploads/$fileName")

            fileRef.putFile(it)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "File Uploaded Successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "File Upload Failed", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Toast.makeText(requireContext(), "No file selected", Toast.LENGTH_SHORT).show()
        }
    }



    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment FilesFregment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FilesFregment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
package com.example.filedrive.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.filedrive.R
import com.example.filedrive.util.FileUploadutil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

class FilesFregment : Fragment() {
    private lateinit var storageReference: StorageReference
    private var fileUri: Uri? = null

    private lateinit var filesAdapter: FilesAdapter
    private lateinit var filesList: MutableList<FileItem>
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storageReference = FirebaseStorage.getInstance().reference
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_files_fragment, container, false)

        val buttonChooseFile: Button = view.findViewById(R.id.button_choose_file)
        val buttonUploadFile: Button = view.findViewById(R.id.button_upload_file)

        buttonChooseFile.setOnClickListener {
            chooseFile()
        }

        buttonUploadFile.setOnClickListener {
            uploadFile()
        }

        // Initialize RecyclerView and adapter
        recyclerView = view.findViewById(R.id.recycler_view_files)
        filesList = mutableListOf()
        filesAdapter = FilesAdapter(requireContext(), filesList) {
            listFiles()
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = filesAdapter

        listFiles()

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
            FileUploadutil.uploadFile(it, requireContext()) {
                listFiles()
            }
        } ?: run {
            Toast.makeText(requireContext(), "No file selected", Toast.LENGTH_SHORT).show()
            Log.e("FilesFragment", "No file selected")
        }
    }


    object FileUploadUtil {

        fun uploadFile(fileUri: Uri, context: Context, onSuccess: () -> Unit) {
            val auth = FirebaseAuth.getInstance()

            if (auth.currentUser != null) {
                val uid = auth.currentUser!!.uid
                val storageRef = FirebaseStorage.getInstance().reference
                val fileRef = storageRef.child("uploads/$uid/${fileUri.lastPathSegment}")

                fileRef.putFile(fileUri)
                    .addOnSuccessListener {
                        Log.d("FileUploadUtil", "File uploaded successfully: ${fileUri.lastPathSegment}")
                        // Call onSuccess to list files after successful upload
                        onSuccess()
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FileUploadUtil", "Failed to upload file: ${exception.message}")
                        Toast.makeText(context, "Failed to upload file: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
                Log.e("FileUploadUtil", "User not authenticated")
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listFiles()
    }

    private fun listFiles() {
        val auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            val uid = auth.currentUser!!.uid
            val listRef = storageReference.child("uploads/$uid/")

            listRef.listAll()
                .addOnSuccessListener { listResult ->
                    filesList.clear()
                    Log.d("FilesFragment", "Files found: ${listResult.items.size}")

                    for (item in listResult.items) {
                        Log.d("FilesFragment", "File: ${item.name}")
                        // Only log the file name without getting the download URL
                        filesList.add(FileItem(item.name, "")) // Add file name without URL
                    }

                    filesAdapter.notifyDataSetChanged()

                    // Optional: Display a Toast indicating the files were listed successfully
                    Toast.makeText(requireContext(), "Files listed successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Failed to list files: ${exception.message}", Toast.LENGTH_SHORT).show()
                    Log.e("FilesFragment", "Failed to list files", exception)
                }
        } else {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
            Log.e("FilesFragment", "User not authenticated")
        }
    }





    companion object {
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

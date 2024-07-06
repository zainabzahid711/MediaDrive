package com.example.filedrive.ui

import android.app.Activity
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
                        item.downloadUrl.addOnSuccessListener { uri ->
                            Log.d("FilesFragment", "File: ${item.name}, URL: $uri")
                            filesList.add(FileItem(item.name, uri.toString()))
                            filesAdapter.notifyDataSetChanged()
                        }.addOnFailureListener { exception ->
                            Log.e("FilesFragment", "Failed to get download URL: ${exception.message}")
                        }
                    }
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

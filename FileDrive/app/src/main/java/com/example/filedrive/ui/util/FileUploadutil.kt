package com.example.filedrive.util

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.UUID

object FileUploadutil {

    fun uploadFile(fileUri: Uri, context: Context, listFiles: () -> Unit) {
        val fileName = UUID.randomUUID().toString()
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid

        if (uid != null) {
            val storageReference: StorageReference = FirebaseStorage.getInstance().reference
            val fileRef = storageReference.child("uploads/$uid/$fileName")
            Log.d("FileUploadutil", "Uploading file: $fileUri with name: $fileName")

            fileRef.putFile(fileUri)
                .addOnSuccessListener {
                    Toast.makeText(context, "File Uploaded Successfully", Toast.LENGTH_SHORT).show()
                    listFiles()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "File Upload Failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                    Log.e("FileUploadutil", "File Upload Failed", exception)
                }
        } else {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
            Log.e("FileUploadutil", "User not authenticated")
        }
    }
}

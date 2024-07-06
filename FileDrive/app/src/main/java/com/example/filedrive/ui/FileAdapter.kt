package com.example.filedrive.ui

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.filedrive.R
import com.example.filedrive.util.FileUploadutil

data class FileItem(val name: String, val url: String)

class FilesAdapter(
    private val context: Context,
    private val filesList: List<FileItem>,
    private val listFiles: () -> Unit
) : RecyclerView.Adapter<FilesAdapter.FileViewHolder>() {

    class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileName: TextView = view.findViewById(R.id.file_name)
        val fileUrl: TextView = view.findViewById(R.id.file_url)
        val fileIcon: ImageView = view.findViewById(R.id.file_icon)
        val uploadButton: ImageView = view.findViewById(R.id.upload_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_itemfile, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val fileItem = filesList[position]
        holder.fileName.text = fileItem.name
        holder.fileUrl.text = fileItem.url
        holder.fileIcon.setImageResource(getFileIcon(fileItem.name))

        holder.uploadButton.setOnClickListener {
            val fileUri = Uri.parse(fileItem.url)
            FileUploadutil.uploadFile(fileUri, context, listFiles)
        }
    }

    private fun getFileIcon(fileName: String): Int {
        return when {
            fileName.endsWith(".pdf") -> R.drawable.baseline_assignment_24
            fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png") -> R.drawable.baseline_assignment_24
            fileName.endsWith(".mp4") || fileName.endsWith(".avi") || fileName.endsWith(".mov") -> R.drawable.baseline_assignment_24
            else -> R.drawable.baseline_assignment_24
        }
    }

    override fun getItemCount(): Int {
        return filesList.size
    }
}

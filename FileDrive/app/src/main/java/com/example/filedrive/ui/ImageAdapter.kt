package com.example.filedrive.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.filedrive.R
import android.content.Context
import android.content.Intent
import com.example.filedrive.ViewFullImage

class ImageAdapter(
    private val context: Context,
    private val listImages: ArrayList<UrlDataClass>,
    private val imageClickListener: (String) -> Unit,
    private val imageLongClickListener: (UrlDataClass) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ViewHolder> () {

    private var selectedItems: HashSet<Int> = HashSet()

    class ViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView){
        val imageView: ImageView = itemView.findViewById(R.id.imageView2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_image,parent,false)

        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageUrl = listImages[position].url.toString()
        val imgObj = listImages[position]

        Glide.with(context).load(imageUrl).into(holder.imageView)

        holder.itemView.isActivated = selectedItems.contains(position)

        // Click listener for image view
        holder.imageView.setOnClickListener {
            toggleSelection(position)
            val intent = Intent(context, ViewFullImage::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("IMAGE_URL", imageUrl)
            }
            context.startActivity(intent)
            imageClickListener.invoke(imageUrl)
        }

        // click listener for item
        holder.itemView.setOnClickListener {
            val intent = Intent(context, ViewFullImage::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("IMAGE_URL", imageUrl)
            }
            context.startActivity(intent)
        }

        // Long click listener
        holder.imageView.setOnLongClickListener {
            toggleSelection(position)

            imageLongClickListener.invoke(imgObj)
            true // Consume the long click event
        }

    }

    override fun getItemCount(): Int {
        return listImages.size
    }

    fun toggleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }
        notifyItemChanged(position)
    }

    fun clearSelection() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun getSelectedItems(): Set<Int> {
        return selectedItems
    }
}
package com.example.filedrive

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import com.bumptech.glide.Glide

class ViewFullImage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_full_image)

        supportActionBar?.hide()

        var imageView = findViewById<ImageView>  (R.id.fullImage)
        val imageUrl = intent.getStringExtra("IMAGE_URL")

        Glide.with(this@ViewFullImage)
            .load(imageUrl)
            .into(imageView)
    }
}
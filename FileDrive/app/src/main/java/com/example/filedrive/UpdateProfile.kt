package com.example.filedrive

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.storage

class UpdateProfile : AppCompatActivity() {

    private var profileUri : Uri? = null
    private  var dbStorage = Firebase.storage



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_profile)


        supportActionBar?.hide()

//        Starting code
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val dbRef = FirebaseDatabase.getInstance().getReference("Users").child(userId.toString())


        var updatedImage = findViewById<ImageView>(R.id.updateProfileImage)
        var updateName = findViewById<EditText>(R.id.updateName)
        var updateEmail = findViewById<EditText>(R.id.updateEmail)
        var saveBtn = findViewById<Button>(R.id.updateProfileBtn)
        var back = findViewById<ImageView> (R.id.updateProfileBack)

        back.setOnClickListener{
            startActivity(Intent(this@UpdateProfile,MainActivity::class.java))
        }

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userName = snapshot.child("name").getValue(String::class.java)
                val userEmail = snapshot.child("email").getValue(String::class.java)
                val pofileImage = snapshot.child("imageUrl").getValue(String::class.java)

                updateName.setText(userName)
                updateEmail.setText(userEmail)

                    Glide.with(this@UpdateProfile.applicationContext)
                        .load(pofileImage)
                        .placeholder(R.drawable.man)
                        .transform(CircleCrop())
                        .into(updatedImage)
                        .waitForLayout()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UpdateProfile, "Error: "+error.message, Toast.LENGTH_SHORT).show()
            }

        })

        var galleryImage = registerForActivityResult(
            ActivityResultContracts.GetContent(),
            ActivityResultCallback {Url ->
                Url?.let {
                    Glide.with(this@UpdateProfile)
                        .load(Url)
                        .transform(CircleCrop())
                        .into(updatedImage)
                        .waitForLayout()

                    profileUri = Url
                }
            }
        )


        updatedImage.setOnClickListener {
            galleryImage.launch("image/*")
        }

        updateEmail.setOnClickListener{
            updateEmail.error = "error"

            val location = IntArray(2)
            updateEmail.getLocationOnScreen(location)

            val toast = Toast.makeText(this, "Email Account can't be changed", Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.TOP or Gravity.START, location[0] + 0, location[1] + updateEmail.height)
            toast.show()
        }

        saveBtn.setOnClickListener {
            updateData()
        }
    }


    private fun updateData() {
        var updateName = findViewById<EditText>(R.id.updateName)
        var updateNameS = updateName.text.toString()
        var saveBtn = findViewById<Button>(R.id.updateProfileBtn)

        if(updateNameS.isEmpty())
        {
            updateName.error = "Enter your name"
            updateName.requestFocus()
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val dbRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)
            var nameFlag = false
            var profileFlag = false

            profileUri?.let {
                dbStorage.getReference("Profile Images").child(System.currentTimeMillis().toString())
                    .putFile(it)
                    .addOnSuccessListener {task ->
                        task.metadata?.reference?.downloadUrl
                            ?.addOnSuccessListener { downloadUri ->
                                profileUri = downloadUri

                                dbRef.child("imageUrl").setValue(downloadUri.toString())
                            }
                            ?.addOnFailureListener { exception ->
                                Toast.makeText(this, exception.toString(), Toast.LENGTH_SHORT).show()
                            }
                    }
            }


            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userName = snapshot.child("name").getValue(String::class.java)

                        if (userName != null && userName != updateNameS) {
                            dbRef.child("name").setValue(updateNameS)
                            nameFlag = true
                        }}
                        if (profileUri !=  null)
                            profileFlag = true


                    if (nameFlag || profileFlag) {
                        saveBtn.text = "..."
                        val delayMillis: Long = 3000
                        val handler = Handler()

                        handler.postDelayed({
                            Toast.makeText(this@UpdateProfile, "Profile Updated", Toast.LENGTH_LONG).show()
                            saveBtn.postDelayed({
                                saveBtn.text = "SAVE"
                            }, 100)
                        }, delayMillis)
                    }


                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@UpdateProfile, error.toString(), Toast.LENGTH_SHORT).show()
                }
            })

        }

    }

}

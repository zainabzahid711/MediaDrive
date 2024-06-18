package com.example.filedrive

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.google.firebase.ktx.Firebase

class Signup : AppCompatActivity() {

    private lateinit var dbRef: DatabaseReference
    private var dbStorage = Firebase.storage
    private var uri: Uri? = null
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        supportActionBar?.hide()

        dbStorage = FirebaseStorage.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()

        val signUpBtn = findViewById<Button>(R.id.signup_btn)
        val btnGallery = findViewById<ImageView>(R.id.imagePikerBtn)
        val checkedBox = findViewById<CheckBox>(R.id.imageChecked)

        val galleryImage = registerForActivityResult(
            ActivityResultContracts.GetContent(),
            ActivityResultCallback { url ->
                url?.let {
                    uri = url
                    btnGallery.visibility = View.GONE
                    checkedBox.visibility = View.VISIBLE
                }
            }
        )

        btnGallery.setOnClickListener {
            galleryImage.launch("image/*")
        }

        signUpBtn.setOnClickListener {
            postSignupDataToFirebase()
        }
    }



    private fun postSignupDataToFirebase() {
        val name = findViewById<EditText>(R.id.name_id)
        val email = findViewById<EditText>(R.id.email_id)
        val password = findViewById<EditText>(R.id.password_id)

        val name_ = name.text.toString()
        val email_ = email.text.toString()
        val password_ = password.text.toString()

        if (name_.isEmpty()) {
            name.error = "enter your name"
            name.requestFocus()
            return
        }
        if (email_.isEmpty()) {
            email.error = "enter valid email address"
            email.requestFocus()
            return
        }
        if (password_.isEmpty()) {
            password.error = "enter valid password"
            password.requestFocus()
            return
        }

        val auth: FirebaseAuth = FirebaseAuth.getInstance()

        val progress = findViewById<LinearLayout>(R.id.progress_id)
        val form = findViewById<LinearLayout>(R.id.form_container)

        form.visibility = View.GONE
        progress.visibility = View.VISIBLE

        auth.createUserWithEmailAndPassword(email_, password_).addOnCompleteListener {

            form.visibility = View.VISIBLE
            progress.visibility = View.GONE

            if (it.isSuccessful) {
                uri?.let { // Check if uri is not null
                    form.visibility = View.GONE
                    progress.visibility = View.VISIBLE

                    dbStorage.getReference("Profile Images").child(System.currentTimeMillis().toString())
                        .putFile(it)
                        .addOnSuccessListener { task ->
                            task.metadata?.reference?.downloadUrl
                                ?.addOnSuccessListener { downloadUri ->
                                    uri = downloadUri
                                }
                                ?.addOnFailureListener { exception ->
                                    Toast.makeText(this, exception.toString(), Toast.LENGTH_SHORT).show()
                                }
                        }
                }

                val userId = FirebaseAuth.getInstance().currentUser?.uid
                dbRef = FirebaseDatabase.getInstance().getReference("Users").child(userId.toString())
                val signupDetails = SignupClass(name_, email_, uri.toString())
                dbRef.setValue(signupDetails)

                Toast.makeText(this, "Signup Successful", Toast.LENGTH_SHORT).show()
                name.text = null
                email.text = null
                password.text = null
                val home = Intent(this, MainActivity::class.java)
                startActivity(home)
            } else
                Toast.makeText(this, "Signup Fail: " + it.exception, Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    fun signinIntent(view: View) {
        startActivity(Intent(this, Login::class.java))
    }
}

class SignupClass(name_: String, email_: String, uri: String) {
    var name: String = name_
    var email: String = email_
    var imageUrl: String = uri
}

package com.example.filedrive

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        supportActionBar?.hide()

        var requestBtn = findViewById<Button>(R.id.signin_btn)

        requestBtn.setOnClickListener{
            signin_with_firebase()
        }
    }



    private fun signin_with_firebase(){

//    decleration
        var email =    findViewById<EditText> (R.id.signin_email_id)
        var password = findViewById<EditText> (R.id.signin_password_id)

//    convert to string
        var email_ =    email.text.toString()
        var password_ = password.text.toString()


//    Validation
        if(email_.isEmpty())
        {
            email.error = "enter correct email address"
            email.requestFocus()
            return
        }
        if(password_.isEmpty())
        {
            password.error = "enter correct email address"
            password.requestFocus()
            return
        }


//    Send data to firebase
        val auth: FirebaseAuth = FirebaseAuth.getInstance()

        var progress = findViewById<LinearLayout>  (R.id.signin_progress_id)
        var form =     findViewById<LinearLayout>  (R.id.signin_form_container)

        form.visibility =     View.GONE
        progress.visibility = View.VISIBLE

        auth.signInWithEmailAndPassword(email_,password_).addOnCompleteListener {

            form.visibility= View.VISIBLE
            progress.visibility= View.GONE

            if (it.isSuccessful) {
                var home = Intent(this, MainActivity::class.java)
                startActivity(home)
                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                email.text = null
                password.text = null
            }
            else
                Toast.makeText(this,"Login Fail: "+it.exception, Toast.LENGTH_SHORT).show()
        }
    }

    fun signupForm(view: View){
        startActivity(Intent(this,Signup::class.java))
    }
}
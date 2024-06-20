package com.example.filedrive

import android.os.Bundle
import android.view.Menu
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.filedrive.databinding.ActivityMainBinding
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.ui.NavigationUI
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.filedrive.ui.UrlDataClass
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.storage
import java.io.File
import java.util.Date
import java.util.Locale
import java.io.IOException
import java.text.SimpleDateFormat
import android.Manifest

class MainActivity : AppCompatActivity() {


    //    late declarations
    private var userId = FirebaseAuth.getInstance().currentUser?.uid
    private var uri : Uri?= null
    private lateinit var galleryImage: ActivityResultLauncher<String>
    private  var dbStorage= Firebase.storage
    private lateinit var dbRef: DatabaseReference
    private lateinit var takePicture: ActivityResultLauncher<Uri>


    private val CAMERA_PERMISSION_REQUEST_CODE = 123

    private var dbRefListener: ValueEventListener? = null

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        userId?.let {
            dbRef = FirebaseDatabase.getInstance().getReference("Users").child(userId.toString())
        }

        galleryImage = registerForActivityResult(
            ActivityResultContracts.GetContent(),
            ActivityResultCallback  {url->
                url?.let{
                    uri = url
                    uploadImage(url)
                }
            }
        )

        takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                // Image capture was successful, now you can upload the image
                uri?.let { uploadImage(it) }
            } else {
                // Image capture failed or was canceled
                Toast.makeText(this, "Image capture failed", Toast.LENGTH_SHORT).show()
            }
        }

        binding.appBarMain.fabAdd.setOnClickListener { view ->
            showPopupMenu(view)
        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery,R.id.nav_favImg, R.id.nav_slideshow, R.id.nav_Files, R.id.nav_Faq
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)



        //started code from here
        var auth: FirebaseAuth=FirebaseAuth.getInstance()

        //function call to get username and email to show on header
        displayUserInfo(auth)


        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    auth.signOut()
                    startActivity(Intent(this, Login::class.java))
                    finish()
                    true
                }
                else -> {
                    // Perform fragment navigation for other menu items
                    menuItem.isChecked = true
                    drawerLayout.closeDrawers()
                    val handled = NavigationUI.onNavDestinationSelected(menuItem, navController)
                    handled || super.onOptionsItemSelected(menuItem)
                }
            }
        }
    }


    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.menu_fab, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_upload_image -> {
                    galleryImage.launch("image/*")
                    true
                }
                R.id.action_open_camera -> {
//                    Snackbar.make(view, "Open Camera", Snackbar.LENGTH_LONG).show()
                    openCamera()
                    true
                }
                else -> false
            }
        }

        // Set icons programmatically
        val menu = popupMenu.menu
        menu.findItem(R.id.action_upload_image)?.setIcon(R.drawable.baseline_cloud_upload_24)
        menu.findItem(R.id.action_open_camera)?.setIcon(R.drawable.baseline_cloud_upload_24)


        popupMenu.show()
    }

    fun hideFab() {
        binding.appBarMain.fabAdd.visibility = View.GONE
    }

    fun showFab() {
        binding.appBarMain.fabAdd.visibility = View.VISIBLE
    }

    private fun displayUserInfo(auth: FirebaseAuth) {

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val dbRef = FirebaseDatabase.getInstance().getReference("Users").child(userId.toString())

        dbRefListener = dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val dbName = snapshot.child("name").getValue(String::class.java)
                    val dbEmail = snapshot.child("email").getValue(String::class.java)
                    val dbImage = snapshot.child("imageUrl").getValue(String::class.java)

                    val headerView = binding.navView.getHeaderView(0)
                    val userNameTextView: TextView = headerView.findViewById(R.id.userName)
                    val userEmailTextView: TextView = headerView.findViewById(R.id.userEmail)
                    val displayImage : ImageView = headerView.findViewById(R.id.imageView)

                    displayImage.setOnClickListener{
                        startActivity(Intent(this@MainActivity,UpdateProfile::class.java))
                    }

                    userNameTextView.text = dbName
                    userEmailTextView.text = dbEmail

                    if (dbImage != null) {
                        Glide.with(this@MainActivity.applicationContext)
                            .load(dbImage)
                            .placeholder(R.drawable.man)
                            .transform(CircleCrop())
                            .into(displayImage)
                            .waitForLayout()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle an error in fetching the data
                val errorMessage = "Error: ${error.message}" // Get the error message from DatabaseError

                // Use applicationContext or pass a valid context to Toast.makeText()
                Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
            }

        })

    }

    private fun uploadImage(imageUri: Uri) {
        binding.appBarMain.fabAdd.setImageResource(R.drawable.baseline_access_time_24)

        dbStorage.getReference("Gallery Images").child(userId.toString())
            .child(System.currentTimeMillis().toString())
            .putFile(imageUri)
            .addOnSuccessListener { task ->
                task.metadata?.reference?.downloadUrl?.addOnSuccessListener { url ->
                    uri = url

                    //store image url in realTime database
                    var uniqueId = dbRef.push().key.toString()

                    dbRef.child("galleryImagesUrl").child(uniqueId).setValue(
                        UrlDataClass(uri.toString(),false,
                            favFlag = false
                        )
                    )
                        .addOnSuccessListener {
                            binding.appBarMain.fabAdd.setImageResource(R.drawable.baseline_drive_folder_upload_24)

                            Toast.makeText(this, "upload successfully", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener {
                            Toast.makeText(this, it.message.toString(), Toast.LENGTH_SHORT).show()
                        }

                }
                    ?.addOnFailureListener {
                        Toast.makeText(this, it.message.toString(), Toast.LENGTH_SHORT)
                            .show()
                    }
            }
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermission()
            return
        }

        val photoFile: File? = createImageFile()

        photoFile?.let {
            val photoUri: Uri = FileProvider.getUriForFile(
                this,
                "${packageName}.file-provider",
                it
            )
            takePicture.launch(photoUri)
        }
    }


    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permission already granted
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed with camera operation
                    openCamera()
                } else {
                    // Permission denied
                    Toast.makeText(
                        this,
                        "Camera permission denied. Cannot open camera.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            // ... handle other permission requests if needed
        }
    }


    @Throws(IOException::class)
    private fun createImageFile(): File? {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId){
            R.id.close_app -> {
                finishAffinity()
                true
            }
            R.id.restart_app -> {
                startActivity(Intent(this,Signup::class.java))
                finish()
                true
            }
            R.id.updateProfile -> {
                startActivity(Intent(this,UpdateProfile::class.java))
                true
            }
            R.id.home_item ->{
                startActivity(Intent(this,MainActivity::class.java))
                true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
package com.cheezycode.chatapplication

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.cheezycode.chatapplication.databinding.ActivityMainBinding
import com.cheezycode.chatapplication.model.User
import com.cheezycode.chatapplication.ui.ChatActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var getResult: ActivityResultLauncher<Intent>
    private val STORAGE_REQUEST_CODE = 2343
    private lateinit var uri: Uri
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersRef: CollectionReference = db.collection("Users")
    private lateinit var storageRef: StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        storageRef = FirebaseStorage.getInstance().reference

        binding.signInButton.setOnClickListener {
            signIn()
        }
        binding.signUpBtn.setOnClickListener {
            createAccount()
        }

        binding.tvRegister.setOnClickListener {
            showAnimation()
        }

        binding.tvSignin.setOnClickListener {
            showPreviousAnimation()
        }

        binding.tvSelectImageForProfile.setOnClickListener {
            showAnimation()
        }

        binding.tvGoToSignup.setOnClickListener {
            showPreviousAnimation()
        }

        binding.viewSelectImage.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermission()
            } else {
                getImage()
            }
        }

        getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                binding.viewSelectImage.setImageURI(it.data?.data)
                uri = it.data?.data!!
            }
        }

    }

    private fun requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this@MainActivity,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            AlertDialog.Builder(this@MainActivity)
                .setPositiveButton(R.string.dialog_button_yes) { _, _ ->
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                        STORAGE_REQUEST_CODE
                    )
                }.setNegativeButton(R.string.dialog_button_no) { dialog, _ ->
                    dialog.cancel()
                }.setTitle("Permission needed")
                .setMessage("This permission is needed to accessing the storage")
        } else {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                STORAGE_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_REQUEST_CODE && grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getImage()
        } else {
            Toast.makeText(this@MainActivity, "Permission not granted", Toast.LENGTH_LONG).show()
        }
    }

    private fun getImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        getResult.launch(intent)

    }

    private fun createAccount() {
        val username = binding.tvUsername.editText?.text.toString().trim()
        val email = binding.email2.editText?.text.toString().trim()
        val password = binding.password2.editText?.text.toString().trim()
        val confirmPassword = binding.passwordConfirm.editText?.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "You should provide an email and password", Toast.LENGTH_SHORT)
                .show()
            return
        }
        if (username.isEmpty()) {
            Toast.makeText(this, "You should provide username", Toast.LENGTH_SHORT).show()
            return
        }
        if (password != confirmPassword) {
            Toast.makeText(this, "Password don't match", Toast.LENGTH_SHORT)
                .show()
            return
        }
        if (password.length <= 6) {
            Toast.makeText(this, "password should be greater than 6", Toast.LENGTH_LONG).show()
            return
        }
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "You account is created", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this,
                        "${task.exception}",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }

        if (this::uri.isInitialized) {
            // add user model data to firebase
            val filePath = storageRef.child("profile_images").child(uri.lastPathSegment!!)
            filePath.putFile(uri).addOnSuccessListener { task ->
                val result: Task<Uri> = task.metadata?.reference?.downloadUrl!!
                result.addOnSuccessListener {
                    uri = it
                }
                val user =
                    User(username, uri.toString(), FirebaseAuth.getInstance().currentUser?.uid!!)
                usersRef.document().set(user).addOnSuccessListener {
                    Toast.makeText(
                        this@MainActivity,
                        "Account createddddddddd",
                        Toast.LENGTH_LONG
                    ).show()
                    sendToAct()
                }.addOnFailureListener {
                    Toast.makeText(
                        this@MainActivity,
                        it.message,
                        Toast.LENGTH_LONG
                    ).show()
                }

            }


        } else {
            // else not add
        }
    }

    private fun signIn() {
        val email = binding.email.editText?.text.toString().trim()
        val password = binding.password.editText?.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "You should provide an email and password", Toast.LENGTH_SHORT)
                .show()
            return
        }

        FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "You are signed in", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this,
                        "${task.exception}",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }
    }

    private fun showAnimation() {
        binding.viewFlipper.setInAnimation(this, android.R.anim.slide_in_left)
        binding.viewFlipper.setOutAnimation(this, android.R.anim.slide_out_right)
        binding.viewFlipper.showNext()
    }

    private fun showPreviousAnimation() {
        binding.viewFlipper.setInAnimation(this, android.R.anim.slide_in_left)
        binding.viewFlipper.setOutAnimation(this, android.R.anim.slide_out_right)
        binding.viewFlipper.showPrevious()
    }

    private fun sendToAct(){
        startActivity(Intent(this@MainActivity, ChatActivity::class.java))
    }
}
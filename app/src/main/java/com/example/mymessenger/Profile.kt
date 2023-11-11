package com.example.mymessenger

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.bumptech.glide.Glide
import com.example.mymessenger.PhoneNumber
import com.example.mymessenger.R
import com.example.mymessenger.databinding.ActivityProfileBinding
import com.example.mymessenger.models.User
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

class Profile : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private var user: User? = null
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private var selectedImage: Uri? = null
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.progressbar.visibility = View.INVISIBLE

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("darkMode", MODE_PRIVATE)

        // Set the switch to the user's last selected mode
        val isDarkMode = sharedPreferences.getBoolean("isDarkMode", false)
        binding.darkswitch.isChecked = isDarkMode

        // Set the app's theme based on the user's preference
        setAppTheme(isDarkMode)

        binding.darkswitch.setOnCheckedChangeListener { _, b ->
            // Update the user's dark mode preference
            val editor = sharedPreferences.edit()
            editor.putBoolean("isDarkMode", b)
            editor.apply()

            // Apply the selected theme immediately
            setAppTheme(b)

            if (b) {
                setTheme(R.style.AppThemeDark) // Apply dark theme
            } else {
                setTheme(R.style.AppThemeLight) // Apply light theme
            }

            recreate()

            // Provide feedback to the user
            val modeMessage = if (b) "Dark mode enabled" else "Dark mode disabled"
            Toast.makeText(this, modeMessage, Toast.LENGTH_SHORT).show()
        }

        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance()
        val userReference = database.getReference("users").child(auth.uid!!)

        val storage = FirebaseStorage.getInstance()
        val storageReference: StorageReference = storage.getReference().child("profiles").child(auth.uid!!)

        userReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    user = dataSnapshot.getValue(User::class.java)

                    // Set the username
                    binding.etuname.setText(user?.name)

                    // Load the profile image using Glide
                    Glide.with(this@Profile)
                        .load(user?.profileImage)
                        .placeholder(R.drawable.avatar)
                        .into(binding.upprofile)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle the error if necessary
            }
        })

        binding.updatebtn.setOnClickListener {
            binding.progressbar.visibility = View.VISIBLE
            updateNameInFirebase()
        }

        binding.upprofile.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            galleryLauncher.launch(intent)
        }

        binding.deleteprofile.setOnClickListener {
            val builder = AlertDialog.Builder(this@Profile)
            builder.setTitle("Delete Account")
            builder.setMessage("Are you sure you want to delete your account?")
            builder.setPositiveButton("Yes") { _, _ ->
                // Delete the user's account and data
                deleteAccountAndData()
            }
            builder.setNegativeButton("No", null)
            builder.show()
        }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val uri: Uri? = result.data?.data
                val storage1 = FirebaseStorage.getInstance()
                val time = Date().time
                val reference: StorageReference = storage1.getReference().child("profiles").child(time.toString())
                reference.putFile(uri!!)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            reference.downloadUrl.addOnSuccessListener(OnSuccessListener { uri ->
                                val userReference = FirebaseDatabase.getInstance()
                                    .getReference("users")
                                    .child(FirebaseAuth.getInstance().uid!!)
                                val updateData = hashMapOf<String, Any>()
                                updateData["profileImage"] = uri.toString()
                                userReference.updateChildren(updateData)
                                    .addOnCompleteListener(OnCompleteListener { task ->
                                        binding.progressbar.visibility = View.INVISIBLE
                                    })
                                user?.profileImage = uri.toString()
                                Glide.with(this@Profile).load(user?.profileImage).into(binding.upprofile)
                            })
                        }
                    }
            }
        }
    }

    private fun updateNameInFirebase() {
        val newName = binding.etuname.text.toString()
        if (!TextUtils.isEmpty(newName)) {
            val userReference = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(FirebaseAuth.getInstance().uid!!)
            val updateData = hashMapOf<String, Any>()
            updateData["name"] = newName
            userReference.updateChildren(updateData)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        binding.progressbar.visibility = View.INVISIBLE
                    }
                }
            user?.name = newName
        }
    }

    private fun deleteAccountAndData() {
        val userReference = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(FirebaseAuth.getInstance().uid!!)
        userReference.removeValue()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val storage = FirebaseStorage.getInstance()
                    val storageReference: StorageReference =
                        storage.getReference().child("profiles").child(FirebaseAuth.getInstance().uid!!)
                    storageReference.delete()
                        .addOnCompleteListener { storageTask ->
                            if (storageTask.isSuccessful) {
                                val user = FirebaseAuth.getInstance().currentUser
                                user?.delete()
                                    ?.addOnCompleteListener { authTask ->
                                        if (authTask.isSuccessful) {
                                            Toast.makeText(
                                                this@Profile,
                                                "Account deleted successfully",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            FirebaseAuth.getInstance().signOut()
                                            val intent = Intent(this@Profile, PhoneNumber::class.java)
                                            intent.flags =
                                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            Toast.makeText(
                                                this@Profile,
                                                "Account deletion failed. Please try again.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            Log.e("AccountDeletion", "Account deletion failed", authTask.exception)
                                        }
                                    }
                            } else {
                                Toast.makeText(this@Profile, "Failed to delete files from Firebase Storage.", Toast.LENGTH_SHORT).show()
                                Log.e("AccountDeletion", "Failed to delete files from Firebase Storage.", storageTask.exception)
                            }
                        }
                } else {
                    Toast.makeText(this@Profile, "Failed to delete data from the Realtime Database.", Toast.LENGTH_SHORT).show()
                    Log.e("AccountDeletion", "Failed to delete data from the Realtime Database.", task.exception)
                }
            }
    }

    private fun setAppTheme(isDarkMode: Boolean) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}

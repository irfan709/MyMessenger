package com.example.mymessenger

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.mymessenger.models.User
import com.example.mymessenger.databinding.ActivitySetupProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class SetupProfile : AppCompatActivity() {
    private lateinit var binding: ActivitySetupProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private var selectedImage: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        binding.setimg.setOnClickListener {
            val intent = Intent().apply {
                action = Intent.ACTION_GET_CONTENT
                type = "image/*"
            }
            galleryLauncher.launch(intent)
        }

        galleryLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                    val uri = result.data?.data
                    if (uri != null) {
                        selectedImage = uri
                        binding.setimg.setImageURI(uri)
                        selectedImage = result.data?.data
                    }
                }
            }

        binding.save.setOnClickListener {
            val name: String = binding.profname.text.toString()
            if (name.isEmpty()) {
                binding.profname.error = "Field cannot be empty"
            }

            binding.progress.visibility = View.VISIBLE
            if (selectedImage != null) {
                val reference = storage.getReference("profiles")
                    .child(auth.uid ?: "")
                reference.putFile(selectedImage!!).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        reference.downloadUrl.addOnSuccessListener { url ->
                            val uid = auth.uid ?: ""
                            val number = auth.currentUser?.phoneNumber ?: ""
                            val image = url.toString()

                            val user = User(uid, name, number, image, "")

                            database.reference.child("users")
                                .child(uid)
                                .setValue(user)
                                .addOnSuccessListener {
                                    binding.progress.visibility = View.INVISIBLE

                                    val intent = Intent(this@SetupProfile, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                        }
                    }
                }
            } else {
                val uid = auth.uid ?: ""
                val number = auth.currentUser?.phoneNumber ?: ""

                val user = User(uid, name, number, "No Image", "")

                database.reference.child("users")
                    .child(uid)
                    .setValue(user)
                    .addOnSuccessListener {
                        binding.progress.visibility = View.INVISIBLE

                        val intent = Intent(this@SetupProfile, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
            }
        }
    }
}
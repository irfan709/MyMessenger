package com.example.mymessenger

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import com.example.mymessenger.R
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mymessenger.adapters.UserAdapter
import com.example.mymessenger.databinding.ActivityMainBinding
import com.example.mymessenger.models.User
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val users: ArrayList<User> = ArrayList()
    private lateinit var user: User
    private lateinit var userAdapter: UserAdapter
    private lateinit var database: FirebaseDatabase
    private val PERMISSION_REQUEST_READ_CONTACTS = 1
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("darkMode", MODE_PRIVATE)

        // Set the app's theme based on the user's preference
        val isDarkMode = sharedPreferences.getBoolean("isDarkMode", false)
        AppCompatDelegate.setDefaultNightMode(if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)

        // Check and request permission to read contacts if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                PERMISSION_REQUEST_READ_CONTACTS
            )
        } else {
            // Permission is granted, you can retrieve phone numbers
            retrievePhoneNumbers()
        }

        database = FirebaseDatabase.getInstance()

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val updateData = HashMap<String, Any>()
            updateData["token"] = token
            database.reference.child("users").child(FirebaseAuth.getInstance().uid.toString())
                .updateChildren(updateData)
        }

        userAdapter = UserAdapter(this, users)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        binding.userslist.layoutManager = layoutManager
        binding.userslist.adapter = userAdapter

        val phoneUsers = retrievePhoneNumbers()

        database.reference.child("users").child(FirebaseAuth.getInstance().uid.toString())
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    user = snapshot.getValue(User::class.java) ?: User()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle the error if necessary
                }
            })

        database.reference.child("users")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    users.clear()
                    for (snapshot1 in snapshot.children) {
                        val user = snapshot1.getValue(User::class.java)
                        if (user != null && user.id != FirebaseAuth.getInstance().uid) {
                            for (phoneUser in phoneUsers) {
                                if (phoneUser.number == user.number) {
                                    user.name = phoneUser.name
                                    users.add(user)
                                }
                            }
                        }
                    }
                    userAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle the error if necessary
                }
            })

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.profile_menu -> {
                    startActivity(Intent(this, Profile::class.java))
                }

                else -> {

                }
            }
            true
        }
    }

    private fun retrievePhoneNumbers(): ArrayList<User> {
        val users = ArrayList<User>()

        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            null
        )

        if (cursor != null) {
            val nameColumnIndex =
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneNumberColumnIndex =
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameColumnIndex)
                val phoneNumber = cursor.getString(phoneNumberColumnIndex)

                val user = User("", name, phoneNumber, "default_profile_image", "")
                users.add(user)
            }
            cursor.close()
        }

        return users
    }

    override fun onResume() {
        super.onResume()
        val currentId = FirebaseAuth.getInstance().uid
        database.reference.child("presence").child(currentId.toString()).setValue("Online")
    }

    override fun onPause() {
        super.onPause()
        val currentId = FirebaseAuth.getInstance().uid
        database.reference.child("presence").child(currentId.toString()).setValue("Offline")
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val id = binding.bottomNavigationView.id
        if (binding.bottomNavigationView.id != R.id.chat_menu) {
            binding.bottomNavigationView.id = R.id.chat_menu
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_READ_CONTACTS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can now retrieve phone numbers
                retrievePhoneNumbers()
            } else {
                Toast.makeText(this, "Permissions denied...", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

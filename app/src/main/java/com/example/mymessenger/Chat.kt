package com.example.mymessenger

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.example.mymessenger.adapters.MessageAdapter
import com.example.mymessenger.databinding.ActivityChatBinding
import com.example.mymessenger.models.Message
import com.example.mymessenger.models.User
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.Map

class Chat : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: MessageAdapter
    private val messages: ArrayList<Message> = ArrayList()
    private lateinit var senderRoom: String
    private lateinit var receiverRoom: String
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private lateinit var senderUid: String
    private lateinit var receiverUid: String
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        val name = intent.getStringExtra("name")
        val profile = intent.getStringExtra("image")
        val token = intent.getStringExtra("token")
        binding.name.text = name
        Glide.with(this).load(profile)
            .placeholder(R.drawable.avatar)
            .into(binding.profile)

        binding.imageView2.setOnClickListener {
            finish()
        }
        receiverUid = intent.getStringExtra("uid").toString()
        senderUid = FirebaseAuth.getInstance().uid!!
        database.getReference().child("presence").child(receiverUid).addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val status = snapshot.getValue(String::class.java)
                    if (!status.isNullOrEmpty()) {
                        if (status == "Offline") {
                            binding.status.visibility = View.GONE
                        } else {
                            binding.status.text = status
                            binding.status.visibility = View.VISIBLE
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        senderRoom = senderUid + receiverUid
        receiverRoom = receiverUid + senderUid
        val user = name?.let {
            FirebaseAuth.getInstance().currentUser?.phoneNumber?.let { it1 ->
                profile?.let { it2 ->
                    User(
                        senderUid,
                        it, it1, it2, ""
                    )
                }
            }
        }
        adapter = user?.let { MessageAdapter(this, messages, senderRoom, receiverRoom, it) }!!
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        database.getReference().child("chats")
            .child(senderRoom)
            .child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messages.clear()
                    for (snapshot1 in snapshot.children) {
                        val messageId = snapshot1.key
                        val message = snapshot1.getValue(Message::class.java)
                        if (messageId != null) {
                            message?.messageId = messageId
                        }
                        message?.let { messages.add(it) }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        binding.sendBtn.setOnClickListener {
            val messageTxt = binding.messageBox.text.toString()
            if (!messageTxt.isEmpty()) {
                val date = Date()
                val message = Message("", messageTxt, senderUid, "", date.time, -1)
                binding.messageBox.setText("")
                val randomKey = database.reference.push().key
                val lastMsgObj = HashMap<String, Any>()
                lastMsgObj["lastMsg"] = message.message
                lastMsgObj["lastMsgTime"] = date.time
                database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj)
                database.getReference().child("chats").child(receiverRoom)
                    .updateChildren(lastMsgObj)
                database.getReference().child("chats")
                    .child(senderRoom)
                    .child("messages")
                    .child(randomKey!!)
                    .setValue(message).addOnSuccessListener {
                        database.getReference().child("chats")
                            .child(receiverRoom)
                            .child("messages")
                            .child(randomKey)
                            .setValue(message).addOnSuccessListener {
                                sendNotification(name, message.message, token)
                            }
                    }
            }
        }

        val handler = Handler(Looper.getMainLooper())
        binding.messageBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                database.getReference().child("presence").child(senderUid).setValue("typing...")
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed(userStoppedTyping, 1000)
            }

            private val userStoppedTyping = Runnable {
                database.getReference().child("presence").child(senderUid).setValue("Online")
            }
        })
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.attachment.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            galleryLauncher.launch(intent)
        }
        galleryLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                    val selectedImage = result.data?.data
                    val calendar = Calendar.getInstance()
                    val reference =
                        storage.reference.child("chats").child(calendar.timeInMillis.toString())
                    binding.progressbar.visibility = View.VISIBLE
                    reference.putFile(selectedImage!!)
                        .addOnCompleteListener(OnCompleteListener<UploadTask.TaskSnapshot> { task ->
                            if (task.isSuccessful) {
                                reference.downloadUrl.addOnSuccessListener(OnSuccessListener<Uri> { uri ->
                                    val filePath = uri.toString()
                                    val messageTxt = binding.messageBox.text.toString()
                                    val date = Date()
                                    val message = Message("", messageTxt, senderUid, "", date.time)
                                    message.message = "photo"
                                    message.msgImg = filePath
                                    binding.messageBox.setText("")
                                    val randomKey = database.reference.push().key
                                    val lastMsgObj = HashMap<String, Any>()
                                    lastMsgObj["lastMsg"] = message.message
                                    lastMsgObj["lastMsgTime"] = date.time
                                    database.getReference().child("chats").child(senderRoom)
                                        .updateChildren(lastMsgObj)
                                    database.getReference().child("chats").child(receiverRoom)
                                        .updateChildren(lastMsgObj)
                                    database.getReference().child("chats")
                                        .child(senderRoom)
                                        .child("messages")
                                        .child(randomKey!!)
                                        .setValue(message).addOnSuccessListener {
                                            database.getReference().child("chats")
                                                .child(receiverRoom)
                                                .child("messages")
                                                .child(randomKey)
                                                .setValue(message).addOnSuccessListener { }
                                        }
                                })
                            }
                        })
                }
            }
    }

    private fun sendNotification(name: String?, message: String?, token: String?) {
        try {
            val queue = Volley.newRequestQueue(this)
            val url = "https://fcm.googleapis.com/fcm/send"
            val data = JSONObject()
            data.put("title", name)
            data.put("body", message)
            val notificationData = JSONObject()
            notificationData.put("notification", data)
            notificationData.put("to", token)
            val request = object : JsonObjectRequest(url, notificationData,
                Response.Listener { response ->
                    // Toast.makeText(ChatActivity.this, "success", Toast.LENGTH_SHORT).show();
                },
                Response.ErrorListener { error ->
                    Toast.makeText(this, error.localizedMessage, Toast.LENGTH_SHORT).show()
                }) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val map: HashMap<String, String> = HashMap()
                    val key =
                        "Key=AAAAVE893CE:APA91bEwBrK2n5Q_bqdk8dQ2IzW5QuCpc0nf0mkJL7eCjG64sB1UT6jk-4BObTcD0qABD-qvvDdpaErChAwxknX8lgZwtYR6IqeUgSm6Qb1o8vMhvOx5kckJzyY0FF9dvlad9LZKfbmK"
                    map["Content-Type"] = "application/json"
                    map["Authorization"] = key
                    return map
                }
            }
            queue.add(request)
        } catch (ex: Exception) {
        }
    }

    override fun onResume() {
        super.onResume()
        val currentId = FirebaseAuth.getInstance().uid
        database.getReference().child("presence").child(currentId!!).setValue("Online")
    }

    override fun onPause() {
        super.onPause()
        val currentId = FirebaseAuth.getInstance().uid
        database.getReference().child("presence").child(currentId!!).setValue("Offline")
    }
}
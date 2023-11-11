package com.example.mymessenger.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mymessenger.Chat
import com.example.mymessenger.R
import com.example.mymessenger.databinding.ChatConversationBinding
import com.example.mymessenger.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale

class UserAdapter(private val context: Context, private val users: ArrayList<User>) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: ChatConversationBinding = ChatConversationBinding.bind(itemView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserAdapter.UserViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.chat_conversation, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserAdapter.UserViewHolder, position: Int) {
        val user = users[position]
        val senderId = FirebaseAuth.getInstance().uid
        val senderRoom = senderId + user.id

        FirebaseDatabase.getInstance().reference.child("chats")
            .child(senderRoom)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val lastMsg = snapshot.child("lastMsg").getValue(String::class.java)
                        val lastMsgTime = snapshot.child("lastMsgTime").getValue(Long::class.java)
                        val date = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        holder.binding.lastMsg.text = lastMsg
                        holder.binding.msgTime.text = date.format(Date(lastMsgTime ?: 0L))
                    } else {
                        holder.binding.lastMsg.text = "Tap to chat"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })

        holder.binding.username.text = user.name

        Glide.with(context).load(user.profileImage).placeholder(R.drawable.avatar).into(holder.binding.profile)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, Chat::class.java)
            intent.putExtra("uid", user.id)
            intent.putExtra("name", user.name)
            intent.putExtra("image", user.profileImage)
            intent.putExtra("token", user.token)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return users.size
    }
}
package com.example.mymessenger.adapters

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mymessenger.R

import com.example.mymessenger.databinding.DeleteDialogBinding
import com.example.mymessenger.databinding.ItemReceiveBinding
import com.example.mymessenger.databinding.ItemSendBinding
import com.example.mymessenger.models.Message
import com.example.mymessenger.models.User
import com.github.pgreze.reactions.ReactionPopup
import com.github.pgreze.reactions.ReactionsConfigBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class MessageAdapter(
    private val context: Context,
    private val messages: ArrayList<Message>,
    private val senderRoom: String,
    private val receiveRoom: String,
    private val senderUser: User
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val ITEM_SEND = 1
    private val ITEM_RECEIVE = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_SEND) {
            val view = LayoutInflater.from(context).inflate(R.layout.item_send, parent, false)
            SentViewHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.item_receive, parent, false)
            ReceiverViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        val reactions = intArrayOf(
            R.drawable.like,
            R.drawable.love,
            R.drawable.laugh,
            R.drawable.wow,
            R.drawable.sad,
            R.drawable.angry
        )

        val config = ReactionsConfigBuilder(context)
            .withReactions(reactions)
            .build()

        val reactionPosition: Int = -1

        val popup = ReactionPopup(context, config) {reactionPosition
            if (reactionPosition >= 0 && reactionPosition < reactions.size) {
                if (holder is SentViewHolder) {
                    val sentViewHolder = holder
                    sentViewHolder.binding.feeling.setImageResource(reactions[reactionPosition])
                    sentViewHolder.binding.feeling.visibility = View.VISIBLE
                } else if (holder is ReceiverViewHolder) {
                    val receiverViewHolder = holder
                    receiverViewHolder.binding.feeling.setImageResource(reactions[reactionPosition])
                    receiverViewHolder.binding.feeling.visibility = View.VISIBLE
                }

                message.feelings = reactionPosition

                // Update the message in Firebase Realtime Database
                FirebaseDatabase.getInstance().getReference()
                    .child("chats")
                    .child(senderRoom)
                    .child("messages")
                    .child(message.messageId.toString())
                    .setValue(message)

                FirebaseDatabase.getInstance().getReference()
                    .child("chats")
                    .child(receiveRoom)
                    .child("messages")
                    .child(message.messageId.toString())
                    .setValue(message)

                notifyItemChanged(holder.adapterPosition)
            }
            return@ReactionPopup
        }


        if (holder is SentViewHolder) {
            val sentViewHolder = holder
            if (message.message == "photo") {
                sentViewHolder.binding.image.visibility = View.VISIBLE
                sentViewHolder.binding.message.visibility = View.GONE
                Glide.with(context)
                    .load(message.msgImg)
                    .placeholder(R.drawable.avatar)
                    .into(sentViewHolder.binding.image)
            }

            sentViewHolder.binding.message.text = message.message

            if (message.feelings >= 0) {
                sentViewHolder.binding.feeling.setImageResource(reactions[message.feelings])
                sentViewHolder.binding.feeling.visibility = View.VISIBLE
            } else {
                sentViewHolder.binding.feeling.visibility = View.INVISIBLE
            }

            sentViewHolder.binding.message.setOnTouchListener { view, motionEvent ->
                popup.onTouch(view, motionEvent)
                false
            }

            sentViewHolder.binding.image.setOnTouchListener { view, motionEvent ->
                popup.onTouch(view, motionEvent)
                false
            }

            sentViewHolder.itemView.setOnLongClickListener {
                Log.d("LongClick", "Long click listener triggered")
                val view1 = LayoutInflater.from(context).inflate(R.layout.delete_dialog, null)
                val binding = DeleteDialogBinding.bind(view1)
                val dialog = AlertDialog.Builder(context)
                    .setTitle("Delete Message")
                    .setView(binding.root)
                    .create()

                binding.everyone.setOnClickListener {
                    message.message = "This message was deleted"
                    message.feelings = -1
                    FirebaseDatabase.getInstance().getReference()
                        .child("chats")
                        .child(senderRoom)
                        .child("messages")
                        .child(message.messageId!!)
                        .setValue(message)

                    FirebaseDatabase.getInstance().getReference()
                        .child("chats")
                        .child(receiveRoom)
                        .child("messages")
                        .child(message.messageId!!)
                        .setValue(message)

                    dialog.dismiss()
                }

                binding.delete.setOnClickListener {
                    FirebaseDatabase.getInstance().getReference()
                        .child("chats")
                        .child(senderRoom)
                        .child("messages")
                        .child(message.messageId!!)
                        .setValue(null)

                    dialog.dismiss()
                }

                binding.cancel.setOnClickListener {
                    dialog.dismiss()
                }

                dialog.show()
                false
            }
        } else if (holder is ReceiverViewHolder) {
            val receiverViewHolder = holder
            Glide.with(context)
                .load(senderUser.profileImage)
                .placeholder(R.drawable.avatar)
                .into(receiverViewHolder.binding.recimg)

            if (message.message == "photo") {
                receiverViewHolder.binding.image.visibility = View.VISIBLE
                receiverViewHolder.binding.message.visibility = View.GONE
                Glide.with(context)
                    .load(message.msgImg)
                    .placeholder(R.drawable.avatar)
                    .into(receiverViewHolder.binding.image)
            }

            receiverViewHolder.binding.message.text = message.message

            if (message.feelings >= 0) {
                receiverViewHolder.binding.feeling.setImageResource(reactions[message.feelings])
                receiverViewHolder.binding.feeling.visibility = View.VISIBLE
            } else {
                receiverViewHolder.binding.feeling.visibility = View.INVISIBLE
            }

            receiverViewHolder.binding.message.setOnTouchListener { view, motionEvent ->
                popup.onTouch(view, motionEvent)
                false
            }

            receiverViewHolder.binding.image.setOnTouchListener { view, motionEvent ->
                popup.onTouch(view, motionEvent)
                false
            }

            receiverViewHolder.itemView.setOnLongClickListener {
                Log.d("LongClick", "Long click listener triggered")
                val view1 = LayoutInflater.from(context).inflate(R.layout.delete_dialog, null)
                val binding = DeleteDialogBinding.bind(view1)
                val dialog = AlertDialog.Builder(context)
                    .setTitle("Delete Message")
                    .setView(binding.root)
                    .create()

                binding.everyone.setOnClickListener {
                    message.message = "This message was deleted"
                    message.feelings = -1
                    FirebaseDatabase.getInstance().getReference()
                        .child("chats")
                        .child(senderRoom)
                        .child("messages")
                        .child(message.messageId!!)
                        .setValue(message)

                    FirebaseDatabase.getInstance().getReference()
                        .child("chats")
                        .child(receiveRoom)
                        .child("messages")
                        .child(message.messageId!!)
                        .setValue(message)

                    dialog.dismiss()
                }

                binding.delete.setOnClickListener {
                    FirebaseDatabase.getInstance().getReference()
                        .child("chats")
                        .child(senderRoom)
                        .child("messages")
                        .child(message.messageId!!)
                        .setValue(null)

                    dialog.dismiss()
                }

                binding.cancel.setOnClickListener {
                    dialog.dismiss()
                }

                dialog.show()
                false
            }
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (FirebaseAuth.getInstance().uid == message.senderId) ITEM_SEND else ITEM_RECEIVE
    }

    inner class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: ItemSendBinding = ItemSendBinding.bind(itemView)
    }

    inner class ReceiverViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: ItemReceiveBinding = ItemReceiveBinding.bind(itemView)
    }
}
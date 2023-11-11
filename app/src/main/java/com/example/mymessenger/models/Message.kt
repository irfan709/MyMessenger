package com.example.mymessenger.models

data class Message(
    var messageId: String? = "",
    var message: String = "",
    var senderId: String = "",
    var msgImg: String? = "",
    var timestamp: Long = 0,
    var messageType: Int = 0,
    var feelings: Int = -1
)

package com.example.model

import okhttp3.HttpUrl

data class Message(
    val id: String="",
    val gonderenId: String="",
    val mesaj: String?="",
    val createdAt: Long = System.currentTimeMillis(),
    val gonderenAdi: String = "",
    val gonderenResim: String? = null,
    val imageUrl: String? = null
)

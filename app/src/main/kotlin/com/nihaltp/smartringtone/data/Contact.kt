package com.nihaltp.smartringtone.data

data class Contact(
    val id: String,
    val name: String,
    val phone: String,
    val photoUri: String?,
    val currentRingtone: String?,
    val score: Int,
    val mappedRingtoneName: String?,
)

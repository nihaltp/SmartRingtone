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

data class Ringtone(
    val id: Int,
    val name: String,
    val uri: String,
)

data class CallLogEntry(
    val number: String,
    val name: String,
    val direction: String,
    val type: String,
    val timestamp: Long,
    val scoreChange: String,
)

enum class ContactSortOrder {
    SCORE,
    NAME,
}

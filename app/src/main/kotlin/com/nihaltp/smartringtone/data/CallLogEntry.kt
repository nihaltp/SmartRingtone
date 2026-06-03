package com.nihaltp.smartringtone.data

data class CallLogEntry(
    val number: String,
    val name: String,
    val direction: String,
    val type: String,
    val timestamp: Long,
    val scoreChange: String,
)

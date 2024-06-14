package com.example.bankingsystem

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfers")
data class Transfer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fromCustomerId: Int,
    val toCustomerId: Int,
    val amount: Double,
    val timestamp: Long
)

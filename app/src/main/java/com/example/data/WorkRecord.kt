package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_records")
data class WorkRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val date: String, // YYYY-MM-DD
    val clientName: String,
    val workType: String, // "HOURLY", "DAILY", "FIXED", "CONTRACT"
    val quantity: Double, // number of hours or days or units
    val rate: Double, // rate per hour/day/unit
    val totalAmount: Double, // calculated as quantity * rate (or rate if FIXED)
    val paymentStatus: String, // "PAID", "PENDING", "PARTIAL"
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val paidAmount: Double = 0.0,
    val category: String = "Other", // "Farming", "Construction", "Plumbing", "Delivery", "Other"
    val paymentHistoryJson: String = "[]" // JSON representation of payment history entries
)

package com.example.data

data class UpiAccount(
    val id: String,
    val label: String, // e.g., "Personal GPay", "Business Paytm"
    val upiId: String,
    val payeeName: String,
    val isDefault: Boolean = false
)

data class PaymentInstallment(
    val id: String,
    val date: String, // YYYY-MM-DD
    val amount: Double,
    val note: String = "",
    val upiAccountLabel: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class SuggestedTask(
    val title: String,
    val category: String,
    val estimatedDurationHours: Double,
    val estimatedEarnings: Double,
    val description: String
)

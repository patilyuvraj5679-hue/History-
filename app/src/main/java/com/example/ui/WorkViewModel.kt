package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.GeminiRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WorkViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: WorkRepository
    private val sharedPrefs = application.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    // UPI state
    var upiId by mutableStateOf("")
        private set
    var payeeName by mutableStateOf("")
        private set
    var isUpiEnabled by mutableStateOf(false)
        private set

    // Multiple UPI Accounts list
    var upiAccounts by mutableStateOf<List<UpiAccount>>(emptyList())
        private set

    // Security PIN & Biometrics
    var isPinLockEnabled by mutableStateOf(false)
        private set
    var isBiometricEnabled by mutableStateOf(false)
        private set
    var isAppLocked by mutableStateOf(false)
    var securityPin by mutableStateOf("")
        private set

    // AI Chat & Insights
    var chatMessages by mutableStateOf<List<ChatMessage>>(emptyList())
        private set
    var isChatLoading by mutableStateOf(false)
        private set
    var isInsightsLoading by mutableStateOf(false)
        private set
    var aiInsightsReport by mutableStateOf("")
        private set
    var voiceParsingLoading by mutableStateOf(false)
        private set

    init {
        val database = AppDatabase.getDatabase(application)
        repository = WorkRepository(database.workDao())

        // Load UPI settings
        upiId = sharedPrefs.getString("upi_id", "") ?: ""
        payeeName = sharedPrefs.getString("payee_name", "") ?: ""
        isUpiEnabled = sharedPrefs.getBoolean("upi_enabled", false)

        // Load other states
        loadUpiAccounts()
        loadSecuritySettings()
    }

    fun loadUpiAccounts() {
        val json = sharedPrefs.getString("upi_accounts", "[]") ?: "[]"
        try {
            val type = Types.newParameterizedType(List::class.java, UpiAccount::class.java)
            val adapter = moshi.adapter<List<UpiAccount>>(type)
            upiAccounts = adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            upiAccounts = emptyList()
        }
    }

    fun saveUpiAccount(label: String, vpa: String, name: String) {
        val newAccount = UpiAccount(
            id = UUID.randomUUID().toString(),
            label = label,
            upiId = vpa,
            payeeName = name,
            isDefault = upiAccounts.isEmpty()
        )
        val updated = upiAccounts + newAccount
        persistUpiAccounts(updated)
    }

    fun deleteUpiAccount(id: String) {
        val updated = upiAccounts.filter { it.id != id }
        persistUpiAccounts(updated)
    }

    fun setDefaultUpiAccount(id: String) {
        val updated = upiAccounts.map { it.copy(isDefault = it.id == id) }
        persistUpiAccounts(updated)
        val defaultAcc = updated.firstOrNull { it.isDefault }
        if (defaultAcc != null) {
            saveUpiSettings(defaultAcc.upiId, defaultAcc.payeeName, true)
        }
    }

    private fun persistUpiAccounts(list: List<UpiAccount>) {
        upiAccounts = list
        try {
            val type = Types.newParameterizedType(List::class.java, UpiAccount::class.java)
            val json = moshi.adapter<List<UpiAccount>>(type).toJson(list)
            sharedPrefs.edit().putString("upi_accounts", json).apply()
        } catch (e: Exception) {}
    }

    fun loadSecuritySettings() {
        isPinLockEnabled = sharedPrefs.getBoolean("pin_lock_enabled", false)
        isBiometricEnabled = sharedPrefs.getBoolean("biometric_enabled", false)
        securityPin = sharedPrefs.getString("security_pin", "") ?: ""
        if (isPinLockEnabled || isBiometricEnabled) {
            isAppLocked = true
        }
    }

    fun saveSecuritySettings(pinEnabled: Boolean, pin: String, bioEnabled: Boolean) {
        sharedPrefs.edit()
            .putBoolean("pin_lock_enabled", pinEnabled)
            .putString("security_pin", pin)
            .putBoolean("biometric_enabled", bioEnabled)
            .apply()
        isPinLockEnabled = pinEnabled
        securityPin = pin
        isBiometricEnabled = bioEnabled
    }

    fun saveUpiSettings(id: String, name: String, enabled: Boolean) {
        sharedPrefs.edit()
            .putString("upi_id", id.trim())
            .putString("payee_name", name.trim())
            .putBoolean("upi_enabled", enabled)
            .apply()
        upiId = id.trim()
        payeeName = name.trim()
        isUpiEnabled = enabled
    }

    fun getUpiPaymentUri(amount: Double, note: String): String {
        if (!isUpiEnabled || upiId.isBlank()) return ""
        return try {
            val encodedName = java.net.URLEncoder.encode(payeeName, "UTF-8")
            val encodedNote = java.net.URLEncoder.encode(note, "UTF-8")
            // UPI deep link URI
            "upi://pay?pa=$upiId&pn=$encodedName&am=${"%.2f".format(amount)}&cu=INR&tn=$encodedNote"
        } catch (e: Exception) {
            ""
        }
    }

    fun getQrCodeUrl(upiUri: String): String {
        if (upiUri.isBlank()) return ""
        return try {
            val encodedUri = java.net.URLEncoder.encode(upiUri, "UTF-8")
            "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=$encodedUri"
        } catch (e: Exception) {
            ""
        }
    }

    // Language state: true = Marathi, false = English
    var isMarathi by mutableStateOf(true)
        private set

    fun toggleLanguage() {
        isMarathi = !isMarathi
    }

    // Work records from database
    val allRecords: StateFlow<List<WorkRecord>> = repository.allWorkRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uniqueClients: StateFlow<List<String>> = repository.uniqueClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filters
    private val _selectedClientFilter = MutableStateFlow<String?>("All")
    val selectedClientFilter = _selectedClientFilter.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<String?>("All")
    val selectedStatusFilter = _selectedStatusFilter.asStateFlow()

    // Combined filtered records
    val filteredRecords: StateFlow<List<WorkRecord>> = combine(
        allRecords,
        _selectedClientFilter,
        _selectedStatusFilter
    ) { records, client, status ->
        records.filter { record ->
            val matchClient = client == null || client == "All" || client == "सर्व" || record.clientName.equals(client, ignoreCase = true)
            val matchStatus = status == null || status == "All" || status == "सर्व" || record.paymentStatus == status
            matchClient && matchStatus
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setClientFilter(client: String?) {
        _selectedClientFilter.value = client
    }

    fun setStatusFilter(status: String?) {
        _selectedStatusFilter.value = status
    }

    // WorkRecord Actions
    fun saveWorkRecord(
        id: Int = 0,
        title: String,
        date: String,
        clientName: String,
        workType: String,
        quantity: Double,
        rate: Double,
        paymentStatus: String,
        notes: String,
        category: String = "Other",
        paidAmount: Double = 0.0,
        paymentHistoryJson: String = "[]"
    ) {
        val total = if (workType == "FIXED" || workType == "CONTRACT") rate else quantity * rate
        val finalPaid = if (paymentStatus == "PAID") total else paidAmount
        val record = WorkRecord(
            id = id,
            title = title,
            date = date,
            clientName = clientName,
            workType = workType,
            quantity = quantity,
            rate = rate,
            totalAmount = total,
            paymentStatus = paymentStatus,
            notes = notes,
            category = category,
            paidAmount = finalPaid,
            paymentHistoryJson = paymentHistoryJson
        )
        viewModelScope.launch {
            repository.insert(record)
        }
    }

    fun deleteWorkRecord(record: WorkRecord) {
        viewModelScope.launch {
            repository.delete(record)
        }
    }

    // AI Generation state
    var aiGenerationLoading by mutableStateOf(false)
        private set

    var aiGeneratedReport by mutableStateOf("")
        private set

    fun clearAiReport() {
        aiGeneratedReport = ""
    }

    fun generateAiWorkReport(selectedClient: String?) {
        viewModelScope.launch {
            aiGenerationLoading = true
            aiGeneratedReport = ""

            val currentRecords = filteredRecords.value
            if (currentRecords.isEmpty()) {
                aiGeneratedReport = if (isMarathi) {
                    "कोणताही कामाचा हिशोब उपलब्ध नाही. कृपया आधी काही नोंदी जोडा."
                } else {
                    "No work records available. Please add some records first."
                }
                aiGenerationLoading = false
                return@launch
            }

            val clientLabel = selectedClient ?: (if (isMarathi) "सर्व ग्राहक" else "All Clients")
            val recordsSummary = currentRecords.joinToString(separator = "\n") { r ->
                "- ${r.date}: ${r.title} (${r.workType}, ${r.quantity} x ${r.rate} = ₹${r.totalAmount}) [Status: ${r.paymentStatus}]"
            }

            val systemPrompt = """
                You are a professional assistant specialized in composing work summaries, billing updates, and payment requests for Indian laborers, freelancers, and contract workers.
                You understand both Marathi and English.
                Your task is to draft a polite, clear, and highly professional work summary update based on the provided list of daily work records.
                Respond with the draft message.
                If the user has selected a specific client, include a polite request for outstanding payment if there is any pending balance.
                Keep the tone warm, respectful, and crystal clear.
                Always provide the output in two sections:
                1. मराठी संदेश (Marathi Message) - formatted for sharing on WhatsApp.
                2. English Message - formatted for sharing on WhatsApp.
                Include emojis where appropriate (like calendar, checkmark, cash, worker).
            """.trimIndent()

            val totalAmount = currentRecords.sumOf { it.totalAmount }
            val paidAmount = currentRecords.filter { it.paymentStatus == "PAID" }.sumOf { it.totalAmount }
            val pendingAmount = currentRecords.filter { it.paymentStatus == "PENDING" }.sumOf { it.totalAmount }

            val prompt = """
                Generate a billing update/summary for Client: $clientLabel.
                Here are the work records:
                $recordsSummary
                
                Summary Metrics:
                - Total Earned: ₹$totalAmount
                - Total Paid: ₹$paidAmount
                - Pending Balance: ₹$pendingAmount
                
                Format the message nicely with clear bullet points. Ensure it is easy to copy and share on WhatsApp.
            """.trimIndent()

            val result = GeminiRepository.generateContent(prompt = prompt, systemInstruction = systemPrompt)
            
            val upiSuffix = if (isUpiEnabled && upiId.isNotEmpty() && pendingAmount > 0) {
                val upiLink = getUpiPaymentUri(pendingAmount, "Outstanding Balance")
                if (isMarathi) {
                    "\n\n💸 *UPI द्वारे पेमेंट करा:*\n*नाव:* $payeeName\n*UPI ID:* $upiId\n*थेट पेमेंट करण्यासाठी लिंकवर क्लिक करा:* $upiLink"
                } else {
                    "\n\n💸 *Pay via UPI:*\n*Name:* $payeeName\n*UPI ID:* $upiId\n*Click to pay directly:* $upiLink"
                }
            } else {
                ""
            }
            
            aiGeneratedReport = result + upiSuffix
            aiGenerationLoading = false
        }
    }

    // Add installment payment
    fun addPaymentInstallment(record: WorkRecord, amount: Double, note: String, upiAccountLabel: String?) {
        val historyType = Types.newParameterizedType(List::class.java, PaymentInstallment::class.java)
        val adapter = moshi.adapter<List<PaymentInstallment>>(historyType)
        
        val currentHistory = try {
            adapter.fromJson(record.paymentHistoryJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val newInstallment = PaymentInstallment(
            id = UUID.randomUUID().toString(),
            date = dateStr,
            amount = amount,
            note = note,
            upiAccountLabel = upiAccountLabel
        )

        val updatedHistory = currentHistory + newInstallment
        val updatedHistoryJson = adapter.toJson(updatedHistory)

        val newPaidAmount = record.paidAmount + amount
        val newStatus = when {
            newPaidAmount >= record.totalAmount -> "PAID"
            newPaidAmount > 0.0 -> "PARTIAL"
            else -> "PENDING"
        }

        val updatedRecord = record.copy(
            paidAmount = newPaidAmount,
            paymentStatus = newStatus,
            paymentHistoryJson = updatedHistoryJson
        )

        viewModelScope.launch {
            repository.insert(updatedRecord)
        }
    }

    // Voice parsing AI helper
    fun parseVoiceInputAndAddTask(
        speechText: String, 
        onResult: (title: String, client: String, rate: Double, qty: Double, type: String, category: String) -> Unit
    ) {
        viewModelScope.launch {
            voiceParsingLoading = true
            val systemPrompt = """
                You are an intelligent natural language work-record parser. Your goal is to parse natural language speech into a structured JSON.
                The JSON MUST contain:
                - title (String): name of the daily task/work done (translate to Marathi if the speech is in Marathi, or English if in English)
                - clientName (String): project or employer/client name
                - rate (Double): rate per hour/day
                - quantity (Double): time spent (hours or days)
                - workType (String): one of "HOURLY", "DAILY", "FIXED"
                - category (String): one of "Farming", "Construction", "Plumbing", "Delivery", "Other"
                
                Respond ONLY with the raw JSON object. Do not include any markdown format blocks, backticks or comments.
            """.trimIndent()

            val prompt = "Parse this speech: \"$speechText\""
            try {
                val json = GeminiRepository.generateContent(prompt = prompt, systemInstruction = systemPrompt)
                val cleanJson = json.replace("```json", "").replace("```", "").trim()
                
                val adapter = moshi.adapter(Map::class.java)
                val map = adapter.fromJson(cleanJson)
                if (map != null) {
                    val title = map["title"]?.toString() ?: "Voice Task"
                    val client = map["clientName"]?.toString() ?: "General"
                    val rate = map["rate"]?.toString()?.toDoubleOrNull() ?: 100.0
                    val qty = map["quantity"]?.toString()?.toDoubleOrNull() ?: 1.0
                    val type = map["workType"]?.toString() ?: "FIXED"
                    val category = map["category"]?.toString() ?: "Other"
                    
                    onResult(title, client, rate, qty, type, category)
                }
            } catch (e: Exception) {
                onResult(speechText, "Voice Import", 100.0, 1.0, "FIXED", "Other")
            } finally {
                voiceParsingLoading = false
            }
        }
    }

    // AI Earning & Spending Insights
    fun generateEarningInsights() {
        viewModelScope.launch {
            isInsightsLoading = true
            aiInsightsReport = ""
            val records = allRecords.value
            if (records.isEmpty()) {
                aiInsightsReport = if (isMarathi) "हिशोब अहवाल तयार करण्यासाठी पुरेशी माहिती उपलब्ध नाही." else "No data available to generate insights."
                isInsightsLoading = false
                return@launch
            }
            val summary = records.joinToString(separator = "\n") { r ->
                "- Category: ${r.category}, Title: ${r.title} under Project: ${r.clientName} (Amount: ₹${r.totalAmount}, Paid: ₹${r.paidAmount}, Status: ${r.paymentStatus})"
            }

            val systemPrompt = """
                You are an expert AI financial advisor and business strategist for Indian laborers, freelancers, and small business owners.
                Analyze the user's daily work records, categories, and payment statuses.
                Provide clear, actionable insights in both Marathi (मराठी) and English:
                1. Earnings overview & average rate per category.
                2. Client dependency warnings (if one client dominates earnings).
                3. Projections for the next month.
                4. Strategic advice to increase overall hourly/daily rates or reduce pending balances.
                Use clear markdown bullet points and emojis to make it professional and extremely readable.
            """.trimIndent()

            val prompt = "Here are my work records:\n$summary"
            try {
                aiInsightsReport = GeminiRepository.generateContent(prompt = prompt, systemInstruction = systemPrompt)
            } catch (e: Exception) {
                aiInsightsReport = "Failed to generate insights: ${e.localizedMessage}"
            } finally {
                isInsightsLoading = false
            }
        }
    }

    // Built-in AI Chat Assistant
    fun sendChatMessage(messageText: String) {
        val userMsg = ChatMessage(id = UUID.randomUUID().toString(), content = messageText, isUser = true)
        chatMessages = chatMessages + userMsg
        
        viewModelScope.launch {
            isChatLoading = true
            val records = allRecords.value
            val summary = records.take(15).joinToString(separator = "\n") { r ->
                "- ${r.date}: ${r.title} under ${r.clientName} (Amount: ₹${r.totalAmount}, Paid: ₹${r.paidAmount}, Status: ${r.paymentStatus})"
            }

            val systemPrompt = """
                You are "Kamacha Hisob AI Assistant", an expert, friendly AI financial companion inside a Work Record & Expense Tracking App.
                You help workers, freelancers, and contractors manage their jobs, calculate payments, and secure their hard-earned money.
                You have direct access to their recent 15 records in the database:
                $summary
                
                Answer the user's questions clearly, concisely, and supportively. Use calculations and values from their database whenever they ask about their income, clients, or pending dues.
                Answer in Marathi if the user asks in Marathi or Marathi script, or English otherwise.
            """.trimIndent()

            val prompt = "User message: $messageText"
            try {
                val reply = GeminiRepository.generateContent(prompt = prompt, systemInstruction = systemPrompt)
                val assistantMsg = ChatMessage(id = UUID.randomUUID().toString(), content = reply, isUser = false)
                chatMessages = chatMessages + assistantMsg
            } catch (e: Exception) {
                val errorMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = "Error: Could not connect to assistant. Please try again.",
                    isUser = false
                )
                chatMessages = chatMessages + errorMsg
            } finally {
                isChatLoading = false
            }
        }
    }

    fun clearChat() {
        chatMessages = emptyList()
    }

    // Cloud backup / Export-Import JSON
    fun exportBackupToJson(): String {
        return try {
            val type = Types.newParameterizedType(List::class.java, WorkRecord::class.java)
            moshi.adapter<List<WorkRecord>>(type).toJson(allRecords.value)
        } catch (e: Exception) {
            ""
        }
    }

    fun importBackupFromJson(jsonString: String): Boolean {
        return try {
            val type = Types.newParameterizedType(List::class.java, WorkRecord::class.java)
            val records = moshi.adapter<List<WorkRecord>>(type).fromJson(jsonString)
            if (records != null) {
                viewModelScope.launch {
                    records.forEach { repository.insert(it) }
                }
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }
}


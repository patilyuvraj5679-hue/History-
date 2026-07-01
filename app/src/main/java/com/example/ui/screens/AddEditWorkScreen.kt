package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WorkRecord
import com.example.ui.LanguageHelper
import com.example.ui.WorkViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditWorkScreen(
    viewModel: WorkViewModel,
    recordToEdit: WorkRecord?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMarathi = viewModel.isMarathi
    val uniqueClients by viewModel.uniqueClients.collectAsState()

    // Form states
    var title by remember { mutableStateOf(recordToEdit?.title ?: "") }
    var clientName by remember { mutableStateOf(recordToEdit?.clientName ?: "") }
    var workType by remember { mutableStateOf(recordToEdit?.workType ?: "DAILY") }
    var rateInput by remember { mutableStateOf(recordToEdit?.rate?.toString() ?: "") }
    var quantityInput by remember { mutableStateOf(recordToEdit?.quantity?.toString() ?: "1.0") }
    var paymentStatus by remember { mutableStateOf(recordToEdit?.paymentStatus ?: "PENDING") }
    var notes by remember { mutableStateOf(recordToEdit?.notes ?: "") }
    var category by remember { mutableStateOf(recordToEdit?.category ?: "Other") }
    
    // Date picker state (we default to today in YYYY-MM-DD)
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    var dateInput by remember { mutableStateOf(recordToEdit?.date ?: todayStr) }

    // Validation errors
    var titleError by remember { mutableStateOf(false) }
    var clientError by remember { mutableStateOf(false) }
    var rateError by remember { mutableStateOf(false) }
    var qtyError by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = if (recordToEdit == null) {
                            LanguageHelper.getText(isMarathi, "add_record")
                        } else {
                            LanguageHelper.getText(isMarathi, "edit_record")
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { viewModel.toggleLanguage() },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Change Language",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = LanguageHelper.getText(isMarathi, "toggle_language"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = false
                },
                label = { Text(LanguageHelper.getText(isMarathi, "work_title")) },
                placeholder = { Text("उदा. कापूस वेचणी, पाईप लाईन, भाजीपाला") },
                isError = titleError,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("title_input"),
                shape = RoundedCornerShape(12.dp)
            )
            if (titleError) {
                Text(
                    text = LanguageHelper.getText(isMarathi, "m_text_field_empty"),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Client Name Input
            OutlinedTextField(
                value = clientName,
                onValueChange = {
                    clientName = it
                    clientError = false
                },
                label = { Text(LanguageHelper.getText(isMarathi, "client_name")) },
                placeholder = { Text("उदा. रमेश पाटील, नामदेव राव") },
                isError = clientError,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("client_input"),
                shape = RoundedCornerShape(12.dp)
            )
            if (clientError) {
                Text(
                    text = LanguageHelper.getText(isMarathi, "m_text_field_empty"),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }

            // Client Suggestions
            if (uniqueClients.isNotEmpty() && clientName.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = LanguageHelper.getText(isMarathi, "client_suggestions"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uniqueClients.take(4).forEach { client ->
                        SuggestionChip(
                            onClick = {
                                clientName = client
                                clientError = false
                            },
                            label = { Text(client) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date Input
            OutlinedTextField(
                value = dateInput,
                onValueChange = { dateInput = it },
                label = { Text(LanguageHelper.getText(isMarathi, "work_date")) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Date fast selector (Today, Yesterday)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = { dateInput = todayStr },
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(text = "📅 ${LanguageHelper.getText(isMarathi, "today")}")
                }
                val yesterdayStr = remember {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DATE, -1)
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                }
                TextButton(
                    onClick = { dateInput = yesterdayStr },
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(text = "📅 ${LanguageHelper.getText(isMarathi, "yesterday")}")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Task Category
            Text(
                text = if (isMarathi) "कामाचा वर्ग (Task Category)" else "Task Category",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf("Farming", "Construction", "Plumbing", "Delivery", "Other")
                categories.forEach { cat ->
                    val isSelected = category == cat
                    val catLabel = when (cat) {
                        "Farming" -> if (isMarathi) "🚜 शेती काम" else "🚜 Farming"
                        "Construction" -> if (isMarathi) "🧱 बांधकाम" else "🧱 Construction"
                        "Plumbing" -> if (isMarathi) "🔧 प्लंबिंग" else "🔧 Plumbing"
                        "Delivery" -> if (isMarathi) "📦 माल वाहतूक" else "📦 Delivery"
                        else -> if (isMarathi) "⚙️ इतर" else "⚙️ Other"
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { category = cat },
                        label = { Text(catLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Work Type Segmented Bar (Hourly, Daily, Fixed)
            Text(
                text = LanguageHelper.getText(isMarathi, "work_type"),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                val types = listOf("HOURLY", "DAILY", "FIXED")
                types.forEach { type ->
                    val isSelected = workType == type
                    val label = when (type) {
                        "HOURLY" -> LanguageHelper.getText(isMarathi, "hourly")
                        "DAILY" -> LanguageHelper.getText(isMarathi, "daily")
                        else -> LanguageHelper.getText(isMarathi, "fixed")
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                workType = type
                                if (type == "FIXED") {
                                    quantityInput = "1.0"
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rate and Quantity inputs side-by-side (if not FIXED)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rate
                OutlinedTextField(
                    value = rateInput,
                    onValueChange = {
                        rateInput = it
                        rateError = false
                    },
                    label = { Text(LanguageHelper.getText(isMarathi, "rate")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = rateError,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rate_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Quantity (Only show if Hourly or Daily)
                if (workType != "FIXED") {
                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = {
                            quantityInput = it
                            qtyError = false
                        },
                        label = { Text(LanguageHelper.getText(isMarathi, "quantity_hours")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = qtyError,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("qty_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            if (rateError) {
                Text(
                    text = LanguageHelper.getText(isMarathi, "rate_error"),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }
            if (qtyError && workType != "FIXED") {
                Text(
                    text = LanguageHelper.getText(isMarathi, "qty_error"),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }

            // Real-time Estimated Earnings Calculation
            val rateVal = rateInput.toDoubleOrNull() ?: 0.0
            val qtyVal = if (workType == "FIXED") 1.0 else (quantityInput.toDoubleOrNull() ?: 0.0)
            val estimatedEarnings = rateVal * qtyVal

            Spacer(modifier = Modifier.height(16.dp))

            // Real-time Estimated Earnings Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isMarathi) "अंदाजित कमाई (Estimated Earnings)" else "Estimated Earnings",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${"%,.2f".format(estimatedEarnings)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = when (workType) {
                                "HOURLY" -> if (isMarathi) "₹$rateVal × $qtyVal तास" else "₹$rateVal × $qtyVal hrs"
                                "DAILY" -> if (isMarathi) "₹$rateVal × $qtyVal दिवस" else "₹$rateVal × $qtyVal days"
                                else -> if (isMarathi) "ठराविक" else "Fixed"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Status Switch Card
            Text(
                text = LanguageHelper.getText(isMarathi, "filter_status"),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val statusText = if (paymentStatus == "PAID") {
                            LanguageHelper.getText(isMarathi, "paid")
                        } else {
                            LanguageHelper.getText(isMarathi, "pending")
                        }
                        Text(
                            text = statusText,
                            fontWeight = FontWeight.Bold,
                            color = if (paymentStatus == "PAID") Color(0xFF2ECC71) else MaterialTheme.colorScheme.primary
                        )
                    }

                    Switch(
                        checked = paymentStatus == "PAID",
                        onCheckedChange = { isChecked ->
                            paymentStatus = if (isChecked) "PAID" else "PENDING"
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF2ECC71),
                            checkedTrackColor = Color(0xFFD4EFDF)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes Input
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(LanguageHelper.getText(isMarathi, "notes")) },
                placeholder = { Text("उदा. भाजीपाला माल वाहतूक भाडे किंवा इतर तपशील") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Actions Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel Button
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Text(
                        text = LanguageHelper.getText(isMarathi, "cancel"),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Save Button
                Button(
                    onClick = {
                        // Validate inputs
                        val hasTitle = title.trim().isNotEmpty()
                        val hasClient = clientName.trim().isNotEmpty()
                        val rate = rateInput.toDoubleOrNull()
                        val qty = if (workType == "FIXED") 1.0 else quantityInput.toDoubleOrNull()

                        titleError = !hasTitle
                        clientError = !hasClient
                        rateError = rate == null
                        qtyError = qty == null

                        if (hasTitle && hasClient && rate != null && qty != null) {
                            viewModel.saveWorkRecord(
                                id = recordToEdit?.id ?: 0,
                                title = title.trim(),
                                date = dateInput.trim(),
                                clientName = clientName.trim(),
                                workType = workType,
                                quantity = qty,
                                rate = rate,
                                paymentStatus = paymentStatus,
                                notes = notes.trim(),
                                category = category,
                                paidAmount = recordToEdit?.paidAmount ?: 0.0,
                                paymentHistoryJson = recordToEdit?.paymentHistoryJson ?: "[]"
                            )
                            onBack()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_button"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Done, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = LanguageHelper.getText(isMarathi, "save"),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

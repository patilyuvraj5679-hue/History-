package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.LanguageHelper
import com.example.ui.WorkViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: WorkViewModel,
    onAddRecord: () -> Unit,
    onEditRecord: (WorkRecord) -> Unit,
    onGoToUpdates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMarathi = viewModel.isMarathi
    val filteredRecords by viewModel.filteredRecords.collectAsState()
    val allRecords by viewModel.allRecords.collectAsState()
    val uniqueClients by viewModel.uniqueClients.collectAsState()
    val selectedClient by viewModel.selectedClientFilter.collectAsState()
    val selectedStatus by viewModel.selectedStatusFilter.collectAsState()

    // Search and Sort states
    var searchQuery by remember { mutableStateOf("") }
    var sortMethod by remember { mutableStateOf("DATE_DESC") } // DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC

    // Tab control: "LIST", "CALENDAR", "AI_CHAT"
    var activeTab by remember { mutableStateOf("LIST") }

    // Dialog & overlay states
    var recordToDelete by remember { mutableStateOf<WorkRecord?>(null) }
    var showUpiAccountsDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var selectedRecordForDetails by remember { mutableStateOf<WorkRecord?>(null) }
    var recordForReceipt by remember { mutableStateOf<WorkRecord?>(null) }

    // Voice parsing input states
    var voiceInputText by remember { mutableStateOf("") }
    var showVoiceParsedResult by remember { mutableStateOf(false) }
    var parsedTitle by remember { mutableStateOf("") }
    var parsedClient by remember { mutableStateOf("") }
    var parsedRate by remember { mutableStateOf(0.0) }
    var parsedQty by remember { mutableStateOf(1.0) }
    var parsedType by remember { mutableStateOf("DAILY") }
    var parsedCategory by remember { mutableStateOf("Other") }

    // UPI setup variables (adding a new account)
    var upiAccountLabel by remember { mutableStateOf("") }
    var upiAccountVpa by remember { mutableStateOf("") }
    var upiAccountName by remember { mutableStateOf("") }
    var upiIdError by remember { mutableStateOf<String?>(null) }
    var upiNameError by remember { mutableStateOf<String?>(null) }
    var upiLabelError by remember { mutableStateOf<String?>(null) }

    // Installment input states (in details dialog)
    var installmentAmountInput by remember { mutableStateOf("") }
    var installmentNoteInput by remember { mutableStateOf("") }
    var selectedUpiAccountLabelForInstallment by remember { mutableStateOf<String?>(null) }

    // Chatbot state
    var chatInputText by remember { mutableStateOf("") }
    val chatMessages = viewModel.chatMessages
    val isChatLoading = viewModel.isChatLoading

    // Calendar states
    val calendarInstance = remember { Calendar.getInstance() }
    var currentYear by remember { mutableStateOf(calendarInstance.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableStateOf(calendarInstance.get(Calendar.MONTH)) } // 0-indexed
    var selectedCalendarDate by remember { mutableStateOf<String?>(null) }

    // Dropdown expanded states
    var clientDropdownExpanded by remember { mutableStateOf(false) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }
    var sortDropdownExpanded by remember { mutableStateOf(false) }

    // UPI QR Dialog
    var upiQrAmount by remember { mutableStateOf(0.0) }
    var upiQrNote by remember { mutableStateOf("") }
    var showUpiQrDialog by remember { mutableStateOf(false) }

    // Stats calculations using the robust multi-installment structure
    val totalEarnings = allRecords.sumOf { it.totalAmount }
    val paidAmount = allRecords.sumOf { it.paidAmount }
    val pendingAmount = totalEarnings - paidAmount

    val context = LocalContext.current

    // Client-side local searching and sorting of filtered records
    val sortedAndSearchedRecords = remember(filteredRecords, searchQuery, selectedCalendarDate, sortMethod) {
        filteredRecords
            .filter { record ->
                val matchesSearch = searchQuery.isBlank() ||
                        record.title.contains(searchQuery, ignoreCase = true) ||
                        record.clientName.contains(searchQuery, ignoreCase = true) ||
                        record.category.contains(searchQuery, ignoreCase = true)
                val matchesCalendar = selectedCalendarDate == null || record.date == selectedCalendarDate
                matchesSearch && matchesCalendar
            }
            .sortedWith { r1, r2 ->
                when (sortMethod) {
                    "DATE_ASC" -> r1.date.compareTo(r2.date)
                    "AMOUNT_DESC" -> r2.totalAmount.compareTo(r1.totalAmount)
                    "AMOUNT_ASC" -> r1.totalAmount.compareTo(r2.totalAmount)
                    else -> r2.date.compareTo(r1.date) // "DATE_DESC"
                }
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = LanguageHelper.getText(isMarathi, "app_title"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = LanguageHelper.getText(isMarathi, "app_subtitle"),
                            fontSize = 11.sp,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { viewModel.toggleLanguage() },
                        modifier = Modifier.padding(end = 4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Change Language",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = LanguageHelper.getText(isMarathi, "toggle_language"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(
                        onClick = { showUpiAccountsDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "UPI Accounts",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showBackupDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Backup & Cloud Data",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Voice Input FAB
                SmallFloatingActionButton(
                    onClick = { showVoiceDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = "Voice Record Entry")
                }
                // Add Standard Task FAB
                FloatingActionButton(
                    onClick = onAddRecord,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_record_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = LanguageHelper.getText(isMarathi, "add_record")
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Hero / Stats Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LanguageHelper.getText(isMarathi, "total_earnings"),
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        IconButton(
                            onClick = onGoToUpdates,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Report Updates",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = "₹${"%,.2f".format(totalEarnings)}",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Paid Status Card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF2ECC71),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = LanguageHelper.getText(isMarathi, "paid_amount"),
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "₹${"%,.0f".format(paidAmount)}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }
                        }

                        // Outstanding Balance Card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Pending,
                                        contentDescription = null,
                                        tint = Color(0xFFF1C40F),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = LanguageHelper.getText(isMarathi, "pending_amount"),
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "₹${"%,.0f".format(pendingAmount)}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions Button & Navigation Tabs Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tab Selection Buttons
                val tabOptions = listOf(
                    "LIST" to Pair(Icons.Default.List, if (isMarathi) "यादी" else "List"),
                    "CALENDAR" to Pair(Icons.Default.CalendarMonth, if (isMarathi) "कॅलेंडर" else "Calendar"),
                    "AI_CHAT" to Pair(Icons.Default.Forum, if (isMarathi) "सहाय्यक" else "AI Chat")
                )
                tabOptions.forEach { (tabKey, details) ->
                    val isSelected = activeTab == tabKey
                    FilledTonalButton(
                        onClick = {
                            activeTab = tabKey
                            selectedCalendarDate = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Icon(imageVector = details.first, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = details.second, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Tab View switcher
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "dashboard_tab_transitions"
            ) { tab ->
                when (tab) {
                    "LIST" -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Search and Sort controls
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Search bar
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text(if (isMarathi) "शोध घ्या..." else "Search...", fontSize = 13.sp) },
                                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    modifier = Modifier.weight(1.5f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                    )
                                )

                                // Sort button trigger
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { sortDropdownExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = when (sortMethod) {
                                                    "DATE_ASC" -> if (isMarathi) "दिनांक ⬆" else "Date Oldest"
                                                    "AMOUNT_DESC" -> if (isMarathi) "कमाई ⬇" else "Amt Highest"
                                                    "AMOUNT_ASC" -> if (isMarathi) "कमाई ⬆" else "Amt Lowest"
                                                    else -> if (isMarathi) "दिनांक ⬇" else "Date Newest"
                                                },
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = sortDropdownExpanded,
                                        onDismissRequest = { sortDropdownExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(if (isMarathi) "दिनांक: नवीन आधी" else "Newest First") },
                                            onClick = { sortMethod = "DATE_DESC"; sortDropdownExpanded = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(if (isMarathi) "दिनांक: जुने आधी" else "Oldest First") },
                                            onClick = { sortMethod = "DATE_ASC"; sortDropdownExpanded = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(if (isMarathi) "कमाई: सर्वाधिक" else "Highest Earnings") },
                                            onClick = { sortMethod = "AMOUNT_DESC"; sortDropdownExpanded = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(if (isMarathi) "कमाई: सर्वात कमी" else "Lowest Earnings") },
                                            onClick = { sortMethod = "AMOUNT_ASC"; sortDropdownExpanded = false }
                                        )
                                    }
                                }
                            }

                            // Filter section with status and client
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Client filter dropdown
                                Box(modifier = Modifier.weight(1.2f)) {
                                    OutlinedButton(
                                        onClick = { clientDropdownExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = selectedClient?.let { if (it == "All") LanguageHelper.getText(isMarathi, "all") else it } ?: LanguageHelper.getText(isMarathi, "all"),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontSize = 12.sp
                                            )
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    DropdownMenu(expanded = clientDropdownExpanded, onDismissRequest = { clientDropdownExpanded = false }) {
                                        DropdownMenuItem(text = { Text(LanguageHelper.getText(isMarathi, "all")) }, onClick = { viewModel.setClientFilter("All"); clientDropdownExpanded = false })
                                        uniqueClients.forEach { client ->
                                            DropdownMenuItem(text = { Text(client) }, onClick = { viewModel.setClientFilter(client); clientDropdownExpanded = false })
                                        }
                                    }
                                }

                                // Status filter dropdown
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { statusDropdownExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = when (selectedStatus) {
                                                    "PAID" -> LanguageHelper.getText(isMarathi, "paid")
                                                    "PENDING" -> LanguageHelper.getText(isMarathi, "pending")
                                                    "PARTIAL" -> if (isMarathi) "अंशत: मिळाले" else "Partial"
                                                    else -> LanguageHelper.getText(isMarathi, "all")
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontSize = 12.sp
                                            )
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    DropdownMenu(expanded = statusDropdownExpanded, onDismissRequest = { statusDropdownExpanded = false }) {
                                        DropdownMenuItem(text = { Text(LanguageHelper.getText(isMarathi, "all")) }, onClick = { viewModel.setStatusFilter("All"); statusDropdownExpanded = false })
                                        DropdownMenuItem(text = { Text(LanguageHelper.getText(isMarathi, "paid")) }, onClick = { viewModel.setStatusFilter("PAID"); statusDropdownExpanded = false })
                                        DropdownMenuItem(text = { Text(LanguageHelper.getText(isMarathi, "pending")) }, onClick = { viewModel.setStatusFilter("PENDING"); statusDropdownExpanded = false })
                                        DropdownMenuItem(text = { Text(if (isMarathi) "अंशत: (Partial)" else "Partial") }, onClick = { viewModel.setStatusFilter("PARTIAL"); statusDropdownExpanded = false })
                                    }
                                }
                            }

                            // Dynamic listing
                            if (sortedAndSearchedRecords.isEmpty()) {
                                ListEmptyState(isMarathi)
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(sortedAndSearchedRecords, key = { it.id }) { record ->
                                        WorkRecordItemCard(
                                            record = record,
                                            isMarathi = isMarathi,
                                            isUpiEnabled = viewModel.isUpiEnabled,
                                            onShowUpiQr = {
                                                upiQrAmount = record.totalAmount - record.paidAmount
                                                upiQrNote = "${record.title} (${record.clientName})"
                                                showUpiQrDialog = true
                                            },
                                            onToggleStatus = {
                                                val newStatus = if (record.paymentStatus == "PAID") "PENDING" else "PAID"
                                                val newPaid = if (newStatus == "PAID") record.totalAmount else 0.0
                                                viewModel.saveWorkRecord(
                                                    id = record.id,
                                                    title = record.title,
                                                    date = record.date,
                                                    clientName = record.clientName,
                                                    workType = record.workType,
                                                    quantity = record.quantity,
                                                    rate = record.rate,
                                                    paymentStatus = newStatus,
                                                    notes = record.notes,
                                                    category = record.category,
                                                    paidAmount = newPaid,
                                                    paymentHistoryJson = record.paymentHistoryJson
                                                )
                                            },
                                            onDetails = { selectedRecordForDetails = record },
                                            onDelete = { recordToDelete = record }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "CALENDAR" -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Calendar View Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val monthName = remember<String>(currentMonth) {
                                    java.text.DateFormatSymbols().months[currentMonth]
                                }
                                IconButton(onClick = {
                                    if (currentMonth == 0) {
                                        currentMonth = 11
                                        currentYear -= 1
                                    } else {
                                        currentMonth -= 1
                                    }
                                }) {
                                    Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                                }
                                Text(
                                    text = "$monthName $currentYear",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = {
                                    if (currentMonth == 11) {
                                        currentMonth = 0
                                        currentYear += 1
                                    } else {
                                        currentMonth += 1
                                    }
                                }) {
                                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Month")
                                }
                            }

                            // Calendar Grid
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                val daysInMonth = remember(currentMonth, currentYear) {
                                    val calendar = Calendar.getInstance()
                                    calendar.set(currentYear, currentMonth, 1)
                                    calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                                }
                                val firstDayOfWeek = remember(currentMonth, currentYear) {
                                    val calendar = Calendar.getInstance()
                                    calendar.set(currentYear, currentMonth, 1)
                                    calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0-6 starting Sunday
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Weekday abbreviations
                                    val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        dayLabels.forEach { label ->
                                            Text(
                                                text = label,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.weight(1f),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Divider()

                                    // Calendar rows
                                    var currentDay = 1
                                    for (row in 0..5) {
                                        if (currentDay > daysInMonth) break
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            for (col in 0..6) {
                                                if (row == 0 && col < firstDayOfWeek) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                } else if (currentDay > daysInMonth) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                } else {
                                                    val dayNum = currentDay
                                                    val dateString = "%04d-%02d-%02d".format(currentYear, currentMonth + 1, dayNum)
                                                    val tasksOnThisDay = allRecords.filter { it.date == dateString }
                                                    val isSelected = selectedCalendarDate == dateString

                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .aspectRatio(1f)
                                                            .padding(2.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                                else if (tasksOnThisDay.isNotEmpty()) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                                                else Color.Transparent
                                                            )
                                                            .clickable {
                                                                selectedCalendarDate = if (isSelected) null else dateString
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Text(
                                                                text = dayNum.toString(),
                                                                fontSize = 13.sp,
                                                                fontWeight = if (tasksOnThisDay.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                            )
                                                            if (tasksOnThisDay.isNotEmpty()) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(4.dp)
                                                                        .clip(CircleShape)
                                                                        .background(
                                                                            if (tasksOnThisDay.all { it.paymentStatus == "PAID" }) Color(0xFF2ECC71)
                                                                            else Color(0xFFE74C3C)
                                                                        )
                                                                )
                                                            }
                                                        }
                                                    }
                                                    currentDay++
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Display tasks of clicked day
                            Text(
                                text = if (selectedCalendarDate != null) {
                                    if (isMarathi) "तारीख $selectedCalendarDate च्या नोंदी:" else "Records for $selectedCalendarDate:"
                                } else {
                                    if (isMarathi) "सर्व कॅलेंडर नोंदी (कृपया दिवस निवडा):" else "All Calendar Records (Click a day):"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (sortedAndSearchedRecords.isEmpty()) {
                                ListEmptyState(isMarathi)
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(sortedAndSearchedRecords, key = { it.id }) { record ->
                                        WorkRecordItemCard(
                                            record = record,
                                            isMarathi = isMarathi,
                                            isUpiEnabled = viewModel.isUpiEnabled,
                                            onShowUpiQr = {
                                                upiQrAmount = record.totalAmount - record.paidAmount
                                                upiQrNote = "${record.title} (${record.clientName})"
                                                showUpiQrDialog = true
                                            },
                                            onToggleStatus = {
                                                val newStatus = if (record.paymentStatus == "PAID") "PENDING" else "PAID"
                                                val newPaid = if (newStatus == "PAID") record.totalAmount else 0.0
                                                viewModel.saveWorkRecord(
                                                    id = record.id,
                                                    title = record.title,
                                                    date = record.date,
                                                    clientName = record.clientName,
                                                    workType = record.workType,
                                                    quantity = record.quantity,
                                                    rate = record.rate,
                                                    paymentStatus = newStatus,
                                                    notes = record.notes,
                                                    category = record.category,
                                                    paidAmount = newPaid,
                                                    paymentHistoryJson = record.paymentHistoryJson
                                                )
                                            },
                                            onDetails = { selectedRecordForDetails = record },
                                            onDelete = { recordToDelete = record }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "AI_CHAT" -> {
                        // AI Chat Companion interface
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.SmartButton, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isMarathi) "हिशोब मदतनीस: माझ्याशी कमाई किंवा पैशांबद्दल चर्चा करा!" else "Kamacha Hisob Assistant: Ask me anything about your earnings!",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Message log
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (chatMessages.isEmpty()) {
                                    item {
                                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (isMarathi) "अद्याप संभाषण नाही. खाली काहीतरी विचारा!" else "No messages yet. Ask me your questions below!",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    items(chatMessages) { message ->
                                        val isUser = message.isUser
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                        ) {
                                            Card(
                                                shape = RoundedCornerShape(
                                                    topStart = 16.dp,
                                                    topEnd = 16.dp,
                                                    bottomStart = if (isUser) 16.dp else 0.dp,
                                                    bottomEnd = if (isUser) 0.dp else 16.dp
                                                ),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                                modifier = Modifier.widthIn(max = 280.dp)
                                            ) {
                                                Text(
                                                    text = message.content,
                                                    fontSize = 13.sp,
                                                    modifier = Modifier.padding(12.dp),
                                                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isChatLoading) {
                                    item {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                            Card(
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                                modifier = Modifier.padding(8.dp)
                                            ) {
                                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(text = if (isMarathi) "विचार करत आहे..." else "Thinking...", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Suggestions chips
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val suggestions = if (isMarathi) {
                                    listOf("माझी या महिन्याची एकूण कमाई किती?", "बाकी असलेले पैसे कसे मिळवू?", "मला काही बचत सल्ला द्या")
                                } else {
                                    listOf("What are my total earnings this month?", "How do I collect pending balances?", "Give me budget tips")
                                }
                                suggestions.forEach { suggestion ->
                                    SuggestionChip(
                                        onClick = {
                                            viewModel.sendChatMessage(suggestion)
                                        },
                                        label = { Text(suggestion, fontSize = 11.sp) }
                                    )
                                }
                            }

                            // Chat keyboard input
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = chatInputText,
                                    onValueChange = { chatInputText = it },
                                    placeholder = { Text(if (isMarathi) "सहाय्यकाला विचारा..." else "Ask assistant...", fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(20.dp),
                                    maxLines = 2,
                                    singleLine = false
                                )
                                IconButton(
                                    onClick = {
                                        if (chatInputText.isNotBlank()) {
                                            viewModel.sendChatMessage(chatInputText.trim())
                                            chatInputText = ""
                                        }
                                    },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                                ) {
                                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text(LanguageHelper.getText(isMarathi, "delete_record"), fontWeight = FontWeight.Bold) },
            text = { Text(LanguageHelper.getText(isMarathi, "delete_confirm")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        recordToDelete?.let { viewModel.deleteWorkRecord(it) }
                        recordToDelete = null
                    }
                ) {
                    Text(
                        LanguageHelper.getText(isMarathi, "delete_record"),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text(LanguageHelper.getText(isMarathi, "cancel"))
                }
            }
        )
    }

    // Multiple UPI Accounts Setup Manager Dialog
    if (showUpiAccountsDialog) {
        val registeredAccounts = viewModel.upiAccounts

        AlertDialog(
            onDismissRequest = { showUpiAccountsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (isMarathi) "UPI बँक खाती व्यवस्थापन" else "UPI Accounts Manager", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = if (isMarathi) "नोंदणीकृत यूपीआय खाती:" else "Registered UPI Accounts:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    if (registeredAccounts.isEmpty()) {
                        Text(
                            text = if (isMarathi) "अद्याप कोणतेही खाते जोडलेले नाही." else "No accounts added yet.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    } else {
                        registeredAccounts.forEach { acc ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = acc.label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            if (acc.isDefault) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(text = if (isMarathi) "मुख्य" else "Default", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        }
                                        Text(text = acc.upiId, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = acc.payeeName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (!acc.isDefault) {
                                            IconButton(onClick = { viewModel.setDefaultUpiAccount(acc.id) }, modifier = Modifier.size(28.dp)) {
                                                Icon(imageVector = Icons.Default.StarBorder, contentDescription = "Set Default", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        IconButton(onClick = { viewModel.deleteUpiAccount(acc.id) }, modifier = Modifier.size(28.dp)) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Divider()

                    Text(text = if (isMarathi) "नवीन खाते जोडा:" else "Add New Account:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    OutlinedTextField(
                        value = upiAccountLabel,
                        onValueChange = { upiAccountLabel = it; upiLabelError = null },
                        label = { Text(if (isMarathi) "खात्याचे नाव (उदा. GPay, वैयक्तिक)" else "Label (e.g. My GPay)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = upiLabelError != null,
                        supportingText = upiLabelError?.let { { Text(it) } },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = upiAccountVpa,
                        onValueChange = { upiAccountVpa = it; upiIdError = null },
                        label = { Text("UPI ID (उदा. name@upi)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = upiIdError != null,
                        supportingText = upiIdError?.let { { Text(it) } },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = upiAccountName,
                        onValueChange = { upiAccountName = it; upiNameError = null },
                        label = { Text(if (isMarathi) "खातेदाराचे नाव" else "Payee Name") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = upiNameError != null,
                        supportingText = upiNameError?.let { { Text(it) } },
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            var valid = true
                            if (upiAccountLabel.trim().isEmpty()) {
                                upiLabelError = "Label is required"
                                valid = false
                            }
                            if (upiAccountVpa.trim().isEmpty() || !upiAccountVpa.contains("@")) {
                                upiIdError = "Valid UPI ID is required"
                                valid = false
                            }
                            if (upiAccountName.trim().isEmpty()) {
                                upiNameError = "Payee name is required"
                                valid = false
                            }

                            if (valid) {
                                viewModel.saveUpiAccount(upiAccountLabel.trim(), upiAccountVpa.trim(), upiAccountName.trim())
                                Toast.makeText(context, if (isMarathi) "UPI खाते जतन झाले!" else "UPI Account Saved!", Toast.LENGTH_SHORT).show()
                                upiAccountLabel = ""
                                upiAccountVpa = ""
                                upiAccountName = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = if (isMarathi) "खाते जतन करा" else "Save Account")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUpiAccountsDialog = false }) {
                    Text(text = if (isMarathi) "बंद करा" else "Close")
                }
            }
        )
    }

    // Cloud, JSON, & CSV Backup Manager Dialog
    if (showBackupDialog) {
        var autoBackupEnabled by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (isMarathi) "डेटा सुरक्षितता आणि बॅकअप" else "Data Security & Backup", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isMarathi) "तुमच्या कामाची सर्व माहिती तुमच्या फोनवर आणि क्लाउडवर सुरक्षित ठेवली जाते."
                        else "All your work records are stored securely offline and can be synced with cloud servers.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Auto Backup toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = if (isMarathi) "स्वयंचलित क्लाउड बॅकअप" else "Auto Cloud Backup", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = if (isMarathi) "वायफाय वर दररोज रात्री सुरक्षित बॅकअप" else "Saves safely to cloud storage daily", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = autoBackupEnabled, onCheckedChange = { autoBackupEnabled = it })
                    }

                    // Export buttons
                    Button(
                        onClick = {
                            val backupJson = viewModel.exportBackupToJson()
                            if (backupJson.isNotEmpty()) {
                                val shareIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, backupJson)
                                    type = "application/json"
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share JSON Backup"))
                            } else {
                                Toast.makeText(context, "Export empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (isMarathi) "JSON हिशोब बॅकअप निर्यात करा" else "Export JSON Backup File")
                    }

                    // CSV report generation
                    OutlinedButton(
                        onClick = {
                            val records = allRecords
                            if (records.isNotEmpty()) {
                                val csvBuilder = StringBuilder()
                                csvBuilder.append("ID,Date,Title,Client,Work Type,Rate,Quantity,Total,Status,Category,Notes\n")
                                records.forEach { r ->
                                    csvBuilder.append("${r.id},${r.date},\"${r.title}\",\"${r.clientName}\",${r.workType},${r.rate},${r.quantity},${r.totalAmount},${r.paymentStatus},${r.category},\"${r.notes}\"\n")
                                }
                                val shareIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, csvBuilder.toString())
                                    type = "text/csv"
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Excel CSV Report"))
                            } else {
                                Toast.makeText(context, "No records to export", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.TableChart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (isMarathi) "Excel (CSV) अहवाल तयार करा" else "Generate Excel (CSV) Report")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text(text = if (isMarathi) "बंद करा" else "Close")
                }
            }
        )
    }

    // Voice Input Assistant Dialog
    if (showVoiceDialog) {
        var isListeningSimulated by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showVoiceDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (isMarathi) "आवाज किंवा नैसर्गिक भाषा नोंद" else "Voice & Natural Language Entry", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isMarathi) {
                            "नोंद बोलून प्रविष्ट करा (उदा. 'रमेश पाटील कापूस वेचणी ३०० रुपये दर ८ तास' किंवा 'आज प्लंबिंग काम केले ४००० रुपये फिक्स दर')"
                        } else {
                            "Speak or write your task (e.g. 'Ramesh Patil cotton farming rate 300 amount 8 hours' or 'today plumbing work done 4000 fixed')"
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = voiceInputText,
                        onValueChange = { voiceInputText = it },
                        placeholder = { Text(if (isMarathi) "येथे बोला किंवा टाइप करा..." else "Type or paste your spoken text here...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Simulated speech recognition toggle
                        Button(
                            onClick = {
                                isListeningSimulated = !isListeningSimulated
                                if (isListeningSimulated) {
                                    voiceInputText = if (isMarathi) {
                                        "रमेश पाटील कापूस वेचणी दर ३०० एकूण ८ तास"
                                    } else {
                                        "Ramesh Patil cotton farming rate 300 total 8 hours"
                                    }
                                    isListeningSimulated = false
                                }
                            }
                        ) {
                            Icon(imageVector = if (isListeningSimulated) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isListeningSimulated) "Listening..." else "Simulate Speech")
                        }

                        Button(
                            onClick = {
                                if (voiceInputText.isNotBlank()) {
                                    viewModel.parseVoiceInputAndAddTask(voiceInputText) { title, client, rate, qty, type, category ->
                                        parsedTitle = title
                                        parsedClient = client
                                        parsedRate = rate
                                        parsedQty = qty
                                        parsedType = type
                                        parsedCategory = category
                                        showVoiceParsedResult = true
                                        showVoiceDialog = false
                                    }
                                }
                            }
                        ) {
                            if (viewModel.voiceParsingLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text(text = if (isMarathi) "विश्लेषण करा" else "Analyze with AI")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVoiceDialog = false }) {
                    Text(text = if (isMarathi) "रद्द करा" else "Cancel")
                }
            }
        )
    }

    // Voice parsed prefill results screen / dialog
    if (showVoiceParsedResult) {
        AlertDialog(
            onDismissRequest = { showVoiceParsedResult = false },
            title = { Text(text = if (isMarathi) "AI द्वारे विश्लेषित नोंद तपशील" else "AI Parsed Record Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(text = if (isMarathi) "कृपया खालील माहिती बरोबर आहे का तपासा:" else "Please verify the prefilled details below:")

                    OutlinedTextField(value = parsedTitle, onValueChange = { parsedTitle = it }, label = { Text("Task Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = parsedClient, onValueChange = { parsedClient = it }, label = { Text("Client Name") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = parsedRate.toString(), onValueChange = { parsedRate = it.toDoubleOrNull() ?: 0.0 }, label = { Text("Rate (₹)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = parsedQty.toString(), onValueChange = { parsedQty = it.toDoubleOrNull() ?: 1.0 }, label = { Text("Qty / Hours") }, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveWorkRecord(
                            title = parsedTitle,
                            clientName = parsedClient,
                            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                            workType = parsedType,
                            quantity = parsedQty,
                            rate = parsedRate,
                            paymentStatus = "PENDING",
                            notes = "AI Voice entered",
                            category = parsedCategory
                        )
                        showVoiceParsedResult = false
                        Toast.makeText(context, "Record added!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(text = if (isMarathi) "जतन करा" else "Save Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoiceParsedResult = false }) {
                    Text(text = if (isMarathi) "दुरुस्त करा" else "Discard")
                }
            }
        )
    }

    // Work Details & Interactive Payment History / Installments logging Dialog
    if (selectedRecordForDetails != null) {
        val record = selectedRecordForDetails!!
        val isPaid = record.paymentStatus == "PAID"
        val remainingAmt = record.totalAmount - record.paidAmount

        // Load logged payments
        val installmentsList = remember(record) {
            val type = Types.newParameterizedType(List::class.java, PaymentInstallment::class.java)
            try {
                Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter<List<PaymentInstallment>>(type).fromJson(record.paymentHistoryJson) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        AlertDialog(
            onDismissRequest = { selectedRecordForDetails = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = if (isMarathi) "काम हिशोब आणि पेमेंट तपशील" else "Work & Payment Details", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = { selectedRecordForDetails = null }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Task Overview Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = record.title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            Text(text = "👤 client: ${record.clientName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(text = "📅 date: ${record.date}", fontSize = 12.sp)
                            Text(text = "🏷️ category: ${record.category}", fontSize = 12.sp)
                            Text(text = "⚙️ formula: ${record.quantity} x ₹${record.rate} = ₹${record.totalAmount}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Progress payment bar
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = if (isMarathi) "पेमेंट प्रगती:" else "Payment Progress:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "₹${record.paidAmount} / ₹${record.totalAmount}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = (record.paidAmount / record.totalAmount).toFloat().coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = if (isPaid) Color(0xFF2ECC71) else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                    }

                    Divider()

                    // Payment Installment list
                    Text(text = if (isMarathi) "पेमेंट हप्ता इतिहास:" else "Payment History Logs:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    if (installmentsList.isEmpty()) {
                        Text(
                            text = if (isMarathi) "अद्याप कोणताही हप्ता जमा झालेला नाही." else "No installment payments logged yet.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    } else {
                        installmentsList.forEach { installment ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "₹${installment.amount}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2ECC71))
                                        Text(text = installment.date, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (installment.note.isNotEmpty()) {
                                            Text(text = installment.note, fontSize = 11.sp, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    if (installment.upiAccountLabel != null) {
                                        Box(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(text = installment.upiAccountLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!isPaid) {
                        Divider()
                        Text(text = if (isMarathi) "नवीन पेमेंट हप्ता नोंदवा:" else "Add Installment Payment:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                        OutlinedTextField(
                            value = installmentAmountInput,
                            onValueChange = { installmentAmountInput = it },
                            label = { Text(if (isMarathi) "हप्ता रक्कम (₹)" else "Installment Amount (₹)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = installmentNoteInput,
                            onValueChange = { installmentNoteInput = it },
                            label = { Text(if (isMarathi) "नोंद / शेरा (उदा. रोख किंवा बँक)" else "Note (e.g. Cash or GPay)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Select which UPI account received it
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            viewModel.upiAccounts.forEach { acc ->
                                val isSelected = selectedUpiAccountLabelForInstallment == acc.label
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedUpiAccountLabelForInstallment = if (isSelected) null else acc.label
                                    },
                                    label = { Text(acc.label) }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val amt = installmentAmountInput.toDoubleOrNull()
                                if (amt != null && amt > 0) {
                                    viewModel.addPaymentInstallment(record, amt, installmentNoteInput.trim(), selectedUpiAccountLabelForInstallment)
                                    Toast.makeText(context, if (isMarathi) "हप्ता जोडला!" else "Installment Payment Logged!", Toast.LENGTH_SHORT).show()
                                    selectedRecordForDetails = null
                                } else {
                                    Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = if (isMarathi) "हप्ता नोंदवा" else "Record Payment")
                        }
                    }

                    Divider()

                    // Additional utility action triggers
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                recordForReceipt = record
                                selectedRecordForDetails = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isMarathi) "पावती / पाकीट" else "Receipt", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                onEditRecord(record)
                                selectedRecordForDetails = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isMarathi) "बदल करा" else "Edit", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedRecordForDetails = null }) {
                    Text(text = if (isMarathi) "बंद करा" else "Close")
                }
            }
        )
    }

    // Digital Invoice & Payment Receipt Sharing Card Generator Dialog
    if (recordForReceipt != null) {
        val r = recordForReceipt!!
        AlertDialog(
            onDismissRequest = { recordForReceipt = null },
            title = { Text(text = if (isMarathi) "डिजिटल पावती आणि बीजक" else "Digital Receipt & Invoice Summary", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Receipt visual card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🧾 KAMACHA HISOB", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            Text(text = "------------------------------------------", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Bill To:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(text = r.clientName, fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Date:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(text = r.date, fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Category:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(text = r.category, fontSize = 12.sp)
                            }

                            Text(text = "------------------------------------------", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = r.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = "₹${r.totalAmount}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text(text = "(${r.quantity} x ₹${r.rate})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Start))

                            Text(text = "------------------------------------------", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Paid Amount:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2ECC71))
                                Text(text = "₹${r.paidAmount}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2ECC71))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Pending Amount:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFE74C3C))
                                Text(text = "₹${r.totalAmount - r.paidAmount}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFE74C3C))
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (r.paymentStatus == "PAID") "✅ FULLY PAID" else "⚠️ PARTIALLY PAID",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (r.paymentStatus == "PAID") Color(0xFF2ECC71) else Color(0xFFE67E22)
                            )
                        }
                    }

                    // Share button
                    Button(
                        onClick = {
                            val receiptText = """
                                🧾 *KAMACHA HISOB - WORK RECEIPT*
                                ---------------------------------
                                *Client Name:* ${r.clientName}
                                *Date:* ${r.date}
                                *Work Item:* ${r.title}
                                *Category:* ${r.category}
                                *Amount:* ₹${r.totalAmount} (${r.quantity} x ₹${r.rate})
                                ---------------------------------
                                *Total Paid:* ₹${r.paidAmount}
                                *Pending Balance:* ₹${r.totalAmount - r.paidAmount}
                                *Status:* ${r.paymentStatus}
                            """.trimIndent()
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, receiptText)
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Receipt"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (isMarathi) "पावती WhatsApp वर पाठवा" else "Share Receipt to WhatsApp")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { recordForReceipt = null }) {
                    Text(text = if (isMarathi) "मागे" else "Back")
                }
            }
        )
    }

    // UPI Payment QR Code Dialog
    if (showUpiQrDialog) {
        val upiUri = viewModel.getUpiPaymentUri(upiQrAmount, upiQrNote)
        val qrUrl = viewModel.getQrCodeUrl(upiUri)

        AlertDialog(
            onDismissRequest = { showUpiQrDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = LanguageHelper.getText(isMarathi, "upi_qr_title"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = { showUpiQrDialog = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "₹${"%,.2f".format(upiQrAmount)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = upiQrNote,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (qrUrl.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .size(220.dp)
                                .padding(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            coil.compose.AsyncImage(
                                model = qrUrl,
                                contentDescription = "UPI QR Code",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                             )
                        }
                    }

                    Text(
                        text = LanguageHelper.getText(isMarathi, "upi_qr_subtitle"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (upiUri.isNotEmpty()) {
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(
                                    android.content.Intent.EXTRA_TEXT,
                                    if (isMarathi) {
                                        "नमस्कार, कृपया माझ्या कामाचे पैसे (₹${"%,.0f".format(upiQrAmount)}) या UPI लिंकद्वारे पाठवा: $upiUri"
                                    } else {
                                        "Hello, please pay ₹${"%,.0f".format(upiQrAmount)} for my work using this UPI link: $upiUri"
                                    }
                                )
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share UPI Payment Link"))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(LanguageHelper.getText(isMarathi, "upi_share_request"))
                }
            }
        )
    }
}

@Composable
fun ListEmptyState(isMarathi: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.Assignment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = LanguageHelper.getText(isMarathi, "no_records"),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun WorkRecordItemCard(
    record: WorkRecord,
    isMarathi: Boolean,
    isUpiEnabled: Boolean = false,
    onShowUpiQr: (() -> Unit)? = null,
    onToggleStatus: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit
) {
    val isPaid = record.paymentStatus == "PAID"
    val isPartial = record.paymentStatus == "PARTIAL"
    val statusColor = when {
        isPaid -> Color(0xFF2ECC71)
        isPartial -> Color(0xFF3498DB)
        else -> Color(0xFFF39C12)
    }
    val statusBg = when {
        isPaid -> Color(0xFFE8F8F5)
        isPartial -> Color(0xFFEBF5FB)
        else -> Color(0xFFFEF9E7)
    }

    val workTypeFormatted = when (record.workType) {
        "HOURLY" -> LanguageHelper.getText(isMarathi, "hourly")
        "DAILY" -> LanguageHelper.getText(isMarathi, "daily")
        else -> LanguageHelper.getText(isMarathi, "fixed")
    }

    val catIcon = when (record.category) {
        "Farming" -> "🚜"
        "Construction" -> "🧱"
        "Plumbing" -> "🔧"
        "Delivery" -> "📦"
        else -> "⚙️"
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetails() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Date & Status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = record.date,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    onClick = onToggleStatus,
                    color = statusBg,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                isPaid -> if (isMarathi) "मिळाले" else "Paid"
                                isPartial -> if (isMarathi) "अंशत:" else "Partial"
                                else -> if (isMarathi) "बाकी" else "Pending"
                            },
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Task title & category
            Text(
                text = "$catIcon ${record.title}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = record.clientName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            Spacer(modifier = Modifier.height(6.dp))

            // Footer pricing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = workTypeFormatted,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (record.workType == "FIXED" || record.workType == "CONTRACT") {
                            "₹${record.rate}"
                        } else {
                            "₹${record.rate} × ${record.quantity}"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Balance remaining or total price tag
                Column(horizontalAlignment = Alignment.End) {
                    if (isPartial) {
                        Text(
                            text = "paid: ₹${record.paidAmount}",
                            fontSize = 10.sp,
                            color = Color(0xFF2ECC71),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "₹${"%,.0f".format(record.totalAmount)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPaid) Color(0xFF2ECC71) else MaterialTheme.colorScheme.primary
                        )

                        if (isUpiEnabled && !isPaid) {
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { onShowUpiQr?.invoke() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = "UPI QR",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (record.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(6.dp)
                ) {
                    Text(
                        text = record.notes,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

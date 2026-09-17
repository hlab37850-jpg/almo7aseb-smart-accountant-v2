package com.example.presentation.vouchers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entities.Account
import com.example.data.local.entities.Voucher
import com.example.presentation.viewmodels.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VouchersScreen(
    viewModel: MainViewModel,
    initialTab: String = "RECEIPT",
    onNewVoucherClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allVouchers by viewModel.allVouchers.collectAsStateWithLifecycle()
    val cashAndBanks by viewModel.cashAndBanks.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(initialTab) } // RECEIPT, PAYMENT, TREASURY
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)
    val currency = settings.baseCurrencySymbol

    val filteredVouchers = remember(allVouchers, selectedTab, searchQuery) {
        allVouchers.filter { v ->
            val matchType = if (selectedTab == "RECEIPT") v.type == "RECEIPT" else if (selectedTab == "PAYMENT") v.type == "PAYMENT" else true
            val matchQuery = v.voucherNumber.contains(searchQuery, ignoreCase = true) ||
                    v.accountName.contains(searchQuery, ignoreCase = true) ||
                    v.statement.contains(searchQuery, ignoreCase = true)
            matchType && matchQuery
        }
    }

    val totalReceipts = allVouchers.filter { it.type == "RECEIPT" }.sumOf { it.amount }
    val totalPayments = allVouchers.filter { it.type == "PAYMENT" }.sumOf { it.amount }

    Scaffold(
        floatingActionButton = {
            if (selectedTab != "TREASURY") {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(if (selectedTab == "RECEIPT") "سند قبض جديد" else "سند صرف جديد") },
                    containerColor = if (selectedTab == "RECEIPT") EmeraldSuccess else CrimsonDanger,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("new_voucher_fab")
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {
            // Summary Stats
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("إجمالي المقبوضات", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            "${String.format(Locale.US, "%.2f", totalReceipts)} $currency",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = EmeraldSuccess
                        )
                    }
                    Divider(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("إجمالي المدفوعات", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            "${String.format(Locale.US, "%.2f", totalPayments)} $currency",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = CrimsonDanger
                        )
                    }
                    Divider(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("صافي السيولة", fontSize = 11.sp, color = TextSecondary)
                        val net = totalReceipts - totalPayments
                        Text(
                            "${String.format(Locale.US, "%.2f", net)} $currency",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (net >= 0) EmeraldSuccess else CrimsonDanger
                        )
                    }
                }
            }

            // Tab Row
            TabRow(
                selectedTabIndex = when (selectedTab) { "RECEIPT" -> 0; "PAYMENT" -> 1; else -> 2 },
                containerColor = SurfaceLight,
                contentColor = BluePrimary,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == "RECEIPT",
                    onClick = { selectedTab = "RECEIPT" },
                    text = { Text("سندات القبض", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == "PAYMENT",
                    onClick = { selectedTab = "PAYMENT" },
                    text = { Text("سندات الصرف", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == "TREASURY",
                    onClick = { selectedTab = "TREASURY" },
                    text = { Text("الصناديق والسيولة", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedTab == "TREASURY") {
                // Treasury and Cash Boxes list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cashAndBanks) { cashAcc ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (cashAcc.type == "BANK") Icons.Default.AccountBalance else Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = BluePrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(cashAcc.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("رمز الحساب: ${cashAcc.accountCode}", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("الرصيد الفعلي", fontSize = 11.sp, color = TextSecondary)
                                    Text(
                                        "${String.format(Locale.US, "%.2f", cashAcc.currentBalance)} $currency",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (cashAcc.currentBalance >= 0) EmeraldSuccess else CrimsonDanger
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("بحث برقم السند أو اسم الحساب...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredVouchers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(54.dp),
                                tint = TextMuted
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("لا توجد سندات مسجلة", fontSize = 14.sp, color = TextSecondary)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredVouchers) { voucher ->
                            val isReceipt = voucher.type == "RECEIPT"
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                                elevation = CardDefaults.cardElevation(1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = voucher.voucherNumber,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isReceipt) EmeraldLight else CrimsonLight
                                            ) {
                                                Text(
                                                    text = if (isReceipt) "سند قبض" else "سند صرف",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isReceipt) EmeraldSuccess else CrimsonDanger
                                                )
                                            }
                                        }

                                        Text(
                                            text = "${String.format(Locale.US, "%.2f", voucher.amount)} $currency",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isReceipt) EmeraldSuccess else CrimsonDanger
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "${if (isReceipt) "من:" else "إلى:"} ${voucher.accountName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )

                                    if (voucher.statement.isNotEmpty()) {
                                        Text(
                                            text = "البيان: ${voucher.statement}",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "الصندوق: ${voucher.cashAccountName}",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                        Text(
                                            text = dateFormat.format(Date(voucher.date)),
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }

                                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        IconButton(onClick = {
                                            val file = viewModel.generateVoucherPdf(voucher)
                                            viewModel.sharePdf(file, context, "${if (isReceipt) "سند قبض" else "سند صرف"} ${voucher.voucherNumber}")
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "مشاركة PDF",
                                                tint = BluePrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Voucher Dialog
    if (showCreateDialog) {
        CreateVoucherDialog(
            defaultType = selectedTab,
            partyAccounts = if (selectedTab == "RECEIPT") (customers + suppliers) else (suppliers + expenses + customers),
            cashAccounts = cashAndBanks,
            onDismiss = { showCreateDialog = false },
            onSave = { voucher ->
                viewModel.saveVoucher(voucher) {
                    showCreateDialog = false
                }
            },
            generateVoucherNumber = { type ->
                viewModel.repository.generateNextVoucherNumber(type)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateVoucherDialog(
    defaultType: String,
    partyAccounts: List<Account>,
    cashAccounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (Voucher) -> Unit,
    generateVoucherNumber: suspend (String) -> String
) {
    var type by remember { mutableStateOf(defaultType) } // RECEIPT or PAYMENT
    var voucherNumber by remember { mutableStateOf("") }
    var selectedParty by remember { mutableStateOf(partyAccounts.firstOrNull()) }
    var selectedCash by remember { mutableStateOf(cashAccounts.firstOrNull()) }
    var amountInput by remember { mutableStateOf("") }
    var statementInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    LaunchedEffect(type) {
        voucherNumber = generateVoucherNumber(type)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (type == "RECEIPT") "إنشاء سند قبض مالي" else "إنشاء سند صرف مالي",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == "RECEIPT",
                        onClick = { type = "RECEIPT" },
                        label = { Text("سند قبض") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == "PAYMENT",
                        onClick = { type = "PAYMENT" },
                        label = { Text("سند صرف") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("رقم السند: $voucherNumber", fontSize = 12.sp, color = BluePrimary, fontWeight = FontWeight.Bold)

                // Party Account selector
                var partyExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = partyExpanded,
                    onExpandedChange = { partyExpanded = !partyExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedParty?.name ?: "اختر الحساب...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (type == "RECEIPT") "استلمنا من:" else "صرفنا إلى:") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = partyExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = partyExpanded,
                        onDismissRequest = { partyExpanded = false }
                    ) {
                        partyAccounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (${acc.accountCode})") },
                                onClick = {
                                    selectedParty = acc
                                    partyExpanded = false
                                }
                            )
                        }
                    }
                }

                // Cash/Bank account selector
                var cashExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = cashExpanded,
                    onExpandedChange = { cashExpanded = !cashExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCash?.name ?: "اختر الصندوق...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الصندوق / البنك") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cashExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = cashExpanded,
                        onDismissRequest = { cashExpanded = false }
                    ) {
                        cashAccounts.forEach { cAcc ->
                            DropdownMenuItem(
                                text = { Text(cAcc.name) },
                                onClick = {
                                    selectedCash = cAcc
                                    cashExpanded = false
                                }
                            )
                        }
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("المبلغ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                // Statement
                OutlinedTextField(
                    value = statementInput,
                    onValueChange = { statementInput = it },
                    label = { Text("وذلك عن (البيان)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountInput.toDoubleOrNull() ?: 0.0
                    if (amount > 0 && selectedParty != null && selectedCash != null) {
                        val v = Voucher(
                            voucherNumber = voucherNumber,
                            type = type,
                            accountId = selectedParty!!.id,
                            accountName = selectedParty!!.name,
                            cashAccountId = selectedCash!!.id,
                            cashAccountName = selectedCash!!.name,
                            amount = amount,
                            statement = statementInput.ifEmpty { if (type == "RECEIPT") "دفعة من الحساب" else "سداد دفعة" },
                            notes = notesInput
                        )
                        onSave(v)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == "RECEIPT") EmeraldSuccess else CrimsonDanger
                )
            ) {
                Text("تأكيد وحفظ السند")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

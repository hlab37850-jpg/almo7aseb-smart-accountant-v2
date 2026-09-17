package com.example.presentation.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entities.Account
import com.example.presentation.viewmodels.MainViewModel
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: MainViewModel,
    onAccountClick: (Long) -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allAccounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val cashAndBanks by viewModel.cashAndBanks.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf("CUSTOMER") } // CUSTOMER, SUPPLIER, CASH, EXPENSE
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val currency = settings.baseCurrencySymbol

    val currentList = when (selectedTab) {
        "CUSTOMER" -> customers
        "SUPPLIER" -> suppliers
        "CASH" -> cashAndBanks
        else -> expenses
    }

    val filteredList = remember(currentList, searchQuery) {
        currentList.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.accountCode.contains(searchQuery, ignoreCase = true) ||
                    it.phone.contains(searchQuery)
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("إضافة حساب جديد") },
                containerColor = BluePrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("new_account_fab")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = when (selectedTab) { "CUSTOMER" -> 0; "SUPPLIER" -> 1; "CASH" -> 2; else -> 3 },
                containerColor = SurfaceLight,
                contentColor = BluePrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Tab(
                    selected = selectedTab == "CUSTOMER",
                    onClick = { selectedTab = "CUSTOMER" },
                    text = { Text("العملاء (${customers.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == "SUPPLIER",
                    onClick = { selectedTab = "SUPPLIER" },
                    text = { Text("الموردون (${suppliers.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == "CASH",
                    onClick = { selectedTab = "CASH" },
                    text = { Text("الصناديق والبنوك", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == "EXPENSE",
                    onClick = { selectedTab = "EXPENSE" },
                    text = { Text("المصروفات", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                placeholder = { Text("بحث بالاسم، رقم الهاتف، أو الرمز...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Accounts List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PeopleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = TextMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا توجد حسابات مطابقة", fontSize = 14.sp, color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList) { acc ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAccountClick(acc.id) }
                                .testTag("account_card_${acc.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = acc.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = SurfaceVariantLight
                                        ) {
                                            Text(
                                                text = acc.accountCode,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    if (acc.phone.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "هاتف: ${acc.phone}",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    if (acc.creditLimit > 0) {
                                        Text(
                                            text = "سقف الائتمان: ${String.format(Locale.US, "%.0f", acc.creditLimit)} $currency",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("الرصيد الحالي", fontSize = 11.sp, color = TextSecondary)
                                    val isCustomer = acc.type == "CUSTOMER"
                                    val bal = acc.currentBalance
                                    val balanceColor = if (isCustomer) {
                                        if (bal > 0) BluePrimary else if (bal < 0) CrimsonDanger else TextSecondary
                                    } else {
                                        if (bal < 0) CrimsonDanger else if (bal > 0) EmeraldSuccess else TextSecondary
                                    }

                                    Text(
                                        text = "${String.format(Locale.US, "%.2f", Math.abs(bal))} $currency",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = balanceColor
                                    )

                                    Text(
                                        text = if (isCustomer) {
                                            if (bal > 0) "(عليه - مدين)" else if (bal < 0) "(له - دائن)" else "(خالص)"
                                        } else {
                                            if (bal < 0) "(له - مستحق للمورد)" else if (bal > 0) "(عليه)" else "(خالص)"
                                        },
                                        fontSize = 10.sp,
                                        color = balanceColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Account Dialog
    if (showAddDialog) {
        AddAccountDialog(
            defaultType = selectedTab,
            onDismiss = { showAddDialog = false },
            onSave = { newAccount ->
                viewModel.saveAccount(newAccount) {
                    showAddDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountDialog(
    defaultType: String,
    onDismiss: () -> Unit,
    onSave: (Account) -> Unit
) {
    var type by remember { mutableStateOf(defaultType) }
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var openingBalInput by remember { mutableStateOf("0") }
    var creditLimitInput by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة حساب مالي جديد", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Type selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = type == "CUSTOMER",
                        onClick = { type = "CUSTOMER" },
                        label = { Text("عميل") }
                    )
                    FilterChip(
                        selected = type == "SUPPLIER",
                        onClick = { type = "SUPPLIER" },
                        label = { Text("مورد") }
                    )
                    FilterChip(
                        selected = type == "EXPENSE",
                        onClick = { type = "EXPENSE" },
                        label = { Text("مصروف") }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الحساب / العميل / المورد *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("رمز الحساب (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف / الجوال") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = openingBalInput,
                        onValueChange = { openingBalInput = it },
                        label = { Text("الرصيد الافتتاحي") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    OutlinedTextField(
                        value = creditLimitInput,
                        onValueChange = { creditLimitInput = it },
                        label = { Text("سقف الائتمان") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty()) {
                        val opening = openingBalInput.toDoubleOrNull() ?: 0.0
                        val limit = creditLimitInput.toDoubleOrNull() ?: 0.0
                        val generatedCode = code.ifEmpty {
                            when (type) {
                                "CUSTOMER" -> "102${(100..999).random()}"
                                "SUPPLIER" -> "201${(100..999).random()}"
                                "EXPENSE" -> "601${(100..999).random()}"
                                else -> "101${(100..999).random()}"
                            }
                        }
                        onSave(
                            Account(
                                accountCode = generatedCode,
                                name = name,
                                type = type,
                                phone = phone,
                                creditLimit = limit,
                                openingBalance = opening,
                                currentBalance = opening,
                                notes = notes
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Text("حفظ الحساب")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

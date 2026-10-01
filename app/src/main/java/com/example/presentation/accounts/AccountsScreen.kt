package com.example.presentation.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import com.example.data.local.entities.AccountCategory
import com.example.presentation.viewmodels.MainViewModel
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(viewModel: MainViewModel, onAccountClick: (Long) -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allAccounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val cashAndBanks by viewModel.cashAndBanks.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val customCategories by viewModel.accountCategories.collectAsStateWithLifecycle()
    val tabs = remember(customCategories) {
        listOf(
            "CUSTOMER" to "العملاء",
            "TRUST" to "عملاء الثقة",
            "WHOLESALE" to "عملاء الجملة",
            "RETAIL" to "عملاء التجزئة",
            "OVERDUE" to "العملاء المتأخرون",
            "SUPPLIER" to "الموردون",
            "CASH" to "الصناديق والبنوك",
            "EXPENSE" to "المصروفات",
            "REVENUE" to "الإيرادات"
        ) + customCategories.map { "CATEGORY:" + it.id to it.name }
    }
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var categoryAccount by remember { mutableStateOf<Account?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    val currency = settings.baseCurrencySymbol

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
        Column(Modifier.fillMaxSize().padding(padding).background(BackgroundLight)) {
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage.coerceIn(0, (tabs.size - 1).coerceAtLeast(0)),
                containerColor = SurfaceLight,
                contentColor = BluePrimary,
                edgePadding = 16.dp,
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(tab.second, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }
                IconButton(onClick = { showCategoryDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة تصنيف", tint = BluePrimary)
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val key = tabs[page].first
                val categoryId = key.removePrefix("CATEGORY:").toLongOrNull()
                val customCategory = categoryId?.let { id -> customCategories.firstOrNull { it.id == id } }
                val systemCategoryName = when (key) {
                    "TRUST" -> "عملاء الثقة"
                    "WHOLESALE" -> "عملاء الجملة"
                    "RETAIL" -> "عملاء التجزئة"
                    else -> null
                }
                val systemCategory = systemCategoryName?.let { name -> customCategories.firstOrNull { it.name == name } }
                val list = when {
                    customCategory != null -> viewModel.accountsForCategory(customCategory.id).collectAsStateWithLifecycle(emptyList()).value
                    systemCategory != null -> viewModel.accountsForCategory(systemCategory.id).collectAsStateWithLifecycle(emptyList()).value
                    key == "CUSTOMER" -> customers
                    key == "SUPPLIER" -> suppliers
                    key == "CASH" -> cashAndBanks
                    key == "EXPENSE" -> expenses
                    key == "REVENUE" -> allAccounts.filter { it.type == "REVENUE" || it.type == "SALES" }
                    key == "OVERDUE" -> customers.filter { it.currentBalance > 0.0 }
                    else -> emptyList()
                }
                val filtered = list.filter {
                    it.name.contains(searchQuery, true) ||
                    it.accountCode.contains(searchQuery, true) ||
                    it.phone.contains(searchQuery)
                }
                Column(Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        placeholder = { Text("بحث بالاسم، رقم الهاتف، أو الرمز...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PeopleOutline, null, Modifier.size(54.dp), tint = TextMuted)
                                Spacer(Modifier.height(8.dp))
                                Text("لا توجد حسابات مطابقة", fontSize = 14.sp, color = TextSecondary)
                            }
                        }
                    } else {
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered, key = { it.id }) { acc ->
                                Card(
                                    Modifier.fillMaxWidth().clickable { onAccountClick(acc.id) }
                                        .testTag("account_card_" + acc.id),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                                    elevation = CardDefaults.cardElevation(1.dp)
                                ) {
                                    Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(acc.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                                Spacer(Modifier.width(8.dp))
                                                Surface(shape = RoundedCornerShape(4.dp), color = SurfaceVariantLight) {
                                                    Text(acc.accountCode, Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = TextSecondary)
                                                }
                                            }
                                            if (acc.phone.isNotEmpty()) {
                                                Spacer(Modifier.height(3.dp))
                                                Text("هاتف: " + acc.phone, fontSize = 12.sp, color = TextSecondary)
                                            }
                                        }
                                        IconButton(onClick = { categoryAccount = acc }) {
                                            Icon(Icons.Default.Label, "تصنيفات الحساب", tint = BluePrimary)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("الرصيد الحالي", fontSize = 11.sp, color = TextSecondary)
                                            val bal = acc.currentBalance
                                            val customer = acc.type == "CUSTOMER"
                                            val c = if (customer) {
                                                if (bal > 0) BluePrimary else if (bal < 0) CrimsonDanger else TextSecondary
                                            } else {
                                                if (bal < 0) CrimsonDanger else if (bal > 0) EmeraldSuccess else TextSecondary
                                            }
                                            Text(String.format(Locale.US, "%.2f", kotlin.math.abs(bal)) + " " + currency,
                                                fontWeight = FontWeight.Bold, fontSize = 14.sp, color = c)
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

    if (showAddDialog) {
        AddAccountDialog(
            defaultType = if (tabs.getOrNull(pagerState.currentPage)?.first == "SUPPLIER") "SUPPLIER"
                else if (tabs.getOrNull(pagerState.currentPage)?.first == "EXPENSE") "EXPENSE" else "CUSTOMER",
            onDismiss = { showAddDialog = false },
            onSave = { account -> viewModel.saveAccount(account) { showAddDialog = false } }
        )
    }

    categoryAccount?.let { account ->
        AccountCategoriesDialog(account, customCategories, viewModel) { categoryAccount = null }
    }

    if (showCategoryDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("إضافة تصنيف حسابات", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم التصنيف") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(onClick = { if (name.isNotBlank()) viewModel.createCategory(name.trim()) { showCategoryDialog = false } }, colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)) { Text("إضافة") }
            },
            dismissButton = { TextButton(onClick = { showCategoryDialog = false }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun AccountCategoriesDialog(
    account: Account,
    categories: List<AccountCategory>,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val linked by viewModel.categoriesForAccount(account.id).collectAsStateWithLifecycle(emptyList())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تصنيفات " + account.name, fontWeight = FontWeight.Bold) },
        text = {
            if (categories.isEmpty()) Text("لا توجد تصنيفات مخصصة حاليًا.")
            else Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { category ->
                    val checked = linked.any { it.id == category.id }
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            if (checked) viewModel.unlinkAccountFromCategory(account.id, category.id)
                            else viewModel.linkAccountToCategory(account.id, category.id)
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                if (it) viewModel.linkAccountToCategory(account.id, category.id)
                                else viewModel.unlinkAccountFromCategory(account.id, category.id)
                            }
                        )
                        Text(category.name)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("تم") } }
    )
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

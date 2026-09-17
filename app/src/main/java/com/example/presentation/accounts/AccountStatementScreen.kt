package com.example.presentation.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entities.Account
import com.example.data.local.entities.AccountTransaction
import com.example.presentation.components.AppTopBar
import com.example.presentation.viewmodels.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AccountStatementScreen(
    accountId: Long,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var account by remember { mutableStateOf<Account?>(null) }
    var transactions by remember { mutableStateOf<List<AccountTransaction>>(emptyList()) }
    var selectedPeriod by remember { mutableStateOf("ALL") } // ALL, TODAY, WEEK, MONTH

    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)
    val currency = settings.baseCurrencySymbol

    // Load account and transactions
    LaunchedEffect(accountId, selectedPeriod) {
        account = viewModel.repository.getAccountById(accountId)

        val cal = Calendar.getInstance()
        val toDate = cal.timeInMillis

        val fromDate: Long? = when (selectedPeriod) {
            "TODAY" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.timeInMillis
            }
            "WEEK" -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.timeInMillis
            }
            "MONTH" -> {
                cal.add(Calendar.MONTH, -1)
                cal.timeInMillis
            }
            else -> null
        }

        transactions = viewModel.repository.getAccountStatementDirect(accountId, fromDate, if (fromDate != null) toDate else null)
    }

    val totalDebit = transactions.sumOf { it.debit }
    val totalCredit = transactions.sumOf { it.credit }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "كشف حساب: ${account?.name ?: ""}",
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = {
                        if (account != null) {
                            val file = viewModel.generateStatementPdf(account!!, transactions)
                            viewModel.sharePdf(file, context, "كشف حساب ${account!!.name}")
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة PDF", tint = EmeraldSuccess)
                    }

                    IconButton(onClick = {
                        if (account != null) {
                            viewModel.shareWhatsAppStatement(account!!, context)
                        }
                    }) {
                        Icon(Icons.Default.Send, contentDescription = "إرسال عبر واتساب", tint = BluePrimary)
                    }
                }
            )
        }
    ) { padding ->
        if (account == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val acc = account!!
            val isCustomer = acc.type == "CUSTOMER"

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(BackgroundLight)
                    .padding(horizontal = 16.dp)
            ) {
                // Header Details Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(acc.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                Text("رمز الحساب: ${acc.accountCode} | هاتف: ${acc.phone.ifEmpty { "غير محدد" }}", fontSize = 12.sp, color = TextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("الرصيد النهائي", fontSize = 11.sp, color = TextSecondary)
                                Text(
                                    "${String.format(Locale.US, "%.2f", Math.abs(acc.currentBalance))} $currency",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = if (acc.currentBalance > 0) (if (isCustomer) BluePrimary else EmeraldSuccess) else if (acc.currentBalance < 0) CrimsonDanger else TextSecondary
                                )
                                Text(
                                    text = if (isCustomer) (if (acc.currentBalance > 0) "مدين (عليه)" else "دائن (له)") else (if (acc.currentBalance < 0) "دائن (مستحق له)" else "مدين"),
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 6.dp))

                        // Period Filters
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = selectedPeriod == "ALL", onClick = { selectedPeriod = "ALL" }, label = { Text("الكل") })
                            FilterChip(selected = selectedPeriod == "TODAY", onClick = { selectedPeriod = "TODAY" }, label = { Text("اليوم") })
                            FilterChip(selected = selectedPeriod == "WEEK", onClick = { selectedPeriod = "WEEK" }, label = { Text("آخر أسبوع") })
                            FilterChip(selected = selectedPeriod == "MONTH", onClick = { selectedPeriod = "MONTH" }, label = { Text("آخر شهر") })
                        }
                    }
                }

                // Totals Bar
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariantLight)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("إجمالي المدين (+)", fontSize = 11.sp, color = TextSecondary)
                            Text("${String.format(Locale.US, "%.2f", totalDebit)} $currency", fontWeight = FontWeight.Bold, color = BluePrimary, fontSize = 13.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("إجمالي الدائن (-)", fontSize = 11.sp, color = TextSecondary)
                            Text("${String.format(Locale.US, "%.2f", totalCredit)} $currency", fontWeight = FontWeight.Bold, color = CrimsonDanger, fontSize = 13.sp)
                        }
                    }
                }

                // Ledger Table
                if (transactions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد حركات مسجلة خلال الفترة المحددة", color = TextSecondary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(transactions) { tx ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                                elevation = CardDefaults.cardElevation(0.5.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(tx.documentNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BluePrimary)
                                        Text(dateFormat.format(Date(tx.date)), fontSize = 11.sp, color = TextMuted)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(tx.statement, fontSize = 12.sp, color = TextPrimary)

                                    Divider(modifier = Modifier.padding(vertical = 6.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        if (tx.debit > 0) {
                                            Text("مدين: +${String.format(Locale.US, "%.2f", tx.debit)} $currency", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                                        }
                                        if (tx.credit > 0) {
                                            Text("دائن: -${String.format(Locale.US, "%.2f", tx.credit)} $currency", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CrimsonDanger)
                                        }
                                        Text("الرصيد: ${String.format(Locale.US, "%.2f", tx.balanceAfter)} $currency", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

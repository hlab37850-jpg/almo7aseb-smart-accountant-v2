package com.example.presentation.invoices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentation.components.ThermalReceiptDialog
import com.example.presentation.viewmodels.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    viewModel: MainViewModel,
    onNavigateToNewPurchase: () -> Unit,
    onNavigateToNewReturn: (Long) -> Unit,
    onViewInvoiceDetails: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val purchaseInvoices by viewModel.purchaseInvoices.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var thermalDialogText by remember { mutableStateOf<String?>(null) }

    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)
    val currency = settings.baseCurrencySymbol

    val filteredInvoices = remember(purchaseInvoices, searchQuery, selectedFilter) {
        purchaseInvoices.filter { inv ->
            val matchFilter = when (selectedFilter) {
                "PURCHASE" -> inv.invoiceType == "PURCHASE"
                "RETURN" -> inv.invoiceType == "PURCHASE_RETURN"
                else -> true
            }
            val matchQuery = inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    inv.accountName.contains(searchQuery, ignoreCase = true)
            matchFilter && matchQuery
        }
    }

    val totalPurchases = purchaseInvoices.filter { it.invoiceType == "PURCHASE" }.sumOf { it.grandTotal }
    val totalReturns = purchaseInvoices.filter { it.invoiceType == "PURCHASE_RETURN" }.sumOf { it.grandTotal }
    val netPurchases = totalPurchases - totalReturns

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToNewPurchase,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("فاتورة شراء جديدة", fontWeight = FontWeight.Bold) },
                containerColor = Color(0xFF7C3AED),
                contentColor = Color.White,
                modifier = Modifier.testTag("new_purchase_fab")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {
            // Summary Card
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
                        Text("إجمالي المشتريات", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            "${String.format(Locale.US, "%.2f", totalPurchases)} $currency",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF7C3AED)
                        )
                    }
                    Divider(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("مردودات المشتريات", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            "${String.format(Locale.US, "%.2f", totalReturns)} $currency",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = AmberWarning
                        )
                    }
                    Divider(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("صافي المشتريات", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            "${String.format(Locale.US, "%.2f", netPurchases)} $currency",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("بحث برقم الفاتورة أو اسم المورد...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("الكل (${purchaseInvoices.size})") }
                )
                FilterChip(
                    selected = selectedFilter == "PURCHASE",
                    onClick = { selectedFilter = "PURCHASE" },
                    label = { Text("مشتريات") }
                )
                FilterChip(
                    selected = selectedFilter == "RETURN",
                    onClick = { selectedFilter = "RETURN" },
                    label = { Text("مردودات") }
                )
            }

            // Invoices List
            if (filteredInvoices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = TextMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "لا توجد فواتير مشتريات مطابقة",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredInvoices) { inv ->
                        InvoiceItemCard(
                            invoice = inv,
                            currency = currency,
                            dateFormat = dateFormat,
                            onCardClick = { onViewInvoiceDetails(inv.id) },
                            onPrintClick = {
                                coroutineScope.launch {
                                    val items = viewModel.repository.getInvoiceItemsDirect(inv.id)
                                    val preview = viewModel.getThermalPreview(inv, items)
                                    thermalDialogText = preview
                                }
                            },
                            onPdfClick = {
                                coroutineScope.launch {
                                    val items = viewModel.repository.getInvoiceItemsDirect(inv.id)
                                    val file = viewModel.generateInvoicePdf(inv, items)
                                    viewModel.sharePdf(file, context, "فاتورة مشتريات ${inv.invoiceNumber}")
                                }
                            },
                            onCreateReturn = {
                                onNavigateToNewReturn(inv.id)
                            }
                        )
                    }
                }
            }
        }
    }

    if (thermalDialogText != null) {
        ThermalReceiptDialog(
            receiptText = thermalDialogText!!,
            onDismiss = { thermalDialogText = null },
            onPrint = {
                viewModel.postMessage("تم إرسال أمر الطباعة إلى الطابعة")
                thermalDialogText = null
            },
            onShare = {
                val sendIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, thermalDialogText)
                    type = "text/plain"
                }
                context.startActivity(android.content.Intent.createChooser(sendIntent, "مشاركة الإيصال"))
            }
        )
    }
}

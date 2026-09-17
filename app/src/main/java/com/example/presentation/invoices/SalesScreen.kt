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
import com.example.data.local.entities.Invoice
import com.example.presentation.components.ThermalReceiptDialog
import com.example.presentation.viewmodels.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    viewModel: MainViewModel,
    onNavigateToNewSale: () -> Unit,
    onNavigateToNewReturn: (Long) -> Unit,
    onViewInvoiceDetails: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val salesInvoices by viewModel.salesInvoices.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, SALE, RETURN

    var thermalDialogText by remember { mutableStateOf<String?>(null) }

    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)
    val currency = settings.baseCurrencySymbol

    val filteredInvoices = remember(salesInvoices, searchQuery, selectedFilter) {
        salesInvoices.filter { inv ->
            val matchFilter = when (selectedFilter) {
                "SALE" -> inv.invoiceType == "SALE"
                "RETURN" -> inv.invoiceType == "SALE_RETURN"
                else -> true
            }
            val matchQuery = inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    inv.accountName.contains(searchQuery, ignoreCase = true)
            matchFilter && matchQuery
        }
    }

    val totalSales = salesInvoices.filter { it.invoiceType == "SALE" }.sumOf { it.grandTotal }
    val totalReturns = salesInvoices.filter { it.invoiceType == "SALE_RETURN" }.sumOf { it.grandTotal }
    val netSales = totalSales - totalReturns

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToNewSale,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("فاتورة مبيعات جديدة", fontWeight = FontWeight.Bold) },
                containerColor = BluePrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("new_sale_fab")
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
                        Text("إجمالي المبيعات", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            "${String.format(Locale.US, "%.2f", totalSales)} $currency",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BluePrimary
                        )
                    }
                    Divider(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("المرتجعات", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            "${String.format(Locale.US, "%.2f", totalReturns)} $currency",
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
                        Text("صافي المبيعات", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            "${String.format(Locale.US, "%.2f", netSales)} $currency",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = EmeraldSuccess
                        )
                    }
                }
            }

            // Search Bar & Filters
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("بحث برقم الفاتورة أو اسم العميل...") },
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
                    label = { Text("الكل (${salesInvoices.size})") }
                )
                FilterChip(
                    selected = selectedFilter == "SALE",
                    onClick = { selectedFilter = "SALE" },
                    label = { Text("مبيعات") }
                )
                FilterChip(
                    selected = selectedFilter == "RETURN",
                    onClick = { selectedFilter = "RETURN" },
                    label = { Text("مرتجعات") }
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
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = TextMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "لا توجد فواتير مبيعات مطابقة",
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
                                    viewModel.sharePdf(file, context, "فاتورة مبيعات ${inv.invoiceNumber}")
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

    // Thermal Receipt Dialog
    if (thermalDialogText != null) {
        ThermalReceiptDialog(
            receiptText = thermalDialogText!!,
            onDismiss = { thermalDialogText = null },
            onPrint = {
                viewModel.postMessage("تم إرسال أمر الطباعة إلى الطابعة المحددة")
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

@Composable
fun InvoiceItemCard(
    invoice: Invoice,
    currency: String,
    dateFormat: SimpleDateFormat,
    onCardClick: () -> Unit,
    onPrintClick: () -> Unit,
    onPdfClick: () -> Unit,
    onCreateReturn: () -> Unit
) {
    val isReturn = invoice.invoiceType == "SALE_RETURN"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("invoice_card_${invoice.invoiceNumber}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = invoice.invoiceNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isReturn) CrimsonLight else BlueLight
                    ) {
                        Text(
                            text = if (isReturn) "مرتجع" else if (invoice.paymentType == "CASH") "نقدي" else "آجل",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isReturn) CrimsonDanger else BluePrimary
                        )
                    }
                }

                Text(
                    text = "${String.format(Locale.US, "%.2f", invoice.grandTotal)} $currency",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isReturn) CrimsonDanger else BluePrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "العميل: ${invoice.accountName}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = dateFormat.format(Date(invoice.date)),
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            if (invoice.remainingAmount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "المدفوع: ${String.format(Locale.US, "%.2f", invoice.paidAmount)} $currency",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "المتبقي: ${String.format(Locale.US, "%.2f", invoice.remainingAmount)} $currency",
                        fontSize = 11.sp,
                        color = CrimsonDanger,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isReturn) {
                    TextButton(onClick = onCreateReturn) {
                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("عمل مرتجع", fontSize = 11.sp, color = CrimsonDanger)
                    }
                }

                IconButton(onClick = onPrintClick) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "طباعة حرارية",
                        tint = BluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onPdfClick) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "مشاركة PDF",
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

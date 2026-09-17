package com.example.presentation.invoices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.data.local.entities.Invoice
import com.example.data.local.entities.InvoiceItem
import com.example.presentation.components.AppTopBar
import com.example.presentation.components.ThermalReceiptDialog
import com.example.presentation.viewmodels.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InvoiceDetailsScreen(
    invoiceId: Long,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToNewReturn: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var invoice by remember { mutableStateOf<Invoice?>(null) }
    var items by remember { mutableStateOf<List<InvoiceItem>>(emptyList()) }
    var thermalDialogText by remember { mutableStateOf<String?>(null) }

    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
    val currency = settings.baseCurrencySymbol

    LaunchedEffect(invoiceId) {
        invoice = viewModel.repository.getInvoiceById(invoiceId)
        items = viewModel.repository.getInvoiceItemsDirect(invoiceId)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "تفاصيل الفاتورة ${invoice?.invoiceNumber ?: ""}",
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = {
                        if (invoice != null) {
                            val file = viewModel.generateInvoicePdf(invoice!!, items)
                            viewModel.sharePdf(file, context, "فاتورة ${invoice!!.invoiceNumber}")
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة PDF", tint = EmeraldSuccess)
                    }

                    IconButton(onClick = {
                        if (invoice != null) {
                            thermalDialogText = viewModel.getThermalPreview(invoice!!, items)
                        }
                    }) {
                        Icon(Icons.Default.Print, contentDescription = "طباعة حرارية", tint = BluePrimary)
                    }
                }
            )
        }
    ) { padding ->
        if (invoice == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val inv = invoice!!
            val isReturn = inv.invoiceType.contains("RETURN")

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(BackgroundLight)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = inv.invoiceNumber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isReturn) CrimsonLight else BlueLight
                                ) {
                                    Text(
                                        text = if (isReturn) "مرتجع" else if (inv.paymentType == "CASH") "نقدي" else "آجل",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isReturn) CrimsonDanger else BluePrimary
                                    )
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("الطرف الثاني (العميل/المورد):", fontSize = 12.sp, color = TextSecondary)
                                Text(inv.accountName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("التاريخ والوقت:", fontSize = 12.sp, color = TextSecondary)
                                Text(dateFormat.format(Date(inv.date)), fontSize = 12.sp, color = TextPrimary)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("المستخدم المسؤول:", fontSize = 12.sp, color = TextSecondary)
                                Text(inv.createdBy, fontSize = 12.sp, color = TextPrimary)
                            }

                            if (inv.notes.isNotEmpty()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("الملاحظات:", fontSize = 12.sp, color = TextSecondary)
                                    Text(inv.notes, fontSize = 12.sp, color = TextPrimary)
                                }
                            }
                        }
                    }
                }

                item {
                    Text("أصناف الفاتورة (${items.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                }

                itemsIndexed(items) { index, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        elevation = CardDefaults.cardElevation(0.5.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${index + 1}. ${item.productName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    "${String.format(Locale.US, "%.1f", item.quantity)} ${item.unitName} × ${String.format(Locale.US, "%.2f", item.unitPrice)} $currency",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                "${String.format(Locale.US, "%.2f", item.total)} $currency",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BluePrimary
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("المجموع الفرعي:", fontSize = 13.sp, color = TextSecondary)
                                Text("${String.format(Locale.US, "%.2f", inv.subtotal)} $currency", fontSize = 13.sp)
                            }
                            if (inv.discount > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("الخصم:", fontSize = 13.sp, color = TextSecondary)
                                    Text("-${String.format(Locale.US, "%.2f", inv.discount)} $currency", fontSize = 13.sp, color = CrimsonDanger)
                                }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("الصافي المستحق:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    "${String.format(Locale.US, "%.2f", inv.grandTotal)} $currency",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = BluePrimary
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("المدفوع:", fontSize = 13.sp, color = TextSecondary)
                                Text("${String.format(Locale.US, "%.2f", inv.paidAmount)} $currency", fontSize = 13.sp, color = EmeraldSuccess)
                            }
                            if (inv.remainingAmount > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("المتبقي الآجل:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CrimsonDanger)
                                    Text("${String.format(Locale.US, "%.2f", inv.remainingAmount)} $currency", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CrimsonDanger)
                                }
                            }
                        }
                    }
                }

                if (!isReturn && (inv.invoiceType == "SALE" || inv.invoiceType == "PURCHASE")) {
                    item {
                        OutlinedButton(
                            onClick = { onNavigateToNewReturn(inv.id) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = null, tint = CrimsonDanger)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إنشاء مرتجع مقابل هذه الفاتورة", color = CrimsonDanger, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (thermalDialogText != null) {
        ThermalReceiptDialog(
            receiptText = thermalDialogText!!,
            onDismiss = { thermalDialogText = null },
            onPrint = {
                viewModel.postMessage("تم إرسال أمر الطباعة")
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

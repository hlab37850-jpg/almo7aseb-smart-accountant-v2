package com.example.presentation.invoices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.data.local.entities.*
import com.example.presentation.components.AppTopBar
import com.example.presentation.viewmodels.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewInvoiceScreen(
    invoiceType: String, // SALE, PURCHASE, SALE_RETURN, PURCHASE_RETURN
    originalInvoiceId: Long? = null,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onInvoiceCreated: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val cashAndBanks by viewModel.cashAndBanks.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()

    val isSaleFlow = invoiceType == "SALE" || invoiceType == "SALE_RETURN"
    val isReturn = invoiceType == "SALE_RETURN" || invoiceType == "PURCHASE_RETURN"

    // Accounts list based on type
    val partyAccounts = if (isSaleFlow) customers else suppliers

    var invoiceNumber by remember { mutableStateOf("") }
    var selectedPartyAccount by remember { mutableStateOf<Account?>(null) }
    var selectedCashAccount by remember { mutableStateOf<Account?>(null) }
    var paymentType by remember { mutableStateOf("CASH") } // CASH, CREDIT, CARD
    var notes by remember { mutableStateOf("") }

    // Invoice items state
    val items = remember { mutableStateListOf<InvoiceItem>() }

    // Discount & Paid
    var discountInput by remember { mutableStateOf("0") }
    var paidInput by remember { mutableStateOf("") }

    // Dialog state for selecting product
    var showProductPicker by remember { mutableStateOf(false) }
    var barcodeInput by remember { mutableStateOf("") }

    val currency = settings.baseCurrencySymbol

    // Auto-generate invoice number on launch
    LaunchedEffect(Unit) {
        val nextNumber = viewModel.repository.generateNextInvoiceNumber(invoiceType)
        invoiceNumber = nextNumber
        if (originalInvoiceId != null && originalInvoiceId > 0) {
            val orig = viewModel.repository.getInvoiceById(originalInvoiceId)
            if (orig != null) {
                notes = "مرتجع مقابل الفاتورة الأصلية رقم ${orig.invoiceNumber}"
                val origItems = viewModel.repository.getInvoiceItemsDirect(originalInvoiceId)
                items.clear()
                items.addAll(origItems.map { it.copy(id = 0L, invoiceId = 0L) })
            }
        }
    }

    // Default select first cash account & party account
    LaunchedEffect(cashAndBanks, partyAccounts) {
        if (selectedCashAccount == null && cashAndBanks.isNotEmpty()) {
            selectedCashAccount = cashAndBanks.first()
        }
        if (selectedPartyAccount == null && partyAccounts.isNotEmpty()) {
            selectedPartyAccount = partyAccounts.first()
        }
    }

    // Totals calculation
    val subtotal = items.sumOf { it.total }
    val discount = discountInput.toDoubleOrNull() ?: 0.0
    val grandTotal = maxOf(0.0, subtotal - discount)
    val paidAmount = if (paymentType == "CASH") grandTotal else (paidInput.toDoubleOrNull() ?: 0.0)
    val remainingAmount = maxOf(0.0, grandTotal - paidAmount)

    val title = when (invoiceType) {
        "SALE" -> "فاتورة مبيعات جديدة"
        "PURCHASE" -> "فاتورة مشتريات جديدة"
        "SALE_RETURN" -> "مرتجع مبيعات"
        else -> "مرتجع مشتريات"
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = title,
                onBackClick = onNavigateBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Invoice Metadata Box
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "رقم الفاتورة: $invoiceNumber",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = BluePrimary
                            )
                            Text(
                                text = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date()),
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        // Party Selector Dropdown
                        var partyExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = partyExpanded,
                            onExpandedChange = { partyExpanded = !partyExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedPartyAccount?.name ?: "اختر الحساب...",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(if (isSaleFlow) "العميل" else "المورد") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = partyExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = partyExpanded,
                                onDismissRequest = { partyExpanded = false }
                            ) {
                                partyAccounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text("${acc.name} (${acc.accountCode})") },
                                        onClick = {
                                            selectedPartyAccount = acc
                                            partyExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Payment Type Selector
                        Text("طريقة الدفع:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = paymentType == "CASH",
                                onClick = { paymentType = "CASH" },
                                label = { Text("نقداً") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = paymentType == "CREDIT",
                                onClick = { paymentType = "CREDIT" },
                                label = { Text("آجل (ذمم)") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = paymentType == "CARD",
                                onClick = { paymentType = "CARD" },
                                label = { Text("شبكة / بنك") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Cash/Bank account selector if paying cash or card
                        if (paymentType != "CREDIT") {
                            var cashExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = cashExpanded,
                                onExpandedChange = { cashExpanded = !cashExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedCashAccount?.name ?: "الصندوق الرئيسي",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("الصندوق / الحساب المالي") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cashExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = cashExpanded,
                                    onDismissRequest = { cashExpanded = false }
                                ) {
                                    cashAndBanks.forEach { acc ->
                                        DropdownMenuItem(
                                            text = { Text(acc.name) },
                                            onClick = {
                                                selectedCashAccount = acc
                                                cashExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Barcode Quick Add & Add Product Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = barcodeInput,
                        onValueChange = { barcodeInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("إدخال باركود الصنف...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = {
                                if (barcodeInput.isNotEmpty()) {
                                    coroutineScope.launch {
                                        val p = viewModel.repository.getProductByBarcode(barcodeInput)
                                        if (p != null) {
                                            val price = if (invoiceType == "SALE" || invoiceType == "SALE_RETURN") p.salePrice else p.purchasePrice
                                            items.add(
                                                InvoiceItem(
                                                    invoiceId = 0L,
                                                    productId = p.id,
                                                    productName = p.name,
                                                    unitName = p.baseUnitName,
                                                    conversionFactor = 1.0,
                                                    quantity = 1.0,
                                                    baseQuantity = 1.0,
                                                    unitPrice = price,
                                                    costPrice = p.purchasePrice,
                                                    total = price
                                                )
                                            )
                                            barcodeInput = ""
                                        } else {
                                            viewModel.postMessage("الباركود غير مسجل في النظام")
                                        }
                                    }
                                }
                            }) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "بحث باركود", tint = BluePrimary)
                            }
                        }
                    )

                    Button(
                        onClick = { showProductPicker = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier.testTag("add_product_button")
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة صنف")
                    }
                }
            }

            // Items List
            if (items.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لم يتم إضافة أي صنف في الفاتورة بعد.\nاضغط 'إضافة صنف' أو امسح الباركود للبدء.",
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(items) { index, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.productName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                IconButton(
                                    onClick = { items.removeAt(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = CrimsonDanger)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Qty Controls
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    FilledIconButton(
                                        onClick = {
                                            if (item.quantity > 1) {
                                                val newQty = item.quantity - 1
                                                items[index] = item.copy(
                                                    quantity = newQty,
                                                    baseQuantity = newQty * item.conversionFactor,
                                                    total = newQty * item.unitPrice
                                                )
                                            }
                                        },
                                        modifier = Modifier.size(32.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = SurfaceVariantLight)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "إنقاص", modifier = Modifier.size(16.dp))
                                    }

                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", item.quantity)} ${item.unitName}",
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )

                                    FilledIconButton(
                                        onClick = {
                                            val newQty = item.quantity + 1
                                            items[index] = item.copy(
                                                quantity = newQty,
                                                baseQuantity = newQty * item.conversionFactor,
                                                total = newQty * item.unitPrice
                                            )
                                        },
                                        modifier = Modifier.size(32.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = SurfaceVariantLight)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "زيادة", modifier = Modifier.size(16.dp))
                                    }
                                }

                                // Price and Total
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${String.format(Locale.US, "%.2f", item.unitPrice)} $currency / وحدة",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.2f", item.total)} $currency",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = BluePrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Totals & Discount Card
            if (items.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        elevation = CardDefaults.cardElevation(1.5.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("المجموع قبل الخصم:", fontSize = 13.sp, color = TextSecondary)
                                Text(
                                    "${String.format(Locale.US, "%.2f", subtotal)} $currency",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("الخصم:", fontSize = 13.sp, color = TextSecondary)
                                OutlinedTextField(
                                    value = discountInput,
                                    onValueChange = { discountInput = it },
                                    modifier = Modifier.width(110.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                            }

                            Divider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الصافي النهائي:", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(
                                    "${String.format(Locale.US, "%.2f", grandTotal)} $currency",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary
                                )
                            }

                            if (paymentType == "CREDIT") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("المبلغ المدفوع (مقدم):", fontSize = 13.sp, color = TextSecondary)
                                    OutlinedTextField(
                                        value = paidInput,
                                        onValueChange = { paidInput = it },
                                        modifier = Modifier.width(110.dp),
                                        placeholder = { Text("0.0") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("المتبقي الآجل:", fontSize = 13.sp, color = TextSecondary)
                                    Text(
                                        "${String.format(Locale.US, "%.2f", remainingAmount)} $currency",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CrimsonDanger
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("ملاحظات / بيان الفاتورة") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }

                // Save Action Buttons
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (selectedPartyAccount == null) {
                                    viewModel.postMessage("يرجى اختيار العميل أو المورد أولاً")
                                    return@Button
                                }
                                val invoice = Invoice(
                                    invoiceNumber = invoiceNumber,
                                    invoiceType = invoiceType,
                                    accountId = selectedPartyAccount!!.id,
                                    accountName = selectedPartyAccount!!.name,
                                    cashAccountId = selectedCashAccount?.id ?: 1L,
                                    paymentType = paymentType,
                                    subtotal = subtotal,
                                    discount = discount,
                                    tax = 0.0,
                                    grandTotal = grandTotal,
                                    paidAmount = paidAmount,
                                    remainingAmount = remainingAmount,
                                    notes = notes,
                                    originalInvoiceId = originalInvoiceId
                                )
                                viewModel.saveInvoice(invoice, items) { id ->
                                    onInvoiceCreated(id)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_invoice_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حفظ وتثبيت الفاتورة", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Product Picker Dialog
    if (showProductPicker) {
        AlertDialog(
            onDismissRequest = { showProductPicker = false },
            title = { Text("اختر الصنف المراد إضافته") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(products) { _, prod ->
                        val price = if (isSaleFlow) prod.salePrice else prod.purchasePrice
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    items.add(
                                        InvoiceItem(
                                            invoiceId = 0L,
                                            productId = prod.id,
                                            productName = prod.name,
                                            unitName = prod.baseUnitName,
                                            conversionFactor = 1.0,
                                            quantity = 1.0,
                                            baseQuantity = 1.0,
                                            unitPrice = price,
                                            costPrice = prod.purchasePrice,
                                            total = price
                                        )
                                    )
                                    showProductPicker = false
                                },
                            colors = CardDefaults.cardColors(containerColor = SurfaceVariantLight)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        "المخزون الحالي: ${prod.currentStock} ${prod.baseUnitName}",
                                        fontSize = 11.sp,
                                        color = if (prod.currentStock <= prod.minStockAlert) CrimsonDanger else TextSecondary
                                    )
                                }
                                Text(
                                    "${String.format(Locale.US, "%.2f", price)} $currency",
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProductPicker = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

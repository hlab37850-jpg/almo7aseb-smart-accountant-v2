package com.example.presentation.products

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
import com.example.data.local.entities.Product
import com.example.data.local.entities.ProductUnit
import com.example.presentation.viewmodels.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: MainViewModel,
    onNavigateToAdjustment: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val stockTransactions by viewModel.stockTransactions.collectAsStateWithLifecycle()
    val inventoryValuation by viewModel.inventoryValuation.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf("ALL") } // ALL, LOW, MOVEMENTS
    var searchQuery by remember { mutableStateOf("") }
    var showAddProductDialog by remember { mutableStateOf(false) }

    val currency = settings.baseCurrencySymbol
    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)

    val displayedProducts = remember(products, lowStockProducts, selectedTab, searchQuery) {
        val list = if (selectedTab == "LOW") lowStockProducts else products
        list.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.code.contains(searchQuery, ignoreCase = true) ||
                    it.barcode.contains(searchQuery)
        }
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTab != "MOVEMENTS") {
                ExtendedFloatingActionButton(
                    onClick = { showAddProductDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("إضافة صنف جديد") },
                    containerColor = BluePrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_product_fab")
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
            // Valuation Summary Card & Stock Adjustment action
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
                        .padding(14.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("إجمالي قيمة المخزون (بالتكلفة)", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            "${String.format(Locale.US, "%.2f", inventoryValuation)} $currency",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = BluePrimary
                        )
                    }

                    Button(
                        onClick = onNavigateToAdjustment,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("جرد وتسوية", fontSize = 12.sp)
                    }
                }
            }

            // Tab Row
            TabRow(
                selectedTabIndex = when (selectedTab) { "ALL" -> 0; "LOW" -> 1; else -> 2 },
                containerColor = SurfaceLight,
                contentColor = BluePrimary,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == "ALL",
                    onClick = { selectedTab = "ALL" },
                    text = { Text("الأصناف (${products.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == "LOW",
                    onClick = { selectedTab = "LOW" },
                    text = { Text("منخفض المخزون (${lowStockProducts.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == "MOVEMENTS",
                    onClick = { selectedTab = "MOVEMENTS" },
                    text = { Text("سجل الحركات", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedTab == "MOVEMENTS") {
                // Stock Movements List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(stockTransactions) { tx ->
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tx.productName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(tx.notes, fontSize = 11.sp, color = TextSecondary)
                                    Text(dateFormat.format(Date(tx.date)), fontSize = 10.sp, color = TextMuted)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    val isPositive = tx.quantity > 0
                                    Text(
                                        "${if (isPositive) "+" else ""}${String.format(Locale.US, "%.1f", tx.quantity)} ${tx.unitName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isPositive) EmeraldSuccess else CrimsonDanger
                                    )
                                    Text("الرصيد بعدها: ${tx.balanceAfter}", fontSize = 11.sp, color = TextSecondary)
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
                    placeholder = { Text("بحث باسم الصنف، الباركود، الكود...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (displayedProducts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد أصناف مطابقة", fontSize = 14.sp, color = TextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(displayedProducts) { prod ->
                            val isLow = prod.currentStock <= prod.minStockAlert
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("product_card_${prod.id}"),
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
                                        Column {
                                            Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                            Text("الكود: ${prod.code} | الباركود: ${prod.barcode.ifEmpty { "لا يوجد" }}", fontSize = 11.sp, color = TextSecondary)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isLow) CrimsonLight else EmeraldLight
                                        ) {
                                            Text(
                                                text = "${String.format(Locale.US, "%.1f", prod.currentStock)} ${prod.baseUnitName}",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isLow) CrimsonDanger else EmeraldSuccess
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "سعر البيع: ${String.format(Locale.US, "%.2f", prod.salePrice)} $currency",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = BluePrimary
                                        )
                                        Text(
                                            text = "سعر الشراء: ${String.format(Locale.US, "%.2f", prod.purchasePrice)} $currency",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    if (isLow) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "تنبيه: الرصيد أقل من حد الطلب الأدنى (${prod.minStockAlert} ${prod.baseUnitName})",
                                            fontSize = 11.sp,
                                            color = AmberWarning
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

    // Add Product Dialog
    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { showAddProductDialog = false },
            onSave = { product, secondaryUnit ->
                val unitsList = if (secondaryUnit != null) listOf(secondaryUnit) else emptyList()
                viewModel.saveProduct(product, unitsList) {
                    showAddProductDialog = false
                }
            }
        )
    }
}

@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onSave: (Product, ProductUnit?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("عام") }
    var baseUnit by remember { mutableStateOf("حبة") }
    var purchasePriceInput by remember { mutableStateOf("") }
    var salePriceInput by remember { mutableStateOf("") }
    var openingStockInput by remember { mutableStateOf("0") }
    var minStockInput by remember { mutableStateOf("5") }

    // Multi-unit fields
    var addSecondaryUnit by remember { mutableStateOf(false) }
    var secondaryUnitName by remember { mutableStateOf("كرتون") }
    var conversionFactorInput by remember { mutableStateOf("12") }
    var secondarySalePriceInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة صنف ومخزون جديد", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الصنف *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("رمز الصنف") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text("الباركود") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("التصنيف") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = baseUnit,
                            onValueChange = { baseUnit = it },
                            label = { Text("الوحدة الأساسية") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = purchasePriceInput,
                            onValueChange = { purchasePriceInput = it },
                            label = { Text("سعر الشراء") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = salePriceInput,
                            onValueChange = { salePriceInput = it },
                            label = { Text("سعر البيع") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = openingStockInput,
                            onValueChange = { openingStockInput = it },
                            label = { Text("الرصيد الافتتاحي") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = minStockInput,
                            onValueChange = { minStockInput = it },
                            label = { Text("حد التنبيه الأدنى") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = addSecondaryUnit,
                            onCheckedChange = { addSecondaryUnit = it }
                        )
                        Text("إضافة وحدة بيع ثانوية (مثل كرتون)", fontSize = 12.sp)
                    }
                }
                if (addSecondaryUnit) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = secondaryUnitName,
                                onValueChange = { secondaryUnitName = it },
                                label = { Text("اسم الوحدة") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = conversionFactorInput,
                                onValueChange = { conversionFactorInput = it },
                                label = { Text("معامل التحويل") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = secondarySalePriceInput,
                            onValueChange = { secondarySalePriceInput = it },
                            label = { Text("سعر بيع الوحدة الثانوية") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty()) {
                        val purchasePrice = purchasePriceInput.toDoubleOrNull() ?: 0.0
                        val salePrice = salePriceInput.toDoubleOrNull() ?: 0.0
                        val openingStock = openingStockInput.toDoubleOrNull() ?: 0.0
                        val minStock = minStockInput.toDoubleOrNull() ?: 5.0
                        val genCode = code.ifEmpty { "PRD-${(100..999).random()}" }

                        val prod = Product(
                            code = genCode,
                            barcode = barcode,
                            name = name,
                            category = category,
                            baseUnitName = baseUnit,
                            purchasePrice = purchasePrice,
                            salePrice = salePrice,
                            minStockAlert = minStock,
                            currentStock = openingStock
                        )

                        val secUnit = if (addSecondaryUnit) {
                            val factor = conversionFactorInput.toDoubleOrNull() ?: 1.0
                            val secPrice = secondarySalePriceInput.toDoubleOrNull() ?: (salePrice * factor)
                            ProductUnit(
                                productId = 0L,
                                unitName = secondaryUnitName,
                                conversionFactor = factor,
                                salePrice = secPrice,
                                purchasePrice = purchasePrice * factor
                            )
                        } else null

                        onSave(prod, secUnit)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Text("حفظ الصنف")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

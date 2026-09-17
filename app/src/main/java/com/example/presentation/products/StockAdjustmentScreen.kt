package com.example.presentation.products

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entities.Product
import com.example.presentation.components.AppTopBar
import com.example.presentation.viewmodels.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustmentScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val adjustments by viewModel.stockAdjustments.collectAsStateWithLifecycle()

    var selectedProduct by remember { mutableStateOf<Product?>(products.firstOrNull()) }
    var actualStockInput by remember { mutableStateOf("") }
    var reasonInput by remember { mutableStateOf("تسوية جرد فعلي") }

    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)

    LaunchedEffect(products) {
        if (selectedProduct == null && products.isNotEmpty()) {
            selectedProduct = products.first()
        }
    }

    val systemStock = selectedProduct?.currentStock ?: 0.0
    val actualStock = actualStockInput.toDoubleOrNull() ?: systemStock
    val diff = actualStock - systemStock

    Scaffold(
        topBar = {
            AppTopBar(
                title = "الجرد والتسوية المخزنية",
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("تسجيل تسوية جردية لصنف", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)

                        // Product selector
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = selectedProduct?.name ?: "اختر الصنف...",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("الصنف المراد جرده") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                products.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text("${p.name} (الرصيد الدفتري: ${p.currentStock} ${p.baseUnitName})") },
                                        onClick = {
                                            selectedProduct = p
                                            actualStockInput = p.currentStock.toString()
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (selectedProduct != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الرصيد المسجل في النظام (الدفتري):", fontSize = 12.sp, color = TextSecondary)
                                Text(
                                    "${selectedProduct!!.currentStock} ${selectedProduct!!.baseUnitName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            OutlinedTextField(
                                value = actualStockInput,
                                onValueChange = { actualStockInput = it },
                                label = { Text("الكمية الفعلية الموجودة في المستودع *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )

                            // Difference Preview
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (diff == 0.0) SurfaceVariantLight else if (diff > 0) EmeraldLight else CrimsonLight,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("فرق الجرد (عجز / زيادة):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(
                                        text = "${if (diff > 0) "+$diff (زيادة مخزنية)" else if (diff < 0) "$diff (عجز مخزني)" else "مطابق تماماً"}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (diff > 0) EmeraldSuccess else if (diff < 0) CrimsonDanger else TextSecondary
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = reasonInput,
                                onValueChange = { reasonInput = it },
                                label = { Text("سبب التسوية / ملاحظات") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    val actual = actualStockInput.toDoubleOrNull()
                                    if (actual != null && selectedProduct != null) {
                                        viewModel.adjustStock(selectedProduct!!.id, actual, reasonInput) {
                                            actualStockInput = ""
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تأكيد واعتماد التسوية المخزنية")
                            }
                        }
                    }
                }
            }

            item {
                Text("سجل التسويات الجردية السابقة", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            }

            if (adjustments.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد تسويات جردية سابقة مسجلة", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                items(adjustments) { adj ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        elevation = CardDefaults.cardElevation(0.5.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(adj.productName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(dateFormat.format(Date(adj.date)), fontSize = 10.sp, color = TextMuted)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("السبب: ${adj.reason}", fontSize = 11.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("النظام: ${adj.systemStock} | الفعلي: ${adj.actualStock}", fontSize = 11.sp, color = TextSecondary)
                                val isPos = adj.difference > 0
                                Text(
                                    text = "الفرق: ${if (isPos) "+" else ""}${adj.difference}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isPos) EmeraldSuccess else CrimsonDanger
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

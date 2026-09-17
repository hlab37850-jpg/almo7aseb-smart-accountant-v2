package com.example.presentation.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.presentation.viewmodels.MainViewModel
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val totalSales by viewModel.totalSales.collectAsStateWithLifecycle()
    val totalPurchases by viewModel.totalPurchases.collectAsStateWithLifecycle()
    val totalCOGS by viewModel.totalCostOfGoodsSold.collectAsStateWithLifecycle()
    val expensesList by viewModel.expenses.collectAsStateWithLifecycle()
    val cashBalance by viewModel.cashBalance.collectAsStateWithLifecycle()
    val totalReceipts by viewModel.todayReceipts.collectAsStateWithLifecycle()
    val totalPayments by viewModel.todayPayments.collectAsStateWithLifecycle()
    val customerReceivables by viewModel.customerReceivables.collectAsStateWithLifecycle()
    val supplierPayables by viewModel.supplierPayables.collectAsStateWithLifecycle()
    val inventoryValuation by viewModel.inventoryValuation.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val lowStockCount by viewModel.lowStockCount.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0: Profit & Loss, 1: Cash Flow, 2: Inventory, 3: Debts

    val currency = settings.baseCurrencySymbol

    // Calculations
    val totalExpenses = expensesList.sumOf { Math.abs(it.currentBalance) }
    val effectiveCOGS = if (totalCOGS > 0) totalCOGS else (totalPurchases * 0.75) // estimated if purchases exist
    val grossProfit = totalSales - effectiveCOGS
    val netProfit = grossProfit - totalExpenses

    val inventoryAtSalePrice = products.sumOf { it.currentStock * it.salePrice }
    val expectedInventoryProfit = inventoryAtSalePrice - inventoryValuation

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfaceLight,
            contentColor = BluePrimary,
            edgePadding = 16.dp
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("الأرباح والخسائر", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("حركة السيولة والصندوق", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("تقييم المخزون", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("الديون والالتزامات", fontWeight = FontWeight.Bold) })
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Profit & Loss Report
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            elevation = CardDefaults.cardElevation(1.5.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("قائمة الدخل والأرباح (Income Statement)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Divider(modifier = Modifier.padding(vertical = 4.dp))

                                ReportRow("إجمالي إيرادات المبيعات", "${String.format(Locale.US, "%.2f", totalSales)} $currency", BluePrimary, true)
                                ReportRow("تكلفة البضاعة المباعة (COGS)", "-${String.format(Locale.US, "%.2f", effectiveCOGS)} $currency", CrimsonDanger, false)
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                ReportRow("مجمل الربح التجاري", "${String.format(Locale.US, "%.2f", grossProfit)} $currency", if (grossProfit >= 0) EmeraldSuccess else CrimsonDanger, true)

                                ReportRow("المصروفات العمومية والتشغيلية", "-${String.format(Locale.US, "%.2f", totalExpenses)} $currency", CrimsonDanger, false)
                                Divider(modifier = Modifier.padding(vertical = 6.dp))

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (netProfit >= 0) EmeraldLight else CrimsonLight,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("صافي الأرباح (Net Profit):", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(
                                            "${String.format(Locale.US, "%.2f", netProfit)} $currency",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = if (netProfit >= 0) EmeraldSuccess else CrimsonDanger
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Cash Flow Report
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            elevation = CardDefaults.cardElevation(1.5.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("حركة السيولة النقدية والصندوق", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Divider(modifier = Modifier.padding(vertical = 4.dp))

                                ReportRow("المقبوضات النقدية (سندات قبض)", "+${String.format(Locale.US, "%.2f", totalReceipts)} $currency", EmeraldSuccess, false)
                                ReportRow("المدفوعات والمصاريف (سندات صرف)", "-${String.format(Locale.US, "%.2f", totalPayments)} $currency", CrimsonDanger, false)

                                Divider(modifier = Modifier.padding(vertical = 6.dp))

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = BlueLight,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("الرصيد النقدي الفعلي المتاح:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            "${String.format(Locale.US, "%.2f", cashBalance)} $currency",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = BluePrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Inventory Valuation
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            elevation = CardDefaults.cardElevation(1.5.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("تقييم المخزون والبضاعة الحالية", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Divider(modifier = Modifier.padding(vertical = 4.dp))

                                ReportRow("إجمالي عدد الأصناف المعرفة", "${products.size} صنف", TextPrimary, false)
                                ReportRow("الأصناف التي وصلت حد الطلب", "$lowStockCount صنف", if (lowStockCount > 0) AmberWarning else TextSecondary, false)
                                ReportRow("قيمة المخزون بسعر التكلفة (الشراء)", "${String.format(Locale.US, "%.2f", inventoryValuation)} $currency", BluePrimary, true)
                                ReportRow("القيمة التقديرية بسعر البيع للمستهلك", "${String.format(Locale.US, "%.2f", inventoryAtSalePrice)} $currency", EmeraldSuccess, true)

                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                ReportRow("هامش الربح المخزني الكامن المتوقع", "${String.format(Locale.US, "%.2f", expectedInventoryProfit)} $currency", EmeraldSuccess, true)
                            }
                        }
                    }
                }
                3 -> {
                    // Debts & Liabilities
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            elevation = CardDefaults.cardElevation(1.5.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("موقف الديون والمستحقات والالتزامات", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Divider(modifier = Modifier.padding(vertical = 4.dp))

                                ReportRow("مستحقات على العملاء (ديون لنا)", "${String.format(Locale.US, "%.2f", customerReceivables)} $currency", BluePrimary, true)
                                ReportRow("مستحقات للموردين (التزامات علينا)", "${String.format(Locale.US, "%.2f", supplierPayables)} $currency", CrimsonDanger, true)

                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                val netWorkingBalance = customerReceivables - supplierPayables
                                ReportRow("صافي الذمم المتداولة", "${String.format(Locale.US, "%.2f", netWorkingBalance)} $currency", if (netWorkingBalance >= 0) EmeraldSuccess else CrimsonDanger, true)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportRow(title: String, value: String, valueColor: Color, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 13.sp, color = if (isBold) TextPrimary else TextSecondary, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontSize = 14.sp, color = valueColor, fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold)
    }
}

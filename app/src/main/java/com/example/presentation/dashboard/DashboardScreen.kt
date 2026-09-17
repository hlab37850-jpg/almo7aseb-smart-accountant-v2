package com.example.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentation.components.DashboardStatCard
import com.example.presentation.components.QuickActionCard
import com.example.presentation.viewmodels.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToSales: () -> Unit,
    onNavigateToPurchases: () -> Unit,
    onNavigateToVouchers: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToNewSale: () -> Unit,
    onNavigateToNewPurchase: () -> Unit,
    onNavigateToNewReceipt: () -> Unit,
    onNavigateToNewPayment: () -> Unit,
    onNavigateToStockAdjustment: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val todaySales by viewModel.todaySales.collectAsStateWithLifecycle()
    val todayPurchases by viewModel.todayPurchases.collectAsStateWithLifecycle()
    val todayReceipts by viewModel.todayReceipts.collectAsStateWithLifecycle()
    val todayPayments by viewModel.todayPayments.collectAsStateWithLifecycle()
    val cashBalance by viewModel.cashBalance.collectAsStateWithLifecycle()
    val customerReceivables by viewModel.customerReceivables.collectAsStateWithLifecycle()
    val supplierPayables by viewModel.supplierPayables.collectAsStateWithLifecycle()
    val lowStockCount by viewModel.lowStockCount.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()

    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
    val currency = settings.baseCurrencySymbol

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            // Welcome Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Navy900)
            ) {
                Row(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = settings.companyName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "المستخدم الحالي: ${settings.currentUsername} (${when (settings.currentUserRole) { "ADMIN" -> "مدير عام"; "ACCOUNTANT" -> "محاسب"; else -> "كاشير" }})",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Quick Actions Row
        item {
            Text(
                text = "العمليات السريعة",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = "فاتورة بيع",
                    icon = Icons.Default.ShoppingCart,
                    color = BluePrimary,
                    bgColor = BlueLight,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToNewSale
                )
                QuickActionCard(
                    title = "فاتورة شراء",
                    icon = Icons.Default.ShoppingBag,
                    color = Color(0xFF7C3AED),
                    bgColor = Color(0xFFF3E8FF),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToNewPurchase
                )
                QuickActionCard(
                    title = "سند قبض",
                    icon = Icons.Default.ArrowDownward,
                    color = EmeraldSuccess,
                    bgColor = EmeraldLight,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToNewReceipt
                )
                QuickActionCard(
                    title = "سند صرف",
                    icon = Icons.Default.ArrowUpward,
                    color = CrimsonDanger,
                    bgColor = CrimsonLight,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToNewPayment
                )
            }
        }

        // Daily Activity Section
        item {
            Text(
                text = "حركة اليوم والسيولة",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardStatCard(
                        title = "مبيعات اليوم",
                        value = "${String.format(Locale.US, "%.2f", todaySales)} $currency",
                        subtitle = "الفواتير النقدية والآجلة",
                        icon = Icons.Default.TrendingUp,
                        color = BluePrimary,
                        bgColor = BlueLight,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSales
                    )

                    DashboardStatCard(
                        title = "مشتريات اليوم",
                        value = "${String.format(Locale.US, "%.2f", todayPurchases)} $currency",
                        subtitle = "التوريدات اليومية",
                        icon = Icons.Default.Inventory,
                        color = Color(0xFF7C3AED),
                        bgColor = Color(0xFFF3E8FF),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToPurchases
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardStatCard(
                        title = "مقبوضات اليوم",
                        value = "${String.format(Locale.US, "%.2f", todayReceipts)} $currency",
                        subtitle = "سندات القبض",
                        icon = Icons.Default.AddCircle,
                        color = EmeraldSuccess,
                        bgColor = EmeraldLight,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToVouchers
                    )

                    DashboardStatCard(
                        title = "مدفوعات اليوم",
                        value = "${String.format(Locale.US, "%.2f", todayPayments)} $currency",
                        subtitle = "سندات الصرف والمصاريف",
                        icon = Icons.Default.RemoveCircle,
                        color = CrimsonDanger,
                        bgColor = CrimsonLight,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToVouchers
                    )
                }

                DashboardStatCard(
                    title = "رصيد الصناديق والبنوك المتاح",
                    value = "${String.format(Locale.US, "%.2f", cashBalance)} $currency",
                    subtitle = "السيولة النقدية الفعلية بعد كافة العمليات",
                    icon = Icons.Default.AccountBalanceWallet,
                    color = if (cashBalance >= 0) EmeraldSuccess else CrimsonDanger,
                    bgColor = if (cashBalance >= 0) EmeraldLight else CrimsonLight,
                    onClick = onNavigateToVouchers
                )
            }
        }

        // Receivables and Payables (الديون)
        item {
            Text(
                text = "موقف الديون والمستحقات",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardStatCard(
                    title = "مستحقات على العملاء",
                    value = "${String.format(Locale.US, "%.2f", customerReceivables)} $currency",
                    subtitle = "ديون لصالح المنشأة",
                    icon = Icons.Default.People,
                    color = BluePrimary,
                    bgColor = BlueLight,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAccounts
                )

                DashboardStatCard(
                    title = "مستحقات للموردين",
                    value = "${String.format(Locale.US, "%.2f", supplierPayables)} $currency",
                    subtitle = "التزامات على المنشأة",
                    icon = Icons.Default.LocalShipping,
                    color = AmberWarning,
                    bgColor = AmberLight,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAccounts
                )
            }
        }

        // Low stock alert banner
        if (lowStockCount > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToProducts() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberLight)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AmberWarning),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تنبيه: يوجد $lowStockCount أصناف قاربت على النفاد!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = AmberWarning
                            )
                            Text(
                                text = lowStockProducts.joinToString("، ") { it.name }.take(40) + "...",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = AmberWarning
                        )
                    }
                }
            }
        }

        // Recent Transactions Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخر الحركات المالية",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                TextButton(onClick = onNavigateToAccounts) {
                    Text("عرض الكل", color = BluePrimary, fontSize = 12.sp)
                }
            }
        }

        if (recentTransactions.isEmpty()) {
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
                            text = "لا توجد حركات مالية مسجلة حتى الآن",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(recentTransactions.take(8)) { tx ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isDebit = tx.debit > 0
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDebit) EmeraldLight else CrimsonLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDebit) Icons.Default.Add else Icons.Default.Remove,
                                contentDescription = null,
                                tint = if (isDebit) EmeraldSuccess else CrimsonDanger,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.accountName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "${tx.documentNumber} - ${tx.statement}",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                            Text(
                                text = dateFormat.format(Date(tx.date)),
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                        Text(
                            text = "${if (isDebit) "+" else "-"}${String.format(Locale.US, "%.2f", if (isDebit) tx.debit else tx.credit)} $currency",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isDebit) EmeraldSuccess else CrimsonDanger
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.presentation.accounts.AccountStatementScreen
import com.example.presentation.accounts.AccountsScreen
import com.example.presentation.components.AppTopBar
import com.example.presentation.dashboard.DashboardScreen
import com.example.presentation.invoices.InvoiceDetailsScreen
import com.example.presentation.invoices.NewInvoiceScreen
import com.example.presentation.invoices.PurchasesScreen
import com.example.presentation.invoices.SalesScreen
import com.example.presentation.navigation.Screen
import com.example.presentation.products.ProductsScreen
import com.example.presentation.products.StockAdjustmentScreen
import com.example.presentation.reports.ReportsScreen
import com.example.presentation.settings.SettingsScreen
import com.example.presentation.viewmodels.MainViewModel
import com.example.presentation.vouchers.VouchersScreen
import com.example.ui.theme.BlueLight
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SurfaceLight
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val snackbarHostState = remember { SnackbarHostState() }
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    // Listen to user messages
    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Determine if on top-level root screens
    val isRootScreen = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.Sales.route,
        Screen.Purchases.route,
        Screen.Vouchers.route,
        Screen.Accounts.route,
        Screen.Products.route,
        Screen.Reports.route,
        Screen.Settings.route
    )

    val currentTitle = when {
        currentRoute == Screen.Dashboard.route -> settings.companyName.ifEmpty { "المحاسب الذكي" }
        currentRoute == Screen.Sales.route -> "المبيعات والمرتجعات"
        currentRoute == Screen.Purchases.route -> "المشتريات والتوريدات"
        currentRoute == Screen.Vouchers.route -> "السندات والصناديق"
        currentRoute == Screen.Accounts.route -> "دليل الحسابات والعملاء"
        currentRoute == Screen.Products.route -> "المخزون والأصناف"
        currentRoute == Screen.Reports.route -> "التقارير المالية"
        currentRoute == Screen.Settings.route -> "إعدادات المنشأة"
        currentRoute?.startsWith("new_invoice") == true -> "فاتورة جديدة"
        currentRoute?.startsWith("invoice_details") == true -> "تفاصيل الفاتورة"
        currentRoute?.startsWith("account_statement") == true -> "كشف حساب"
        currentRoute == Screen.StockAdjustment.route -> "الجرد والتسوية"
        else -> "المحاسب الذكي"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isRootScreen) {
                AppTopBar(
                    title = currentTitle,
                    actions = {
                        // Quick access to Inventory, Reports and Settings
                        IconButton(
                            onClick = { navController.navigate(Screen.Products.route) },
                            modifier = Modifier.testTag("nav_products_icon")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = "المخزون والأصناف",
                                tint = if (currentRoute == Screen.Products.route) BluePrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = { navController.navigate(Screen.Reports.route) },
                            modifier = Modifier.testTag("nav_reports_icon")
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "التقارير",
                                tint = if (currentRoute == Screen.Reports.route) BluePrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = { navController.navigate(Screen.Settings.route) },
                            modifier = Modifier.testTag("nav_settings_icon")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "الإعدادات",
                                tint = if (currentRoute == Screen.Settings.route) BluePrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (isRootScreen) {
                NavigationBar(
                    containerColor = SurfaceLight,
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Dashboard.route,
                        onClick = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "الرئيسية") },
                        label = { Text("الرئيسية", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("bottom_nav_dashboard")
                    )

                    NavigationBarItem(
                        selected = currentRoute == Screen.Sales.route,
                        onClick = {
                            navController.navigate(Screen.Sales.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "المبيعات") },
                        label = { Text("المبيعات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("bottom_nav_sales")
                    )

                    NavigationBarItem(
                        selected = currentRoute == Screen.Purchases.route,
                        onClick = {
                            navController.navigate(Screen.Purchases.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "المشتريات") },
                        label = { Text("المشتريات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("bottom_nav_purchases")
                    )

                    NavigationBarItem(
                        selected = currentRoute == Screen.Vouchers.route,
                        onClick = {
                            navController.navigate(Screen.Vouchers.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Receipt, contentDescription = "السندات") },
                        label = { Text("السندات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("bottom_nav_vouchers")
                    )

                    NavigationBarItem(
                        selected = currentRoute == Screen.Accounts.route,
                        onClick = {
                            navController.navigate(Screen.Accounts.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.People, contentDescription = "الحسابات") },
                        label = { Text("الحسابات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("bottom_nav_accounts")
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(padding)
        ) {
            // Dashboard
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToSales = { navController.navigate(Screen.Sales.route) },
                    onNavigateToPurchases = { navController.navigate(Screen.Purchases.route) },
                    onNavigateToVouchers = { navController.navigate(Screen.Vouchers.route) },
                    onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) },
                    onNavigateToProducts = { navController.navigate(Screen.Products.route) },
                    onNavigateToNewSale = { navController.navigate("new_invoice/SALE?originalId=-1") },
                    onNavigateToNewPurchase = { navController.navigate("new_invoice/PURCHASE?originalId=-1") },
                    onNavigateToNewReceipt = { navController.navigate(Screen.Vouchers.route) },
                    onNavigateToNewPayment = { navController.navigate(Screen.Vouchers.route) },
                    onNavigateToStockAdjustment = { navController.navigate(Screen.StockAdjustment.route) }
                )
            }

            // Sales
            composable(Screen.Sales.route) {
                SalesScreen(
                    viewModel = viewModel,
                    onNavigateToNewSale = { navController.navigate("new_invoice/SALE?originalId=-1") },
                    onNavigateToNewReturn = { origId -> navController.navigate("new_invoice/SALE_RETURN?originalId=$origId") },
                    onViewInvoiceDetails = { id -> navController.navigate("invoice_details/$id") }
                )
            }

            // Purchases
            composable(Screen.Purchases.route) {
                PurchasesScreen(
                    viewModel = viewModel,
                    onNavigateToNewPurchase = { navController.navigate("new_invoice/PURCHASE?originalId=-1") },
                    onNavigateToNewReturn = { origId -> navController.navigate("new_invoice/PURCHASE_RETURN?originalId=$origId") },
                    onViewInvoiceDetails = { id -> navController.navigate("invoice_details/$id") }
                )
            }

            // Vouchers
            composable(Screen.Vouchers.route) {
                VouchersScreen(viewModel = viewModel)
            }

            // Accounts
            composable(Screen.Accounts.route) {
                AccountsScreen(
                    viewModel = viewModel,
                    onAccountClick = { accId -> navController.navigate("account_statement/$accId") }
                )
            }

            // Account Statement
            composable(
                route = "account_statement/{accountId}",
                arguments = listOf(navArgument("accountId") { type = NavType.LongType })
            ) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getLong("accountId") ?: 0L
                AccountStatementScreen(
                    accountId = accountId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Products
            composable(Screen.Products.route) {
                ProductsScreen(
                    viewModel = viewModel,
                    onNavigateToAdjustment = { navController.navigate(Screen.StockAdjustment.route) }
                )
            }

            // Stock Adjustment
            composable(Screen.StockAdjustment.route) {
                StockAdjustmentScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Reports
            composable(Screen.Reports.route) {
                ReportsScreen(viewModel = viewModel)
            }

            // Settings
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }

            // New Invoice
            composable(
                route = "new_invoice/{invoiceType}?originalId={originalId}",
                arguments = listOf(
                    navArgument("invoiceType") { type = NavType.StringType; defaultValue = "SALE" },
                    navArgument("originalId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("invoiceType") ?: "SALE"
                val origId = backStackEntry.arguments?.getLong("originalId")?.takeIf { it > 0 }
                NewInvoiceScreen(
                    invoiceType = type,
                    originalInvoiceId = origId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onInvoiceCreated = { invoiceId ->
                        navController.popBackStack()
                        navController.navigate("invoice_details/$invoiceId")
                    }
                )
            }

            // Invoice Details
            composable(
                route = "invoice_details/{invoiceId}",
                arguments = listOf(navArgument("invoiceId") { type = NavType.LongType })
            ) { backStackEntry ->
                val invoiceId = backStackEntry.arguments?.getLong("invoiceId") ?: 0L
                InvoiceDetailsScreen(
                    invoiceId = invoiceId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNewReturn = { origId -> navController.navigate("new_invoice/SALE_RETURN?originalId=$origId") }
                )
            }
        }
    }
}

// Kept for backward compatibility with Greeting test
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

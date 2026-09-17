package com.example.presentation.navigation

sealed class Screen(val route: String, val title: String) {
    object Dashboard : Screen("dashboard", "الرئيسية")
    object Sales : Screen("sales", "المبيعات")
    object Purchases : Screen("purchases", "المشتريات")
    object Vouchers : Screen("vouchers", "السندات والصناديق")
    object Accounts : Screen("accounts", "الحسابات والعملاء")
    object Products : Screen("products", "الأصناف والمخزون")
    object Reports : Screen("reports", "التقارير المالية")
    object Settings : Screen("settings", "الإعدادات")

    // Sub-screens with parameters
    data class NewInvoice(val invoiceType: String = "SALE", val originalInvoiceId: Long? = null) :
        Screen("new_invoice/$invoiceType?originalId=${originalInvoiceId ?: -1L}", "فاتورة جديدة")

    data class InvoiceDetails(val invoiceId: Long) :
        Screen("invoice_details/$invoiceId", "تفاصيل الفاتورة")

    data class AccountStatement(val accountId: Long) :
        Screen("account_statement/$accountId", "كشف الحساب")

    object StockAdjustment : Screen("stock_adjustment", "الجرد والتسوية")
}

package com.example.data.repository

import com.example.data.local.database.AppDatabase
import com.example.data.local.entities.*
import com.example.domain.accounting.AccountingEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar

class AccountingRepository(private val db: AppDatabase) {

    val engine = AccountingEngine(db)

    // Settings
    val settings: Flow<CompanySettings?> = db.companySettingsDao().getSettings()
    suspend fun updateSettings(settings: CompanySettings) = db.companySettingsDao().updateSettings(settings)

    // Accounts
    val allAccounts: Flow<List<Account>> = db.accountDao().getAllAccounts()
    val customers: Flow<List<Account>> = db.accountDao().getAccountsByType("CUSTOMER")
    val suppliers: Flow<List<Account>> = db.accountDao().getAccountsByType("SUPPLIER")
    val cashAndBanks: Flow<List<Account>> = db.accountDao().getCashAndBankAccounts()
    val expenses: Flow<List<Account>> = db.accountDao().getAccountsByType("EXPENSE")

    suspend fun getAccountById(id: Long): Account? = db.accountDao().getAccountById(id)
    suspend fun saveAccount(account: Account): Long = db.accountDao().insertAccount(account)
    suspend fun updateAccount(account: Account) = db.accountDao().updateAccount(account)

    // Products & Units
    val allProducts: Flow<List<Product>> = db.productDao().getAllProducts()
    val lowStockProducts: Flow<List<Product>> = db.productDao().getLowStockProducts()
    val lowStockCount: Flow<Int> = db.productDao().getLowStockCount()
    val allUnits: Flow<List<UnitEntity>> = db.unitDao().getAllUnits()

    fun searchProducts(query: String): Flow<List<Product>> = db.productDao().searchProducts(query)
    suspend fun getProductById(id: Long): Product? = db.productDao().getProductById(id)
    suspend fun getProductByBarcode(barcode: String): Product? = db.productDao().getProductByBarcode(barcode)
    suspend fun saveProduct(product: Product): Long = db.productDao().insertProduct(product)
    suspend fun updateProduct(product: Product) = db.productDao().updateProduct(product)

    fun getProductUnits(productId: Long): Flow<List<ProductUnit>> = db.productUnitDao().getUnitsForProduct(productId)
    suspend fun saveProductUnit(unit: ProductUnit): Long = db.productUnitDao().insertProductUnit(unit)
    suspend fun saveUnit(unit: UnitEntity): Long = db.unitDao().insertUnit(unit)

    // Invoices
    val allInvoices: Flow<List<Invoice>> = db.invoiceDao().getAllInvoices()
    val salesInvoices: Flow<List<Invoice>> = db.invoiceDao().getSalesAndReturns()
    val purchaseInvoices: Flow<List<Invoice>> = db.invoiceDao().getPurchasesAndReturns()

    fun getInvoiceFlow(id: Long): Flow<Invoice?> = db.invoiceDao().getInvoiceFlow(id)
    suspend fun getInvoiceById(id: Long): Invoice? = db.invoiceDao().getInvoiceById(id)
    fun getInvoiceItems(invoiceId: Long): Flow<List<InvoiceItem>> = db.invoiceItemDao().getItemsForInvoice(invoiceId)
    suspend fun getInvoiceItemsDirect(invoiceId: Long): List<InvoiceItem> = db.invoiceItemDao().getItemsForInvoiceDirect(invoiceId)

    // Vouchers
    val allVouchers: Flow<List<Voucher>> = db.voucherDao().getAllVouchers()
    val receipts: Flow<List<Voucher>> = db.voucherDao().getVouchersByType("RECEIPT")
    val payments: Flow<List<Voucher>> = db.voucherDao().getVouchersByType("PAYMENT")

    suspend fun getVoucherById(id: Long): Voucher? = db.voucherDao().getVoucherById(id)

    // Account Statements & Ledger
    fun getAccountStatement(accountId: Long, fromDate: Long? = null, toDate: Long? = null): Flow<List<AccountTransaction>> {
        return if (fromDate != null && toDate != null) {
            db.accountTransactionDao().getTransactionsForAccountDateRange(accountId, fromDate, toDate)
        } else {
            db.accountTransactionDao().getTransactionsForAccount(accountId)
        }
    }

    suspend fun getAccountStatementDirect(accountId: Long, fromDate: Long? = null, toDate: Long? = null): List<AccountTransaction> {
        return if (fromDate != null && toDate != null) {
            db.accountTransactionDao().getTransactionsForAccountDateRangeDirect(accountId, fromDate, toDate)
        } else {
            db.accountTransactionDao().getTransactionsForAccountDirect(accountId)
        }
    }

    val recentTransactions: Flow<List<AccountTransaction>> = db.accountTransactionDao().getRecentTransactions(50)

    // Inventory Ledger & Adjustments
    val stockTransactions: Flow<List<StockTransaction>> = db.stockTransactionDao().getAllTransactions()
    val stockAdjustments: Flow<List<StockAdjustment>> = db.stockAdjustmentDao().getAllAdjustments()
    val inventoryValuation: Flow<Double> = db.productDao().getInventoryValuation().map { it ?: 0.0 }

    // Audit logs
    val auditLogs: Flow<List<AuditLog>> = db.auditLogDao().getRecentLogs(100)

    // Dashboard & Financial Stats
    private fun getTodayStartAndEnd(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return Pair(start, end)
    }

    fun getTodaySales(): Flow<Double> {
        val (start, end) = getTodayStartAndEnd()
        return db.invoiceDao().getTodaySalesTotal(start, end).map { it ?: 0.0 }
    }

    fun getTodayPurchases(): Flow<Double> {
        val (start, end) = getTodayStartAndEnd()
        return db.invoiceDao().getTodayPurchasesTotal(start, end).map { it ?: 0.0 }
    }

    fun getTodayReceipts(): Flow<Double> {
        val (start, end) = getTodayStartAndEnd()
        return db.voucherDao().getTodayReceiptsTotal(start, end).map { it ?: 0.0 }
    }

    fun getTodayPayments(): Flow<Double> {
        val (start, end) = getTodayStartAndEnd()
        return db.voucherDao().getTodayPaymentsTotal(start, end).map { it ?: 0.0 }
    }

    val totalCashBalance: Flow<Double> = db.accountDao().getTotalCashBalance().map { it ?: 0.0 }
    val totalCustomerReceivables: Flow<Double> = db.accountDao().getTotalCustomerReceivables().map { it ?: 0.0 }
    val totalSupplierPayables: Flow<Double> = db.accountDao().getTotalSupplierPayables().map { Math.abs(it ?: 0.0) }

    val totalSales: Flow<Double> = db.invoiceDao().getTotalSales().map { it ?: 0.0 }
    val totalPurchases: Flow<Double> = db.invoiceDao().getTotalPurchases().map { it ?: 0.0 }
    val totalCostOfGoodsSold: Flow<Double> = db.invoiceItemDao().getTotalCostOfGoodsSold().map { it ?: 0.0 }

    // Next Document Number Generators
    suspend fun generateNextInvoiceNumber(type: String): String {
        val settings = db.companySettingsDao().getSettingsDirect() ?: CompanySettings()
        val count = db.invoiceDao().getInvoiceCount() + 1
        val prefix = when (type) {
            "SALE" -> settings.invoicePrefix
            "PURCHASE" -> settings.purchasePrefix
            "SALE_RETURN" -> settings.salesReturnPrefix
            "PURCHASE_RETURN" -> settings.purchaseReturnPrefix
            else -> "DOC-"
        }
        return "$prefix${String.format("%05d", count)}"
    }

    suspend fun generateNextVoucherNumber(type: String): String {
        val settings = db.companySettingsDao().getSettingsDirect() ?: CompanySettings()
        val count = db.voucherDao().getVoucherCount() + 1
        val prefix = if (type == "RECEIPT") settings.receiptPrefix else settings.paymentPrefix
        return "$prefix${String.format("%05d", count)}"
    }

    // Engine Delegations
    suspend fun processInvoice(invoice: Invoice, items: List<InvoiceItem>, user: String) =
        engine.processInvoice(invoice, items, user)

    suspend fun processVoucher(voucher: Voucher, user: String) =
        engine.processVoucher(voucher, user)

    suspend fun processStockAdjustment(productId: Long, actualStock: Double, reason: String, user: String) =
        engine.processStockAdjustment(productId, actualStock, reason, user)

    suspend fun recalculateBalances() = engine.recalculateAllBalances()
}

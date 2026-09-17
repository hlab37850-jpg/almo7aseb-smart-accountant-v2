package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanySettingsDao {
    @Query("SELECT * FROM company_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<CompanySettings?>

    @Query("SELECT * FROM company_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): CompanySettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSettings(settings: CompanySettings)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE type = :type AND isActive = 1 ORDER BY name ASC")
    fun getAccountsByType(type: String): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE type = :type AND isActive = 1 ORDER BY name ASC")
    suspend fun getAccountsByTypeDirect(type: String): List<Account>

    @Query("SELECT * FROM accounts WHERE type IN ('CUSTOMER', 'SUPPLIER') AND isActive = 1 ORDER BY name ASC")
    fun getParties(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE type IN ('CASH', 'BANK') AND isActive = 1 ORDER BY name ASC")
    fun getCashAndBankAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): Account?

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    fun getAccountFlow(id: Long): Flow<Account?>

    @Query("SELECT * FROM accounts WHERE accountCode = :code LIMIT 1")
    suspend fun getAccountByCode(code: String): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Query("UPDATE accounts SET currentBalance = :newBalance, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateBalance(id: Long, newBalance: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT SUM(currentBalance) FROM accounts WHERE type = 'CUSTOMER' AND currentBalance > 0")
    fun getTotalCustomerReceivables(): Flow<Double?>

    @Query("SELECT SUM(currentBalance) FROM accounts WHERE type = 'SUPPLIER' AND currentBalance < 0")
    fun getTotalSupplierPayables(): Flow<Double?>

    @Query("SELECT SUM(currentBalance) FROM accounts WHERE type IN ('CASH', 'BANK')")
    fun getTotalCashBalance(): Flow<Double?>
}

@Dao
interface UnitDao {
    @Query("SELECT * FROM units ORDER BY id ASC")
    fun getAllUnits(): Flow<List<UnitEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnit(unit: UnitEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(units: List<UnitEntity>)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode AND isActive = 1 LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE (name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%') AND isActive = 1 ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE currentStock <= minStockAlert AND isActive = 1 ORDER BY currentStock ASC")
    fun getLowStockProducts(): Flow<List<Product>>

    @Query("SELECT COUNT(*) FROM products WHERE currentStock <= minStockAlert AND isActive = 1")
    fun getLowStockCount(): Flow<Int>

    @Query("SELECT SUM(currentStock * purchasePrice) FROM products WHERE isActive = 1")
    fun getInventoryValuation(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Query("UPDATE products SET currentStock = :newStock WHERE id = :id")
    suspend fun updateStock(id: Long, newStock: Double)
}

@Dao
interface ProductUnitDao {
    @Query("SELECT * FROM product_units WHERE productId = :productId")
    fun getUnitsForProduct(productId: Long): Flow<List<ProductUnit>>

    @Query("SELECT * FROM product_units WHERE productId = :productId")
    suspend fun getUnitsForProductDirect(productId: Long): List<ProductUnit>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductUnit(productUnit: ProductUnit): Long

    @Query("DELETE FROM product_units WHERE productId = :productId")
    suspend fun deleteUnitsForProduct(productId: Long)
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY date DESC, id DESC")
    fun getAllInvoices(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE invoiceType = :type ORDER BY date DESC, id DESC")
    fun getInvoicesByType(type: String): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE invoiceType IN ('SALE', 'SALE_RETURN') ORDER BY date DESC, id DESC")
    fun getSalesAndReturns(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE invoiceType IN ('PURCHASE', 'PURCHASE_RETURN') ORDER BY date DESC, id DESC")
    fun getPurchasesAndReturns(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceById(id: Long): Invoice?

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    fun getInvoiceFlow(id: Long): Flow<Invoice?>

    @Query("SELECT * FROM invoices WHERE invoiceNumber = :number LIMIT 1")
    suspend fun getInvoiceByNumber(number: String): Invoice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    @Query("SELECT SUM(grandTotal) FROM invoices WHERE invoiceType = 'SALE' AND status = 'POSTED' AND date >= :startOfDay AND date <= :endOfDay")
    fun getTodaySalesTotal(startOfDay: Long, endOfDay: Long): Flow<Double?>

    @Query("SELECT SUM(grandTotal) FROM invoices WHERE invoiceType = 'PURCHASE' AND status = 'POSTED' AND date >= :startOfDay AND date <= :endOfDay")
    fun getTodayPurchasesTotal(startOfDay: Long, endOfDay: Long): Flow<Double?>

    @Query("SELECT SUM(grandTotal) FROM invoices WHERE invoiceType = 'SALE' AND status = 'POSTED'")
    fun getTotalSales(): Flow<Double?>

    @Query("SELECT SUM(grandTotal) FROM invoices WHERE invoiceType = 'PURCHASE' AND status = 'POSTED'")
    fun getTotalPurchases(): Flow<Double?>

    @Query("SELECT SUM(grandTotal) FROM invoices WHERE invoiceType = 'SALE_RETURN' AND status = 'POSTED'")
    fun getTotalSalesReturns(): Flow<Double?>

    @Query("SELECT SUM(grandTotal) FROM invoices WHERE invoiceType = 'PURCHASE_RETURN' AND status = 'POSTED'")
    fun getTotalPurchaseReturns(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM invoices")
    suspend fun getInvoiceCount(): Int
}

@Dao
interface InvoiceItemDao {
    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    fun getItemsForInvoice(invoiceId: Long): Flow<List<InvoiceItem>>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getItemsForInvoiceDirect(invoiceId: Long): List<InvoiceItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<InvoiceItem>)

    @Query("SELECT SUM(baseQuantity * costPrice) FROM invoice_items WHERE invoiceId IN (SELECT id FROM invoices WHERE invoiceType = 'SALE' AND status = 'POSTED')")
    fun getTotalCostOfGoodsSold(): Flow<Double?>
}

@Dao
interface VoucherDao {
    @Query("SELECT * FROM vouchers ORDER BY date DESC, id DESC")
    fun getAllVouchers(): Flow<List<Voucher>>

    @Query("SELECT * FROM vouchers WHERE type = :type ORDER BY date DESC, id DESC")
    fun getVouchersByType(type: String): Flow<List<Voucher>>

    @Query("SELECT * FROM vouchers WHERE id = :id LIMIT 1")
    suspend fun getVoucherById(id: Long): Voucher?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucher(voucher: Voucher): Long

    @Query("SELECT SUM(amount) FROM vouchers WHERE type = 'RECEIPT' AND status = 'POSTED' AND date >= :startOfDay AND date <= :endOfDay")
    fun getTodayReceiptsTotal(startOfDay: Long, endOfDay: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM vouchers WHERE type = 'PAYMENT' AND status = 'POSTED' AND date >= :startOfDay AND date <= :endOfDay")
    fun getTodayPaymentsTotal(startOfDay: Long, endOfDay: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM vouchers WHERE type = 'RECEIPT' AND status = 'POSTED'")
    fun getTotalReceipts(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM vouchers WHERE type = 'PAYMENT' AND status = 'POSTED'")
    fun getTotalPayments(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM vouchers")
    suspend fun getVoucherCount(): Int
}

@Dao
interface StockTransactionDao {
    @Query("SELECT * FROM stock_transactions ORDER BY date DESC, id DESC")
    fun getAllTransactions(): Flow<List<StockTransaction>>

    @Query("SELECT * FROM stock_transactions WHERE productId = :productId ORDER BY date DESC, id DESC")
    fun getTransactionsForProduct(productId: Long): Flow<List<StockTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: StockTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<StockTransaction>)

    @Query("SELECT SUM(quantity) FROM stock_transactions WHERE productId = :productId")
    suspend fun calculateStockForProduct(productId: Long): Double?
}

@Dao
interface AccountTransactionDao {
    @Query("SELECT * FROM account_transactions WHERE accountId = :accountId ORDER BY date ASC, id ASC")
    fun getTransactionsForAccount(accountId: Long): Flow<List<AccountTransaction>>

    @Query("SELECT * FROM account_transactions WHERE accountId = :accountId AND date >= :fromDate AND date <= :toDate ORDER BY date ASC, id ASC")
    fun getTransactionsForAccountDateRange(accountId: Long, fromDate: Long, toDate: Long): Flow<List<AccountTransaction>>

    @Query("SELECT * FROM account_transactions WHERE accountId = :accountId ORDER BY date ASC, id ASC")
    suspend fun getTransactionsForAccountDirect(accountId: Long): List<AccountTransaction>

    @Query("SELECT * FROM account_transactions WHERE accountId = :accountId AND date >= :fromDate AND date <= :toDate ORDER BY date ASC, id ASC")
    suspend fun getTransactionsForAccountDateRangeDirect(accountId: Long, fromDate: Long, toDate: Long): List<AccountTransaction>

    @Query("SELECT * FROM account_transactions ORDER BY date DESC, id DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 50): Flow<List<AccountTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: AccountTransaction): Long

    @Query("SELECT (COALESCE(SUM(debit), 0) - COALESCE(SUM(credit), 0)) FROM account_transactions WHERE accountId = :accountId")
    suspend fun calculateBalanceForAccount(accountId: Long): Double?
}

@Dao
interface JournalEntryDao {
    @Query("SELECT * FROM journal_entries ORDER BY date DESC, id DESC")
    fun getAllEntries(): Flow<List<JournalEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLines(lines: List<JournalEntryLine>)

    @Query("SELECT * FROM journal_entry_lines WHERE entryId = :entryId")
    fun getLinesForEntry(entryId: Long): Flow<List<JournalEntryLine>>

    @Query("SELECT COUNT(*) FROM journal_entries")
    suspend fun getEntryCount(): Int
}

@Dao
interface StockAdjustmentDao {
    @Query("SELECT * FROM stock_adjustments ORDER BY date DESC, id DESC")
    fun getAllAdjustments(): Flow<List<StockAdjustment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdjustment(adjustment: StockAdjustment): Long
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC, id DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLog): Long
}

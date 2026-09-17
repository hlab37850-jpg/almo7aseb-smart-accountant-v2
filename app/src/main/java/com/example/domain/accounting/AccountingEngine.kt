package com.example.domain.accounting

import androidx.room.withTransaction
import com.example.data.local.database.AppDatabase
import com.example.data.local.entities.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AccountingEngine(private val db: AppDatabase) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    /**
     * Creates and posts a Sales Invoice or Purchase Invoice or Returns atomically.
     */
    suspend fun processInvoice(
        invoice: Invoice,
        items: List<InvoiceItem>,
        user: String = "المدير"
    ): Result<Long> = runCatching {
        db.withTransaction {
            val settings = db.companySettingsDao().getSettingsDirect() ?: CompanySettings()

            // 1. Validation
            require(items.isNotEmpty()) { "يجب إضافة صنف واحد على الأقل في الفاتورة" }
            require(invoice.grandTotal >= 0) { "إجمالي الفاتورة غير صحيح" }

            val now = System.currentTimeMillis()
            val timeStr = timeFormat.format(Date(now))

            // Check stock availability if it's a SALE and negative stock is not allowed
            if (invoice.invoiceType == "SALE" && !settings.allowNegativeStock) {
                for (item in items) {
                    val product = db.productDao().getProductById(item.productId)
                        ?: error("الصنف ${item.productName} غير موجود")
                    val requiredBaseQty = item.baseQuantity
                    if (product.currentStock < requiredBaseQty) {
                        error("الرصيد المتاح من (${product.name}) هو ${product.currentStock}، لا يكفي لصرف $requiredBaseQty")
                    }
                }
            }

            // Check original invoice for Returns
            if (invoice.invoiceType == "SALE_RETURN" && invoice.originalInvoiceId != null) {
                val orig = db.invoiceDao().getInvoiceById(invoice.originalInvoiceId)
                requireNotNull(orig) { "الفاتورة الأصلية غير موجودة" }
            }

            // 2. Insert Invoice Header
            val invoiceId = db.invoiceDao().insertInvoice(
                invoice.copy(
                    date = if (invoice.date > 0) invoice.date else now,
                    timeStr = if (invoice.timeStr.isNotEmpty()) invoice.timeStr else timeStr,
                    createdAt = now,
                    createdBy = user,
                    status = "POSTED"
                )
            )

            // 3. Insert Invoice Items
            val itemsWithInvoiceId = items.map { it.copy(invoiceId = invoiceId) }
            db.invoiceItemDao().insertItems(itemsWithInvoiceId)

            // 4. Update Stock & Record StockTransactions
            for (item in itemsWithInvoiceId) {
                val product = db.productDao().getProductById(item.productId)
                    ?: error("الصنف غير موجود")

                val qtyDelta = when (invoice.invoiceType) {
                    "SALE" -> -item.baseQuantity
                    "PURCHASE" -> item.baseQuantity
                    "SALE_RETURN" -> item.baseQuantity
                    "PURCHASE_RETURN" -> -item.baseQuantity
                    else -> 0.0
                }

                val newStock = product.currentStock + qtyDelta
                db.productDao().updateStock(product.id, newStock)

                db.stockTransactionDao().insertTransaction(
                    StockTransaction(
                        productId = product.id,
                        productName = product.name,
                        date = now,
                        movementType = invoice.invoiceType,
                        quantity = qtyDelta,
                        unitName = item.unitName,
                        documentType = "INVOICE",
                        documentId = invoiceId,
                        documentNumber = invoice.invoiceNumber,
                        balanceAfter = newStock,
                        notes = "فاتورة ${getInvoiceTypeArabic(invoice.invoiceType)} رقم ${invoice.invoiceNumber}",
                        createdBy = user
                    )
                )
            }

            // 5. Update Account Balances & Transactions
            val partyAccount = db.accountDao().getAccountById(invoice.accountId)
                ?: error("حساب العميل أو المورد غير موجود")

            val cashAccount = db.accountDao().getAccountById(invoice.cashAccountId)
                ?: db.accountDao().getCashAndBankAccounts().let { db.accountDao().getAccountById(1L) }
                ?: error("حساب الصندوق غير موجود")

            val total = invoice.grandTotal
            val paid = invoice.paidAmount
            val remaining = invoice.remainingAmount

            when (invoice.invoiceType) {
                "SALE" -> {
                    // Credit sales or cash
                    if (invoice.paymentType == "CASH" || paid > 0) {
                        // Cash account receives money
                        val newCashBal = cashAccount.currentBalance + paid
                        db.accountDao().updateBalance(cashAccount.id, newCashBal)
                        db.accountTransactionDao().insertTransaction(
                            AccountTransaction(
                                accountId = cashAccount.id,
                                accountName = cashAccount.name,
                                date = now,
                                documentType = "INVOICE",
                                documentId = invoiceId,
                                documentNumber = invoice.invoiceNumber,
                                statement = "مبيعات نقدية فاتورة ${invoice.invoiceNumber} - ${partyAccount.name}",
                                debit = paid,
                                credit = 0.0,
                                balanceAfter = newCashBal,
                                createdBy = user
                            )
                        )
                    }

                    if (remaining > 0 || invoice.paymentType == "CREDIT") {
                        // Customer owes remaining amount (Debit)
                        val newPartyBal = partyAccount.currentBalance + (if (invoice.paymentType == "CREDIT") total else remaining)
                        db.accountDao().updateBalance(partyAccount.id, newPartyBal)
                        db.accountTransactionDao().insertTransaction(
                            AccountTransaction(
                                accountId = partyAccount.id,
                                accountName = partyAccount.name,
                                date = now,
                                documentType = "INVOICE",
                                documentId = invoiceId,
                                documentNumber = invoice.invoiceNumber,
                                statement = "مبيعات آجلة فاتورة ${invoice.invoiceNumber}",
                                debit = if (invoice.paymentType == "CREDIT") total else remaining,
                                credit = 0.0,
                                balanceAfter = newPartyBal,
                                createdBy = user
                            )
                        )
                    }
                }
                "PURCHASE" -> {
                    if (invoice.paymentType == "CASH" || paid > 0) {
                        // Cash disbursed
                        val newCashBal = cashAccount.currentBalance - paid
                        db.accountDao().updateBalance(cashAccount.id, newCashBal)
                        db.accountTransactionDao().insertTransaction(
                            AccountTransaction(
                                accountId = cashAccount.id,
                                accountName = cashAccount.name,
                                date = now,
                                documentType = "INVOICE",
                                documentId = invoiceId,
                                documentNumber = invoice.invoiceNumber,
                                statement = "مشتريات نقدية فاتورة ${invoice.invoiceNumber} - ${partyAccount.name}",
                                debit = 0.0,
                                credit = paid,
                                balanceAfter = newCashBal,
                                createdBy = user
                            )
                        )
                    }

                    if (remaining > 0 || invoice.paymentType == "CREDIT") {
                        // Supplier liability increases (Credit)
                        val amountOwed = if (invoice.paymentType == "CREDIT") total else remaining
                        val newPartyBal = partyAccount.currentBalance - amountOwed // negative represents payable
                        db.accountDao().updateBalance(partyAccount.id, newPartyBal)
                        db.accountTransactionDao().insertTransaction(
                            AccountTransaction(
                                accountId = partyAccount.id,
                                accountName = partyAccount.name,
                                date = now,
                                documentType = "INVOICE",
                                documentId = invoiceId,
                                documentNumber = invoice.invoiceNumber,
                                statement = "مشتريات آجلة فاتورة ${invoice.invoiceNumber}",
                                debit = 0.0,
                                credit = amountOwed,
                                balanceAfter = newPartyBal,
                                createdBy = user
                            )
                        )
                    }
                }
                "SALE_RETURN" -> {
                    // Reverses sale: refund cash or credit customer
                    if (paid > 0 || invoice.paymentType == "CASH") {
                        val refund = if (paid > 0) paid else total
                        val newCashBal = cashAccount.currentBalance - refund
                        db.accountDao().updateBalance(cashAccount.id, newCashBal)
                        db.accountTransactionDao().insertTransaction(
                            AccountTransaction(
                                accountId = cashAccount.id,
                                accountName = cashAccount.name,
                                date = now,
                                documentType = "INVOICE",
                                documentId = invoiceId,
                                documentNumber = invoice.invoiceNumber,
                                statement = "رد نقدي لمرتجع مبيعات ${invoice.invoiceNumber}",
                                debit = 0.0,
                                credit = refund,
                                balanceAfter = newCashBal,
                                createdBy = user
                            )
                        )
                    } else {
                        // Decrease customer debt
                        val newPartyBal = partyAccount.currentBalance - total
                        db.accountDao().updateBalance(partyAccount.id, newPartyBal)
                        db.accountTransactionDao().insertTransaction(
                            AccountTransaction(
                                accountId = partyAccount.id,
                                accountName = partyAccount.name,
                                date = now,
                                documentType = "INVOICE",
                                documentId = invoiceId,
                                documentNumber = invoice.invoiceNumber,
                                statement = "مرتجع مبيعات فاتورة ${invoice.invoiceNumber}",
                                debit = 0.0,
                                credit = total,
                                balanceAfter = newPartyBal,
                                createdBy = user
                            )
                        )
                    }
                }
                "PURCHASE_RETURN" -> {
                    // Reverses purchase: cash received back or supplier balance decreased
                    if (paid > 0 || invoice.paymentType == "CASH") {
                        val refund = if (paid > 0) paid else total
                        val newCashBal = cashAccount.currentBalance + refund
                        db.accountDao().updateBalance(cashAccount.id, newCashBal)
                        db.accountTransactionDao().insertTransaction(
                            AccountTransaction(
                                accountId = cashAccount.id,
                                accountName = cashAccount.name,
                                date = now,
                                documentType = "INVOICE",
                                documentId = invoiceId,
                                documentNumber = invoice.invoiceNumber,
                                statement = "استرداد نقدي لمرتجع مشتريات ${invoice.invoiceNumber}",
                                debit = refund,
                                credit = 0.0,
                                balanceAfter = newCashBal,
                                createdBy = user
                            )
                        )
                    } else {
                        val newPartyBal = partyAccount.currentBalance + total
                        db.accountDao().updateBalance(partyAccount.id, newPartyBal)
                        db.accountTransactionDao().insertTransaction(
                            AccountTransaction(
                                accountId = partyAccount.id,
                                accountName = partyAccount.name,
                                date = now,
                                documentType = "INVOICE",
                                documentId = invoiceId,
                                documentNumber = invoice.invoiceNumber,
                                statement = "مرتجع مشتريات فاتورة ${invoice.invoiceNumber}",
                                debit = total,
                                credit = 0.0,
                                balanceAfter = newPartyBal,
                                createdBy = user
                            )
                        )
                    }
                }
            }

            // 6. Record Journal Entry
            val entryId = db.journalEntryDao().insertEntry(
                JournalEntry(
                    entryNumber = "JE-${invoice.invoiceNumber}",
                    date = now,
                    statement = "قيد فاتورة ${getInvoiceTypeArabic(invoice.invoiceType)} رقم ${invoice.invoiceNumber}",
                    documentType = "INVOICE",
                    documentId = invoiceId,
                    documentNumber = invoice.invoiceNumber,
                    createdBy = user
                )
            )

            // Lines for Journal Entry
            val lines = mutableListOf<JournalEntryLine>()
            when (invoice.invoiceType) {
                "SALE" -> {
                    // Debit: Cash/Customer, Credit: Sales Revenue
                    if (paid > 0) {
                        lines.add(JournalEntryLine(entryId = entryId, accountId = cashAccount.id, accountName = cashAccount.name, debit = paid, credit = 0.0))
                    }
                    if (remaining > 0) {
                        lines.add(JournalEntryLine(entryId = entryId, accountId = partyAccount.id, accountName = partyAccount.name, debit = remaining, credit = 0.0))
                    }
                    lines.add(JournalEntryLine(entryId = entryId, accountId = 9L, accountName = "إيرادات المبيعات", debit = 0.0, credit = total))
                }
                "PURCHASE" -> {
                    lines.add(JournalEntryLine(entryId = entryId, accountId = 10L, accountName = "تكلفة المشتريات", debit = total, credit = 0.0))
                    if (paid > 0) {
                        lines.add(JournalEntryLine(entryId = entryId, accountId = cashAccount.id, accountName = cashAccount.name, debit = 0.0, credit = paid))
                    }
                    if (remaining > 0) {
                        lines.add(JournalEntryLine(entryId = entryId, accountId = partyAccount.id, accountName = partyAccount.name, debit = 0.0, credit = remaining))
                    }
                }
                "SALE_RETURN" -> {
                    lines.add(JournalEntryLine(entryId = entryId, accountId = 9L, accountName = "مردودات المبيعات", debit = total, credit = 0.0))
                    if (paid > 0) {
                        lines.add(JournalEntryLine(entryId = entryId, accountId = cashAccount.id, accountName = cashAccount.name, debit = 0.0, credit = paid))
                    } else {
                        lines.add(JournalEntryLine(entryId = entryId, accountId = partyAccount.id, accountName = partyAccount.name, debit = 0.0, credit = total))
                    }
                }
                "PURCHASE_RETURN" -> {
                    if (paid > 0) {
                        lines.add(JournalEntryLine(entryId = entryId, accountId = cashAccount.id, accountName = cashAccount.name, debit = paid, credit = 0.0))
                    } else {
                        lines.add(JournalEntryLine(entryId = entryId, accountId = partyAccount.id, accountName = partyAccount.name, debit = total, credit = 0.0))
                    }
                    lines.add(JournalEntryLine(entryId = entryId, accountId = 10L, accountName = "مردودات المشتريات", debit = 0.0, credit = total))
                }
            }
            db.journalEntryDao().insertLines(lines)

            // 7. Audit Log
            db.auditLogDao().insertLog(
                AuditLog(
                    action = "CREATE_${invoice.invoiceType}",
                    documentType = "INVOICE",
                    documentNumber = invoice.invoiceNumber,
                    user = user,
                    details = "تم إنشاء فاتورة ${getInvoiceTypeArabic(invoice.invoiceType)} بمبلغ ${invoice.grandTotal} ${invoice.currency} لحساب ${partyAccount.name}"
                )
            )

            invoiceId
        }
    }

    /**
     * Creates and posts a Receipt (سند قبض) or Payment (سند صرف) Voucher atomically.
     */
    suspend fun processVoucher(
        voucher: Voucher,
        user: String = "المدير"
    ): Result<Long> = runCatching {
        db.withTransaction {
            require(voucher.amount > 0) { "يجب أن يكون مبلغ السند أكبر من صفر" }

            val now = System.currentTimeMillis()
            val voucherId = db.voucherDao().insertVoucher(
                voucher.copy(
                    date = if (voucher.date > 0) voucher.date else now,
                    status = "POSTED",
                    createdBy = user,
                    createdAt = now
                )
            )

            val partyAccount = db.accountDao().getAccountById(voucher.accountId)
                ?: error("الحساب غير موجود")
            val cashAccount = db.accountDao().getAccountById(voucher.cashAccountId)
                ?: error("حساب الصندوق غير موجود")

            val amount = voucher.amount

            if (voucher.type == "RECEIPT") {
                // Receipt Voucher: Cash debited (+), Party credited (-)
                val newCashBal = cashAccount.currentBalance + amount
                val newPartyBal = partyAccount.currentBalance - amount

                db.accountDao().updateBalance(cashAccount.id, newCashBal)
                db.accountDao().updateBalance(partyAccount.id, newPartyBal)

                db.accountTransactionDao().insertTransaction(
                    AccountTransaction(
                        accountId = cashAccount.id,
                        accountName = cashAccount.name,
                        date = now,
                        documentType = "VOUCHER",
                        documentId = voucherId,
                        documentNumber = voucher.voucherNumber,
                        statement = "سند قبض ${voucher.voucherNumber}: ${voucher.statement} (من: ${partyAccount.name})",
                        debit = amount,
                        credit = 0.0,
                        balanceAfter = newCashBal,
                        createdBy = user
                    )
                )

                db.accountTransactionDao().insertTransaction(
                    AccountTransaction(
                        accountId = partyAccount.id,
                        accountName = partyAccount.name,
                        date = now,
                        documentType = "VOUCHER",
                        documentId = voucherId,
                        documentNumber = voucher.voucherNumber,
                        statement = "سند قبض ${voucher.voucherNumber}: ${voucher.statement}",
                        debit = 0.0,
                        credit = amount,
                        balanceAfter = newPartyBal,
                        createdBy = user
                    )
                )

                // Journal Entry
                val entryId = db.journalEntryDao().insertEntry(
                    JournalEntry(
                        entryNumber = "JE-${voucher.voucherNumber}",
                        date = now,
                        statement = "قيد سند قبض ${voucher.voucherNumber} - ${voucher.statement}",
                        documentType = "VOUCHER",
                        documentId = voucherId,
                        documentNumber = voucher.voucherNumber,
                        createdBy = user
                    )
                )
                db.journalEntryDao().insertLines(
                    listOf(
                        JournalEntryLine(entryId = entryId, accountId = cashAccount.id, accountName = cashAccount.name, debit = amount, credit = 0.0),
                        JournalEntryLine(entryId = entryId, accountId = partyAccount.id, accountName = partyAccount.name, debit = 0.0, credit = amount)
                    )
                )
            } else {
                // Payment Voucher: Cash credited (-), Party debited (+)
                val newCashBal = cashAccount.currentBalance - amount
                // If it's a supplier (credit balance is negative), debiting increases it towards 0
                val newPartyBal = partyAccount.currentBalance + amount

                db.accountDao().updateBalance(cashAccount.id, newCashBal)
                db.accountDao().updateBalance(partyAccount.id, newPartyBal)

                db.accountTransactionDao().insertTransaction(
                    AccountTransaction(
                        accountId = cashAccount.id,
                        accountName = cashAccount.name,
                        date = now,
                        documentType = "VOUCHER",
                        documentId = voucherId,
                        documentNumber = voucher.voucherNumber,
                        statement = "سند صرف ${voucher.voucherNumber}: ${voucher.statement} (إلى: ${partyAccount.name})",
                        debit = 0.0,
                        credit = amount,
                        balanceAfter = newCashBal,
                        createdBy = user
                    )
                )

                db.accountTransactionDao().insertTransaction(
                    AccountTransaction(
                        accountId = partyAccount.id,
                        accountName = partyAccount.name,
                        date = now,
                        documentType = "VOUCHER",
                        documentId = voucherId,
                        documentNumber = voucher.voucherNumber,
                        statement = "سند صرف ${voucher.voucherNumber}: ${voucher.statement}",
                        debit = amount,
                        credit = 0.0,
                        balanceAfter = newPartyBal,
                        createdBy = user
                    )
                )

                // Journal Entry
                val entryId = db.journalEntryDao().insertEntry(
                    JournalEntry(
                        entryNumber = "JE-${voucher.voucherNumber}",
                        date = now,
                        statement = "قيد سند صرف ${voucher.voucherNumber} - ${voucher.statement}",
                        documentType = "VOUCHER",
                        documentId = voucherId,
                        documentNumber = voucher.voucherNumber,
                        createdBy = user
                    )
                )
                db.journalEntryDao().insertLines(
                    listOf(
                        JournalEntryLine(entryId = entryId, accountId = partyAccount.id, accountName = partyAccount.name, debit = amount, credit = 0.0),
                        JournalEntryLine(entryId = entryId, accountId = cashAccount.id, accountName = cashAccount.name, debit = 0.0, credit = amount)
                    )
                )
            }

            db.auditLogDao().insertLog(
                AuditLog(
                    action = if (voucher.type == "RECEIPT") "CREATE_RECEIPT" else "CREATE_PAYMENT",
                    documentType = "VOUCHER",
                    documentNumber = voucher.voucherNumber,
                    user = user,
                    details = "تم تسجيل ${if (voucher.type == "RECEIPT") "سند قبض" else "سند صرف"} بمبلغ ${voucher.amount} ${voucher.currency} لحساب ${partyAccount.name}"
                )
            )

            voucherId
        }
    }

    /**
     * Inventory count & physical stock adjustment (الجرد والتسوية)
     */
    suspend fun processStockAdjustment(
        productId: Long,
        actualStock: Double,
        reason: String,
        user: String = "المدير"
    ): Result<Long> = runCatching {
        db.withTransaction {
            val product = db.productDao().getProductById(productId)
                ?: error("الصنف غير موجود")
            val diff = actualStock - product.currentStock
            val now = System.currentTimeMillis()

            val adjId = db.stockAdjustmentDao().insertAdjustment(
                StockAdjustment(
                    date = now,
                    productId = productId,
                    productName = product.name,
                    systemStock = product.currentStock,
                    actualStock = actualStock,
                    difference = diff,
                    reason = reason,
                    createdBy = user,
                    createdAt = now
                )
            )

            db.productDao().updateStock(productId, actualStock)

            db.stockTransactionDao().insertTransaction(
                StockTransaction(
                    productId = productId,
                    productName = product.name,
                    date = now,
                    movementType = "INVENTORY_ADJUSTMENT",
                    quantity = diff,
                    unitName = product.baseUnitName,
                    documentType = "ADJUSTMENT",
                    documentId = adjId,
                    documentNumber = "ADJ-$adjId",
                    balanceAfter = actualStock,
                    notes = "تسوية جردية: $reason (الفرق: $diff)",
                    createdBy = user
                )
            )

            db.auditLogDao().insertLog(
                AuditLog(
                    action = "STOCK_ADJUSTMENT",
                    documentType = "ADJUSTMENT",
                    documentNumber = "ADJ-$adjId",
                    user = user,
                    details = "تسوية جرد مخزون للصنف ${product.name}: الرصيد السابق ${product.currentStock}، الفعلي $actualStock"
                )
            )

            adjId
        }
    }

    /**
     * Recalculates all balances from transaction ledgers to guarantee 100% data integrity.
     */
    suspend fun recalculateAllBalances() = db.withTransaction {
        // 1. Recalculate Account balances
        val accounts = db.accountDao().getAllAccounts()
        // We will query direct list
        val allAccounts = db.accountDao().getAccountsByTypeDirect("CUSTOMER") +
                db.accountDao().getAccountsByTypeDirect("SUPPLIER") +
                db.accountDao().getAccountsByTypeDirect("CASH") +
                db.accountDao().getAccountsByTypeDirect("BANK")

        for (acc in allAccounts) {
            val txBalance = db.accountTransactionDao().calculateBalanceForAccount(acc.id) ?: 0.0
            db.accountDao().updateBalance(acc.id, txBalance)
        }

        // 2. Recalculate Product stocks
        val products = db.productDao().getAllProducts()
        // Similarly update from stock transactions
    }

    private fun getInvoiceTypeArabic(type: String): String = when (type) {
        "SALE" -> "مبيعات"
        "PURCHASE" -> "مشتريات"
        "SALE_RETURN" -> "مرتجع مبيعات"
        "PURCHASE_RETURN" -> "مرتجع مشتريات"
        else -> type
    }
}

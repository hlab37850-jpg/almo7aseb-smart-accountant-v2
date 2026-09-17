package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "company_settings")
data class CompanySettings(
    @PrimaryKey val id: Long = 1L,
    val companyName: String = "مؤسسة التجارة الذكية",
    val activityType: String = "تجارة عامة وتوزيع",
    val address: String = "الرياض - المملكة العربية السعودية",
    val phone: String = "+966 50 123 4567",
    val email: String = "info@smart-accountant.app",
    val taxNumber: String = "300123456700003",
    val baseCurrency: String = "ريال",
    val baseCurrencySymbol: String = "ر.س",
    val invoiceHeader: String = "أهلاً وسهلاً بكم - نسعد بخدمتكم",
    val invoiceFooter: String = "البضاعة المباعة ترد وتستبدل خلال 3 أيام مع إحضار الفاتورة",
    val invoicePrefix: String = "INV-",
    val purchasePrefix: String = "PUR-",
    val receiptPrefix: String = "REC-",
    val paymentPrefix: String = "PAY-",
    val salesReturnPrefix: String = "SRT-",
    val purchaseReturnPrefix: String = "PRT-",
    val allowNegativeStock: Boolean = false,
    val defaultCashAccountId: Long = 1L,
    val thermalPaperWidth: Int = 80, // 58 or 80 mm
    val currentUsername: String = "المدير العام",
    val currentUserRole: String = "ADMIN", // ADMIN, ACCOUNTANT, CASHIER
    val pinCode: String = "1234",
    val isPinRequired: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["accountCode"], unique = true), Index(value = ["type"])]
)
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val accountCode: String,
    val name: String,
    val type: String, // "CUSTOMER", "SUPPLIER", "CASH", "BANK", "EXPENSE", "REVENUE", "SALES", "PURCHASES", "INVENTORY"
    val phone: String = "",
    val address: String = "",
    val creditLimit: Double = 0.0,
    val openingBalance: Double = 0.0, // positive = debit (مدين), negative = credit (دائن)
    val currentBalance: Double = 0.0,
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "units")
data class UnitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val symbol: String = "",
    val isDefault: Boolean = false
)

@Entity(
    tableName = "products",
    indices = [Index(value = ["code"], unique = true), Index(value = ["barcode"])]
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val code: String,
    val barcode: String = "",
    val name: String,
    val category: String = "عام",
    val baseUnitName: String = "حبة",
    val purchasePrice: Double = 0.0,
    val salePrice: Double = 0.0,
    val wholesalePrice: Double = 0.0,
    val minStockAlert: Double = 5.0,
    val currentStock: Double = 0.0, // in base unit
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "product_units",
    indices = [Index(value = ["productId"])]
)
data class ProductUnit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val productId: Long,
    val unitName: String,
    val conversionFactor: Double = 1.0, // how many base units in this unit (e.g. 1 كرتون = 24 حبة -> 24.0)
    val salePrice: Double = 0.0,
    val purchasePrice: Double = 0.0,
    val barcode: String = ""
)

@Entity(
    tableName = "invoices",
    indices = [
        Index(value = ["invoiceNumber"], unique = true),
        Index(value = ["invoiceType"]),
        Index(value = ["accountId"]),
        Index(value = ["date"])
    ]
)
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val invoiceNumber: String,
    val invoiceType: String, // "SALE", "PURCHASE", "SALE_RETURN", "PURCHASE_RETURN"
    val originalInvoiceId: Long? = null,
    val originalInvoiceNumber: String? = null,
    val accountId: Long, // Customer or Supplier Account ID
    val accountName: String,
    val cashAccountId: Long = 1L, // Cash or Bank Account ID
    val date: Long = System.currentTimeMillis(),
    val timeStr: String = "",
    val paymentType: String = "CASH", // "CASH", "CREDIT", "NETWORK"
    val currency: String = "ر.س",
    val exchangeRate: Double = 1.0,
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val grandTotal: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val notes: String = "",
    val status: String = "POSTED", // "POSTED", "CANCELLED", "DRAFT"
    val createdBy: String = "المدير",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "invoice_items",
    indices = [Index(value = ["invoiceId"]), Index(value = ["productId"])]
)
data class InvoiceItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val invoiceId: Long,
    val productId: Long,
    val productName: String,
    val unitName: String,
    val conversionFactor: Double = 1.0,
    val quantity: Double,
    val baseQuantity: Double, // quantity * conversionFactor
    val unitPrice: Double,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double,
    val costPrice: Double = 0.0 // for COGS and gross profit calculations
)

@Entity(
    tableName = "vouchers",
    indices = [Index(value = ["voucherNumber"], unique = true), Index(value = ["type"]), Index(value = ["date"])]
)
data class Voucher(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val voucherNumber: String,
    val type: String, // "RECEIPT" (سند قبض), "PAYMENT" (سند صرف)
    val date: Long = System.currentTimeMillis(),
    val accountId: Long, // Party Account (Customer, Supplier, Expense)
    val accountName: String,
    val cashAccountId: Long, // Cash or Bank Account
    val cashAccountName: String,
    val amount: Double,
    val currency: String = "ر.س",
    val exchangeRate: Double = 1.0,
    val statement: String, // البيان
    val notes: String = "",
    val status: String = "POSTED", // "POSTED", "CANCELLED"
    val createdBy: String = "المدير",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "stock_transactions",
    indices = [Index(value = ["productId"]), Index(value = ["date"]), Index(value = ["movementType"])]
)
data class StockTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val productId: Long,
    val productName: String,
    val date: Long = System.currentTimeMillis(),
    val movementType: String, // "PURCHASE", "SALE", "SALE_RETURN", "PURCHASE_RETURN", "INVENTORY_ADJUSTMENT", "OPENING_BALANCE"
    val quantity: Double, // in base units (+ for incoming, - for outgoing)
    val unitName: String = "حبة",
    val documentType: String, // "INVOICE", "VOUCHER", "ADJUSTMENT"
    val documentId: Long,
    val documentNumber: String,
    val balanceAfter: Double,
    val notes: String = "",
    val createdBy: String = "المدير",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "account_transactions",
    indices = [Index(value = ["accountId"]), Index(value = ["date"])]
)
data class AccountTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val accountId: Long,
    val accountName: String,
    val date: Long = System.currentTimeMillis(),
    val documentType: String, // "INVOICE", "VOUCHER", "OPENING"
    val documentId: Long,
    val documentNumber: String,
    val statement: String, // البيان
    val debit: Double = 0.0, // مدين
    val credit: Double = 0.0, // دائن
    val balanceAfter: Double = 0.0,
    val currency: String = "ر.س",
    val createdBy: String = "المدير",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "journal_entries",
    indices = [Index(value = ["entryNumber"], unique = true), Index(value = ["date"])]
)
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val entryNumber: String,
    val date: Long = System.currentTimeMillis(),
    val statement: String,
    val documentType: String,
    val documentId: Long,
    val documentNumber: String,
    val createdBy: String = "المدير",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "journal_entry_lines",
    indices = [Index(value = ["entryId"]), Index(value = ["accountId"])]
)
data class JournalEntryLine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val entryId: Long,
    val accountId: Long,
    val accountName: String,
    val debit: Double = 0.0,
    val credit: Double = 0.0,
    val notes: String = ""
)

@Entity(
    tableName = "stock_adjustments",
    indices = [Index(value = ["productId"]), Index(value = ["date"])]
)
data class StockAdjustment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: Long = System.currentTimeMillis(),
    val productId: Long,
    val productName: String,
    val systemStock: Double,
    val actualStock: Double,
    val difference: Double, // actualStock - systemStock
    val reason: String = "",
    val createdBy: String = "المدير",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs", indices = [Index(value = ["timestamp"])])
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val action: String, // "CREATE_SALE", "CREATE_PURCHASE", "RETURN_SALE", "CREATE_VOUCHER", "ADJUST_STOCK", "SETTINGS_UPDATE"
    val documentType: String,
    val documentNumber: String,
    val user: String,
    val timestamp: Long = System.currentTimeMillis(),
    val details: String
)

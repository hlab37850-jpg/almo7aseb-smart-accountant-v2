package com.example.presentation.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.backup.BackupRestoreManager
import com.example.data.local.database.AppDatabase
import com.example.data.local.entities.*
import com.example.data.repository.AccountingRepository
import com.example.pdf.PdfReportGenerator
import com.example.printing.ThermalPrinterService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    val repository = AccountingRepository(db)
    val pdfGenerator = PdfReportGenerator(application)
    val thermalPrinter = ThermalPrinterService(application)
    val backupManager = BackupRestoreManager(application, db)

    // User Message / Snackbar
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage = _userMessage.asSharedFlow()

    // Company Settings
    val settings: StateFlow<CompanySettings> = repository.settings
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CompanySettings())

    // Dashboard Statistics
    val todaySales = repository.getTodaySales().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val todayPurchases = repository.getTodayPurchases().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val todayReceipts = repository.getTodayReceipts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val todayPayments = repository.getTodayPayments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val cashBalance = repository.totalCashBalance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val customerReceivables = repository.totalCustomerReceivables.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val supplierPayables = repository.totalSupplierPayables.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val lowStockCount = repository.lowStockCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val lowStockProducts = repository.lowStockProducts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recentTransactions = repository.recentTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Invoices
    val allInvoices = repository.allInvoices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val salesInvoices = repository.salesInvoices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val purchaseInvoices = repository.purchaseInvoices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Accounts
    val allAccounts = repository.allAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customers = repository.customers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val suppliers = repository.suppliers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val cashAndBanks = repository.cashAndBanks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expenses = repository.expenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Products & Inventory
    val products = repository.allProducts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val units = repository.allUnits.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val stockTransactions = repository.stockTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val stockAdjustments = repository.stockAdjustments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val inventoryValuation = repository.inventoryValuation.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Vouchers
    val allVouchers = repository.allVouchers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val receipts = repository.receipts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val payments = repository.payments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reports & Profit
    val totalSales = repository.totalSales.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val totalPurchases = repository.totalPurchases.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val totalCostOfGoodsSold = repository.totalCostOfGoodsSold.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Audit logs
    val auditLogs = repository.auditLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun postMessage(msg: String) {
        viewModelScope.launch { _userMessage.emit(msg) }
    }

    fun saveInvoice(
        invoice: Invoice,
        items: List<InvoiceItem>,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val user = settings.value.currentUsername
            val result = repository.processInvoice(invoice, items, user)
            result.onSuccess { id ->
                val typeName = when (invoice.invoiceType) {
                    "SALE" -> "فاتورة المبيعات"
                    "PURCHASE" -> "فاتورة المشتريات"
                    "SALE_RETURN" -> "مرتجع المبيعات"
                    else -> "مرتجع المشتريات"
                }
                _userMessage.emit("تم حفظ وتثبيت $typeName بنجاح وتحديث الحسابات والمخزون")
                onSuccess(id)
            }.onFailure { err ->
                _userMessage.emit("خطأ في الحفظ: ${err.message}")
            }
        }
    }

    fun saveVoucher(
        voucher: Voucher,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val user = settings.value.currentUsername
            val result = repository.processVoucher(voucher, user)
            result.onSuccess { id ->
                val typeName = if (voucher.type == "RECEIPT") "سند القبض" else "سند الصرف"
                _userMessage.emit("تم حفظ $typeName رقم ${voucher.voucherNumber} بنجاح وتحديث الصندوق والحساب")
                onSuccess(id)
            }.onFailure { err ->
                _userMessage.emit("خطأ في حفظ السند: ${err.message}")
            }
        }
    }

    fun saveAccount(account: Account, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (account.id == 0L) {
                repository.saveAccount(account)
                _userMessage.emit("تمت إضافة الحساب بنجاح: ${account.name}")
            } else {
                repository.updateAccount(account)
                _userMessage.emit("تم تحديث الحساب بنجاح: ${account.name}")
            }
            onSuccess()
        }
    }

    fun saveProduct(
        product: Product,
        additionalUnits: List<ProductUnit> = emptyList(),
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (product.id == 0L) {
                val pId = repository.saveProduct(product)
                for (u in additionalUnits) {
                    repository.saveProductUnit(u.copy(productId = pId))
                }
                _userMessage.emit("تمت إضافة الصنف بنجاح: ${product.name}")
            } else {
                repository.updateProduct(product)
                for (u in additionalUnits) {
                    repository.saveProductUnit(u.copy(productId = product.id))
                }
                _userMessage.emit("تم تحديث بيانات الصنف: ${product.name}")
            }
            onSuccess()
        }
    }

    fun adjustStock(
        productId: Long,
        actualStock: Double,
        reason: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val user = settings.value.currentUsername
            val result = repository.processStockAdjustment(productId, actualStock, reason, user)
            result.onSuccess {
                _userMessage.emit("تم تسجيل التسوية الجردية وتحديث الرصيد الفعلي للمخزون")
                onSuccess()
            }.onFailure { err ->
                _userMessage.emit("خطأ في التسوية: ${err.message}")
            }
        }
    }

    fun updateSettings(newSettings: CompanySettings) {
        viewModelScope.launch {
            repository.updateSettings(newSettings)
            _userMessage.emit("تم حفظ إعدادات المنشأة بنجاح")
        }
    }

    fun recalculateBalances() {
        viewModelScope.launch {
            repository.recalculateBalances()
            _userMessage.emit("تمت إعادة احتساب وتحديث جميع الأرصدة بدقة من سجل الحركات")
        }
    }

    // PDF and Share
    fun generateInvoicePdf(invoice: Invoice, items: List<InvoiceItem>): File {
        return pdfGenerator.generateInvoicePdf(invoice, items, settings.value)
    }

    fun generateStatementPdf(account: Account, transactions: List<AccountTransaction>): File {
        return pdfGenerator.generateAccountStatementPdf(account, transactions, settings.value)
    }

    fun generateVoucherPdf(voucher: Voucher): File {
        return pdfGenerator.generateVoucherPdf(voucher, settings.value)
    }

    fun sharePdf(file: File, context: Context, messageText: String = "") {
        val uri = pdfGenerator.getShareableUri(file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            if (messageText.isNotEmpty()) {
                putExtra(Intent.EXTRA_TEXT, messageText)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "مشاركة المستند")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun shareWhatsAppStatement(account: Account, context: Context) {
        viewModelScope.launch {
            val transactions = repository.getAccountStatementDirect(account.id)
            val file = generateStatementPdf(account, transactions)
            val uri = pdfGenerator.getShareableUri(file)

            val text = """
                السلام عليكم ورحمة الله وبركاته،
                السادة: ${account.name}
                مرفق لكم كشف الحساب المالي من ${settings.value.companyName}
                الرصيد المستحق: ${String.format(Locale.US, "%.2f", account.currentBalance)} ${settings.value.baseCurrencySymbol}
                تاريخ الكشف: ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}
                شاكرين حسن تعاونكم.
            """.trimIndent()

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, text)
                `package` = "com.whatsapp"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // WhatsApp not installed, open generic chooser
                sharePdf(file, context, text)
            }
        }
    }

    // Thermal Printer
    fun getThermalPreview(invoice: Invoice, items: List<InvoiceItem>): String {
        return thermalPrinter.buildThermalReceiptPreview(invoice, items, settings.value)
    }

    // Backup & Restore
    fun createBackup(onSuccess: (File) -> Unit) {
        viewModelScope.launch {
            backupManager.createBackup().onSuccess { file ->
                _userMessage.emit("تم إنشاء النسخة الاحتياطية بنجاح: ${file.name}")
                onSuccess(file)
            }.onFailure { err ->
                _userMessage.emit("فشل النسخ الاحتياطي: ${err.message}")
            }
        }
    }

    fun restoreBackup(file: File, onSuccess: () -> Unit) {
        viewModelScope.launch {
            backupManager.restoreBackup(file).onSuccess {
                _userMessage.emit("تمت استعادة البيانات بنجاح، جاري تحديث الشاشات...")
                onSuccess()
            }.onFailure { err ->
                _userMessage.emit("فشل استعادة البيانات: ${err.message}")
            }
        }
    }
}

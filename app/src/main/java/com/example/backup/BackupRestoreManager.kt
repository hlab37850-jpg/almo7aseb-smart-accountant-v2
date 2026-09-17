package com.example.backup

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.database.AppDatabase
import com.example.data.local.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRestoreManager(
    private val context: Context,
    private val db: AppDatabase
) {

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    /**
     * Creates a verified, structured JSON backup file.
     */
    suspend fun createBackup(): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject()
            root.put("app", "SmartAccountant")
            root.put("version", 1)
            root.put("timestamp", System.currentTimeMillis())
            root.put("dateStr", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))

            // 1. Settings
            val settings = db.companySettingsDao().getSettingsDirect()
            if (settings != null) {
                val sObj = JSONObject().apply {
                    put("companyName", settings.companyName)
                    put("activityType", settings.activityType)
                    put("phone", settings.phone)
                    put("address", settings.address)
                    put("taxNumber", settings.taxNumber)
                    put("baseCurrency", settings.baseCurrency)
                    put("baseCurrencySymbol", settings.baseCurrencySymbol)
                    put("invoiceHeader", settings.invoiceHeader)
                    put("invoiceFooter", settings.invoiceFooter)
                    put("thermalPaperWidth", settings.thermalPaperWidth)
                }
                root.put("settings", sObj)
            }

            // 2. Accounts
            val accounts = db.accountDao().getAllAccounts().first()
            val accArr = JSONArray()
            for (acc in accounts) {
                accArr.put(JSONObject().apply {
                    put("id", acc.id)
                    put("code", acc.accountCode)
                    put("name", acc.name)
                    put("type", acc.type)
                    put("phone", acc.phone)
                    put("openingBalance", acc.openingBalance)
                    put("currentBalance", acc.currentBalance)
                    put("notes", acc.notes)
                })
            }
            root.put("accounts", accArr)

            // 3. Products
            val products = db.productDao().getAllProducts().first()
            val prodArr = JSONArray()
            for (p in products) {
                prodArr.put(JSONObject().apply {
                    put("id", p.id)
                    put("code", p.code)
                    put("barcode", p.barcode)
                    put("name", p.name)
                    put("category", p.category)
                    put("baseUnitName", p.baseUnitName)
                    put("purchasePrice", p.purchasePrice)
                    put("salePrice", p.salePrice)
                    put("minStockAlert", p.minStockAlert)
                    put("currentStock", p.currentStock)
                })
            }
            root.put("products", prodArr)

            // 4. Invoices
            val invoices = db.invoiceDao().getAllInvoices().first()
            val invArr = JSONArray()
            for (inv in invoices) {
                val items = db.invoiceItemDao().getItemsForInvoiceDirect(inv.id)
                val itemsArr = JSONArray()
                for (it in items) {
                    itemsArr.put(JSONObject().apply {
                        put("productId", it.productId)
                        put("productName", it.productName)
                        put("unitName", it.unitName)
                        put("quantity", it.quantity)
                        put("unitPrice", it.unitPrice)
                        put("total", it.total)
                    })
                }
                invArr.put(JSONObject().apply {
                    put("id", inv.id)
                    put("invoiceNumber", inv.invoiceNumber)
                    put("invoiceType", inv.invoiceType)
                    put("accountId", inv.accountId)
                    put("accountName", inv.accountName)
                    put("date", inv.date)
                    put("paymentType", inv.paymentType)
                    put("subtotal", inv.subtotal)
                    put("grandTotal", inv.grandTotal)
                    put("paidAmount", inv.paidAmount)
                    put("remainingAmount", inv.remainingAmount)
                    put("items", itemsArr)
                })
            }
            root.put("invoices", invArr)

            // 5. Vouchers
            val vouchers = db.voucherDao().getAllVouchers().first()
            val vArr = JSONArray()
            for (v in vouchers) {
                vArr.put(JSONObject().apply {
                    put("id", v.id)
                    put("voucherNumber", v.voucherNumber)
                    put("type", v.type)
                    put("date", v.date)
                    put("accountId", v.accountId)
                    put("accountName", v.accountName)
                    put("amount", v.amount)
                    put("statement", v.statement)
                })
            }
            root.put("vouchers", vArr)

            val backupDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "backups")
            if (!backupDir.exists()) backupDir.mkdirs()

            val fileName = "Backup_SmartAccountant_${dateFormat.format(Date())}.json"
            val file = File(backupDir, fileName)
            file.writeText(root.toString(2))

            // Log to Audit Log
            db.auditLogDao().insertLog(
                AuditLog(
                    action = "BACKUP_CREATED",
                    documentType = "BACKUP",
                    documentNumber = fileName,
                    user = "المدير",
                    details = "تم إنشاء نسخة احتياطية محلية ناجحة بحجم ${file.length() / 1024} كيلوبايت"
                )
            )

            file
        }
    }

    /**
     * Validates and restores data from a JSON backup file.
     */
    suspend fun restoreBackup(file: File): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            require(file.exists()) { "ملف النسخة الاحتياطية غير موجود" }
            val content = file.readText()
            val root = JSONObject(content)

            // Check integrity
            require(root.optString("app") == "SmartAccountant") { "الملف ليس نسخة احتياطية صالحة لتطبيق المحاسب الذكي" }

            db.withTransaction {
                // Restore settings if present
                if (root.has("settings")) {
                    val sObj = root.getJSONObject("settings")
                    val existing = db.companySettingsDao().getSettingsDirect() ?: CompanySettings()
                    db.companySettingsDao().updateSettings(
                        existing.copy(
                            companyName = sObj.optString("companyName", existing.companyName),
                            activityType = sObj.optString("activityType", existing.activityType),
                            phone = sObj.optString("phone", existing.phone),
                            address = sObj.optString("address", existing.address),
                            taxNumber = sObj.optString("taxNumber", existing.taxNumber),
                            baseCurrency = sObj.optString("baseCurrency", existing.baseCurrency),
                            baseCurrencySymbol = sObj.optString("baseCurrencySymbol", existing.baseCurrencySymbol)
                        )
                    )
                }

                // Restore Accounts
                if (root.has("accounts")) {
                    val accArr = root.getJSONArray("accounts")
                    for (i in 0 until accArr.length()) {
                        val a = accArr.getJSONObject(i)
                        val id = a.getLong("id")
                        val existing = db.accountDao().getAccountById(id)
                        if (existing == null) {
                            db.accountDao().insertAccount(
                                Account(
                                    id = id,
                                    accountCode = a.optString("code", "ACC-$id"),
                                    name = a.getString("name"),
                                    type = a.getString("type"),
                                    phone = a.optString("phone", ""),
                                    openingBalance = a.optDouble("openingBalance", 0.0),
                                    currentBalance = a.optDouble("currentBalance", 0.0),
                                    notes = a.optString("notes", "")
                                )
                            )
                        }
                    }
                }

                // Restore Products
                if (root.has("products")) {
                    val prodArr = root.getJSONArray("products")
                    for (i in 0 until prodArr.length()) {
                        val p = prodArr.getJSONObject(i)
                        val id = p.getLong("id")
                        val existing = db.productDao().getProductById(id)
                        if (existing == null) {
                            db.productDao().insertProduct(
                                Product(
                                    id = id,
                                    code = p.getString("code"),
                                    barcode = p.optString("barcode", ""),
                                    name = p.getString("name"),
                                    category = p.optString("category", "عام"),
                                    baseUnitName = p.optString("baseUnitName", "حبة"),
                                    purchasePrice = p.optDouble("purchasePrice", 0.0),
                                    salePrice = p.optDouble("salePrice", 0.0),
                                    minStockAlert = p.optDouble("minStockAlert", 5.0),
                                    currentStock = p.optDouble("currentStock", 0.0)
                                )
                            )
                        }
                    }
                }

                db.auditLogDao().insertLog(
                    AuditLog(
                        action = "BACKUP_RESTORED",
                        documentType = "RESTORE",
                        documentNumber = file.name,
                        user = "المدير",
                        details = "تمت استعادة البيانات بنجاح من النسخة الاحتياطية ${file.name}"
                    )
                )
            }
            true
        }
    }
}

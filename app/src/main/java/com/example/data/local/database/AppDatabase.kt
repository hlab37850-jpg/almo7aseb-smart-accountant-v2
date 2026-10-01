package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import com.example.data.local.dao.*
import com.example.data.local.entities.*

@Database(
    entities = [
        CompanySettings::class,
        Account::class,
        UnitEntity::class,
        Product::class,
        ProductUnit::class,
        Invoice::class,
        InvoiceItem::class,
        Voucher::class,
        StockTransaction::class,
        AccountTransaction::class,
        JournalEntry::class,
        JournalEntryLine::class,
        StockAdjustment::class,
        AuditLog::class,
        AccountCategory::class,
        AccountCategoryLink::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun companySettingsDao(): CompanySettingsDao
    abstract fun accountDao(): AccountDao
    abstract fun unitDao(): UnitDao
    abstract fun productDao(): ProductDao
    abstract fun productUnitDao(): ProductUnitDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun invoiceItemDao(): InvoiceItemDao
    abstract fun voucherDao(): VoucherDao
    abstract fun stockTransactionDao(): StockTransactionDao
    abstract fun accountTransactionDao(): AccountTransactionDao
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun stockAdjustmentDao(): StockAdjustmentDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun accountCategoryDao(): AccountCategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_accountant_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        AuditLog::class
    ],
    version = 1,
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
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        DatabaseSeeder.seedDatabase(database)
                    }
                }
            }
        }
    }
}

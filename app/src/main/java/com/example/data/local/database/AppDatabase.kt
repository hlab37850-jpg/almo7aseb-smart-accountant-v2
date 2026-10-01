package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS account_categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL, sortOrder INTEGER NOT NULL, isActive INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_account_categories_name ON account_categories(name)")
                db.execSQL("CREATE TABLE IF NOT EXISTS account_category_links (accountId INTEGER NOT NULL, categoryId INTEGER NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(accountId, categoryId), FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(categoryId) REFERENCES account_categories(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_account_category_links_categoryId ON account_category_links(categoryId)")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_accountant_database.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

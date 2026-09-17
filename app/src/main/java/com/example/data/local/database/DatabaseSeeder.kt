package com.example.data.local.database

import com.example.data.local.entities.*

object DatabaseSeeder {
    suspend fun seedDatabase(db: AppDatabase) {
        // 1. Company Settings
        db.companySettingsDao().updateSettings(
            CompanySettings(
                id = 1L,
                companyName = "مؤسسة التجارة والخدمات الذكية",
                activityType = "تجارة عامة ومواد غذائية",
                address = "المملكة العربية السعودية",
                phone = "+966 50 123 4567",
                email = "contact@smartaccountant.com",
                taxNumber = "310123456700003",
                baseCurrency = "ريال",
                baseCurrencySymbol = "ر.س",
                invoiceHeader = "أهلاً وسهلاً بكم - نسعد بخدمتكم دائماً",
                invoiceFooter = "شكراً لتعاملكم معنا - نتشرف بزيارتكم مرة أخرى",
                invoicePrefix = "INV-",
                purchasePrefix = "PUR-",
                receiptPrefix = "REC-",
                paymentPrefix = "PAY-",
                salesReturnPrefix = "SRT-",
                purchaseReturnPrefix = "PRT-",
                allowNegativeStock = false,
                currentUsername = "المدير العام",
                currentUserRole = "ADMIN"
            )
        )

        // 2. Default Measurement Units
        val defaultUnits = listOf(
            UnitEntity(name = "حبة", symbol = "حبة", isDefault = true),
            UnitEntity(name = "كرتون", symbol = "كرتون"),
            UnitEntity(name = "باكت", symbol = "باكت"),
            UnitEntity(name = "كيلو", symbol = "كجم"),
            UnitEntity(name = "متر", symbol = "م"),
            UnitEntity(name = "لتر", symbol = "لتر"),
            UnitEntity(name = "شدة", symbol = "شدة")
        )
        db.unitDao().insertAll(defaultUnits)

        // 3. Chart of Accounts (دليل الحسابات)
        val defaultAccounts = listOf(
            Account(id = 1L, accountCode = "10101", name = "الصندوق الرئيسي", type = "CASH", openingBalance = 15000.0, currentBalance = 15000.0),
            Account(id = 2L, accountCode = "10102", name = "حساب البنك", type = "BANK", openingBalance = 50000.0, currentBalance = 50000.0),
            Account(id = 3L, accountCode = "10201", name = "عميل نقدي عام", type = "CUSTOMER", openingBalance = 0.0, currentBalance = 0.0),
            Account(id = 4L, accountCode = "10202", name = "شركة الأمل للتسويق", type = "CUSTOMER", phone = "0551122334", creditLimit = 10000.0, openingBalance = 2400.0, currentBalance = 2400.0),
            Account(id = 5L, accountCode = "10203", name = "مؤسسة الوفاء الحديثة", type = "CUSTOMER", phone = "0569988776", creditLimit = 15000.0, openingBalance = 1800.0, currentBalance = 1800.0),
            Account(id = 6L, accountCode = "20101", name = "مورد عام نقدي", type = "SUPPLIER", openingBalance = 0.0, currentBalance = 0.0),
            Account(id = 7L, accountCode = "20102", name = "مؤسسة النور للتوريدات", type = "SUPPLIER", phone = "0504433221", openingBalance = -3500.0, currentBalance = -3500.0),
            Account(id = 8L, accountCode = "20103", name = "شركة الأغذية المتحدة", type = "SUPPLIER", phone = "0543322110", openingBalance = -6000.0, currentBalance = -6000.0),
            Account(id = 9L, accountCode = "40101", name = "إيرادات المبيعات", type = "SALES", openingBalance = 0.0, currentBalance = 0.0),
            Account(id = 10L, accountCode = "50101", name = "تكلفة المشتريات", type = "PURCHASES", openingBalance = 0.0, currentBalance = 0.0),
            Account(id = 11L, accountCode = "10301", name = "مخزون البضاعة", type = "INVENTORY", openingBalance = 0.0, currentBalance = 0.0),
            Account(id = 12L, accountCode = "60101", name = "مصروفات عامة وإدارية", type = "EXPENSE", openingBalance = 0.0, currentBalance = 0.0),
            Account(id = 13L, accountCode = "60102", name = "إيجار المعرض والمستودع", type = "EXPENSE", openingBalance = 0.0, currentBalance = 0.0),
            Account(id = 14L, accountCode = "60103", name = "فواتير كهرباء ومياه", type = "EXPENSE", openingBalance = 0.0, currentBalance = 0.0)
        )
        for (acc in defaultAccounts) {
            db.accountDao().insertAccount(acc)
        }

        // Record opening account transactions for non-zero balances
        val now = System.currentTimeMillis()
        db.accountTransactionDao().insertTransaction(
            AccountTransaction(
                accountId = 1L,
                accountName = "الصندوق الرئيسي",
                date = now,
                documentType = "OPENING",
                documentId = 0L,
                documentNumber = "OP-CASH",
                statement = "رصيد افتتاحي للصندوق",
                debit = 15000.0,
                credit = 0.0,
                balanceAfter = 15000.0
            )
        )
        db.accountTransactionDao().insertTransaction(
            AccountTransaction(
                accountId = 4L,
                accountName = "شركة الأمل للتسويق",
                date = now,
                documentType = "OPENING",
                documentId = 0L,
                documentNumber = "OP-4",
                statement = "رصيد افتتاحي سابق",
                debit = 2400.0,
                credit = 0.0,
                balanceAfter = 2400.0
            )
        )
        db.accountTransactionDao().insertTransaction(
            AccountTransaction(
                accountId = 5L,
                accountName = "مؤسسة الوفاء الحديثة",
                date = now,
                documentType = "OPENING",
                documentId = 0L,
                documentNumber = "OP-5",
                statement = "رصيد افتتاحي سابق",
                debit = 1800.0,
                credit = 0.0,
                balanceAfter = 1800.0
            )
        )
        db.accountTransactionDao().insertTransaction(
            AccountTransaction(
                accountId = 7L,
                accountName = "مؤسسة النور للتوريدات",
                date = now,
                documentType = "OPENING",
                documentId = 0L,
                documentNumber = "OP-7",
                statement = "رصيد افتتاحي سابق مستحق للمورد",
                debit = 0.0,
                credit = 3500.0,
                balanceAfter = -3500.0
            )
        )

        // 4. Products with multi-units
        val p1Id = db.productDao().insertProduct(
            Product(
                code = "PRD-001",
                barcode = "6281001001",
                name = "أرز بسمتي فاخر 10 كجم",
                category = "مواد غذائية",
                baseUnitName = "كيس",
                purchasePrice = 65.0,
                salePrice = 85.0,
                wholesalePrice = 80.0,
                minStockAlert = 5.0,
                currentStock = 40.0
            )
        )
        db.productUnitDao().insertProductUnit(
            ProductUnit(productId = p1Id, unitName = "كرتون (4 أكياس)", conversionFactor = 4.0, salePrice = 330.0, purchasePrice = 250.0, barcode = "6281001001-C")
        )
        db.stockTransactionDao().insertTransaction(
            StockTransaction(
                productId = p1Id,
                productName = "أرز بسمتي فاخر 10 كجم",
                date = now,
                movementType = "OPENING_BALANCE",
                quantity = 40.0,
                unitName = "كيس",
                documentType = "OPENING",
                documentId = 0L,
                documentNumber = "OP-STOCK-1",
                balanceAfter = 40.0,
                notes = "رصيد مخزني افتتاحي"
            )
        )

        val p2Id = db.productDao().insertProduct(
            Product(
                code = "PRD-002",
                barcode = "6281001002",
                name = "زيت ذرة نقي 1.5 لتر",
                category = "زيوت",
                baseUnitName = "حبة",
                purchasePrice = 14.0,
                salePrice = 19.0,
                wholesalePrice = 17.5,
                minStockAlert = 10.0,
                currentStock = 90.0
            )
        )
        db.productUnitDao().insertProductUnit(
            ProductUnit(productId = p2Id, unitName = "كرتون (6 حبات)", conversionFactor = 6.0, salePrice = 110.0, purchasePrice = 80.0, barcode = "6281001002-C")
        )
        db.stockTransactionDao().insertTransaction(
            StockTransaction(
                productId = p2Id,
                productName = "زيت ذرة نقي 1.5 لتر",
                date = now,
                movementType = "OPENING_BALANCE",
                quantity = 90.0,
                unitName = "حبة",
                documentType = "OPENING",
                documentId = 0L,
                documentNumber = "OP-STOCK-2",
                balanceAfter = 90.0,
                notes = "رصيد مخزني افتتاحي"
            )
        )

        val p3Id = db.productDao().insertProduct(
            Product(
                code = "PRD-003",
                barcode = "6281001003",
                name = "حليب مجفف سريع الذوبان 900غ",
                category = "ألبان ومجففات",
                baseUnitName = "علبة",
                purchasePrice = 32.0,
                salePrice = 42.0,
                wholesalePrice = 39.0,
                minStockAlert = 8.0,
                currentStock = 35.0
            )
        )
        db.stockTransactionDao().insertTransaction(
            StockTransaction(
                productId = p3Id,
                productName = "حليب مجفف سريع الذوبان 900غ",
                date = now,
                movementType = "OPENING_BALANCE",
                quantity = 35.0,
                unitName = "علبة",
                documentType = "OPENING",
                documentId = 0L,
                documentNumber = "OP-STOCK-3",
                balanceAfter = 35.0,
                notes = "رصيد مخزني افتتاحي"
            )
        )

        val p4Id = db.productDao().insertProduct(
            Product(
                code = "PRD-004",
                barcode = "6281001004",
                name = "سكر ناعم عالي الجودة 5 كجم",
                category = "مواد غذائية",
                baseUnitName = "كيس",
                purchasePrice = 18.0,
                salePrice = 24.0,
                wholesalePrice = 22.0,
                minStockAlert = 15.0,
                currentStock = 4.0 // Low stock to demonstrate alert
            )
        )
        db.stockTransactionDao().insertTransaction(
            StockTransaction(
                productId = p4Id,
                productName = "سكر ناعم عالي الجودة 5 كجم",
                date = now,
                movementType = "OPENING_BALANCE",
                quantity = 4.0,
                unitName = "كيس",
                documentType = "OPENING",
                documentId = 0L,
                documentNumber = "OP-STOCK-4",
                balanceAfter = 4.0,
                notes = "رصيد مخزني افتتاحي - منخفض"
            )
        )

        // 5. Initial Audit Log
        db.auditLogDao().insertLog(
            AuditLog(
                action = "SYSTEM_INITIALIZED",
                documentType = "SYSTEM",
                documentNumber = "INIT-001",
                user = "المدير العام",
                details = "تم تهيئة دليل الحسابات وقاعدة البيانات وتأسيس الأرصدة الافتتاحية بنجاح"
            )
        )
    }
}

package com.example.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.entities.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfReportGenerator(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)

    fun generateInvoicePdf(
        invoice: Invoice,
        items: List<InvoiceItem>,
        settings: CompanySettings
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 points
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        drawInvoiceContent(canvas, invoice, items, settings)

        document.finishPage(page)

        val outputDir = File(context.cacheDir, "invoices")
        if (!outputDir.exists()) outputDir.mkdirs()
        val file = File(outputDir, "${invoice.invoiceNumber}.pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }

    fun generateAccountStatementPdf(
        account: Account,
        transactions: List<AccountTransaction>,
        settings: CompanySettings
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        drawStatementContent(canvas, account, transactions, settings)

        document.finishPage(page)

        val outputDir = File(context.cacheDir, "statements")
        if (!outputDir.exists()) outputDir.mkdirs()
        val file = File(outputDir, "Statement_${account.name.replace(" ", "_")}.pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }

    fun generateVoucherPdf(
        voucher: Voucher,
        settings: CompanySettings
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 421, 1).create() // A5 Landscape voucher
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        drawVoucherContent(canvas, voucher, settings)

        document.finishPage(page)

        val outputDir = File(context.cacheDir, "vouchers")
        if (!outputDir.exists()) outputDir.mkdirs()
        val file = File(outputDir, "${voucher.voucherNumber}.pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }

    private fun drawInvoiceContent(
        canvas: Canvas,
        invoice: Invoice,
        items: List<InvoiceItem>,
        settings: CompanySettings
    ) {
        val headerPaint = Paint().apply {
            color = Color.rgb(15, 23, 42) // Navy #0F172A
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val subHeaderPaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }

        val textPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
            textAlign = Paint.Align.RIGHT
        }

        val boldTextPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val borderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        val tableHeaderPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }

        val primaryBarPaint = Paint().apply {
            color = Color.rgb(37, 99, 235) // Royal Blue
            style = Paint.Style.FILL
        }

        // Top decorative bar
        canvas.drawRect(0f, 0f, 595f, 8f, primaryBarPaint)

        // Company Header
        var y = 45f
        canvas.drawText(settings.companyName, 595f / 2, y, headerPaint)
        y += 16f
        canvas.drawText("${settings.activityType} - هاتف: ${settings.phone}", 595f / 2, y, subHeaderPaint)
        y += 14f
        canvas.drawText("الرقم الضريبي: ${settings.taxNumber} | العنوان: ${settings.address}", 595f / 2, y, subHeaderPaint)

        y += 20f
        canvas.drawLine(30f, y, 565f, y, borderPaint)

        // Invoice Title Badge
        y += 25f
        val invoiceTypeStr = when (invoice.invoiceType) {
            "SALE" -> "فاتورة مبيعات ضريبية"
            "PURCHASE" -> "فاتورة مشتريات"
            "SALE_RETURN" -> "فاتورة مردودات مبيعات"
            "PURCHASE_RETURN" -> "فاتورة مردودات مشتريات"
            else -> "فاتورة"
        }
        val titlePaint = Paint().apply {
            color = Color.rgb(30, 58, 138)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(invoiceTypeStr, 595f / 2, y, titlePaint)

        // Meta Box
        y += 20f
        canvas.drawRoundRect(30f, y, 565f, y + 65f, 6f, 6f, borderPaint)
        val boxY = y + 18f
        canvas.drawText("رقم الفاتورة: ${invoice.invoiceNumber}", 550f, boxY, boldTextPaint)
        canvas.drawText("العميل / الحساب: ${invoice.accountName}", 550f, boxY + 20f, textPaint)
        canvas.drawText("طريقة الدفع: ${if (invoice.paymentType == "CASH") "نقداً" else if (invoice.paymentType == "CREDIT") "آجل" else "شبكة"}", 550f, boxY + 40f, textPaint)

        canvas.drawText("التاريخ: ${dateFormat.format(Date(invoice.date))} ${invoice.timeStr}", 250f, boxY, textPaint)
        canvas.drawText("المستخدم: ${invoice.createdBy}", 250f, boxY + 20f, textPaint)
        canvas.drawText("العملة: ${invoice.currency}", 250f, boxY + 40f, textPaint)

        // Table Header
        y += 85f
        canvas.drawRect(30f, y, 565f, y + 24f, tableHeaderPaint)
        canvas.drawRect(30f, y, 565f, y + 24f, borderPaint)

        val tableHeaderCol = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("م", 550f, y + 16f, tableHeaderCol)
        canvas.drawText("اسم الصنف والبيان", 380f, y + 16f, tableHeaderCol)
        canvas.drawText("الوحدة", 260f, y + 16f, tableHeaderCol)
        canvas.drawText("الكمية", 200f, y + 16f, tableHeaderCol)
        canvas.drawText("السعر", 130f, y + 16f, tableHeaderCol)
        canvas.drawText("الإجمالي", 60f, y + 16f, tableHeaderCol)

        // Items
        y += 24f
        var index = 1
        for (item in items) {
            canvas.drawRect(30f, y, 565f, y + 22f, borderPaint)
            val centerCol = Paint().apply {
                color = Color.rgb(51, 65, 85)
                textSize = 9.5f
                textAlign = Paint.Align.CENTER
            }
            val rightCol = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 9.5f
                textAlign = Paint.Align.RIGHT
            }

            canvas.drawText("$index", 550f, y + 15f, centerCol)
            canvas.drawText(item.productName, 470f, y + 15f, rightCol)
            canvas.drawText(item.unitName, 260f, y + 15f, centerCol)
            canvas.drawText(String.format(Locale.US, "%.1f", item.quantity), 200f, y + 15f, centerCol)
            canvas.drawText(String.format(Locale.US, "%.2f", item.unitPrice), 130f, y + 15f, centerCol)
            canvas.drawText(String.format(Locale.US, "%.2f", item.total), 60f, y + 15f, centerCol)

            y += 22f
            index++
        }

        // Totals Box
        y += 15f
        val totalsBoxLeft = 320f
        canvas.drawRoundRect(totalsBoxLeft, y, 565f, y + 90f, 6f, 6f, borderPaint)
        var totY = y + 18f

        canvas.drawText("الإجمالي قبل الخصم: ${String.format(Locale.US, "%.2f", invoice.subtotal)} ${invoice.currency}", 550f, totY, textPaint)
        totY += 18f
        if (invoice.discount > 0) {
            canvas.drawText("الخصم الممنوح: ${String.format(Locale.US, "%.2f", invoice.discount)} ${invoice.currency}", 550f, totY, textPaint)
            totY += 18f
        }
        canvas.drawText("الصافي المستحق: ${String.format(Locale.US, "%.2f", invoice.grandTotal)} ${invoice.currency}", 550f, totY, boldTextPaint)
        totY += 18f
        canvas.drawText("المبلغ المدفوع: ${String.format(Locale.US, "%.2f", invoice.paidAmount)} | المتبقي: ${String.format(Locale.US, "%.2f", invoice.remainingAmount)}", 550f, totY, textPaint)

        // Notes and Footer
        val footerY = 790f
        canvas.drawLine(30f, footerY - 20f, 565f, footerY - 20f, borderPaint)
        canvas.drawText(settings.invoiceFooter, 595f / 2, footerY - 5f, subHeaderPaint)
        canvas.drawText("تم إنشاء هذا المستند بواسطة تطبيق المحاسب الذكي - نظام غير متصل بالإنترنت", 595f / 2, footerY + 12f, subHeaderPaint)
    }

    private fun drawStatementContent(
        canvas: Canvas,
        account: Account,
        transactions: List<AccountTransaction>,
        settings: CompanySettings
    ) {
        val headerPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subHeaderPaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }
        val textPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
            textAlign = Paint.Align.RIGHT
        }
        val boldTextPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val borderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val tableHeaderPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }

        var y = 45f
        canvas.drawText(settings.companyName, 595f / 2, y, headerPaint)
        y += 16f
        canvas.drawText("كشف حساب مالي تفصيلي", 595f / 2, y, Paint().apply {
            color = Color.rgb(37, 99, 235)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        })

        y += 25f
        canvas.drawRoundRect(30f, y, 565f, y + 50f, 6f, 6f, borderPaint)
        canvas.drawText("اسم الحساب: ${account.name} (${account.accountCode})", 550f, y + 20f, boldTextPaint)
        canvas.drawText("الهاتف: ${account.phone.ifEmpty { "غير محدد" }}", 550f, y + 38f, textPaint)
        canvas.drawText("الرصيد الحالي: ${String.format(Locale.US, "%.2f", account.currentBalance)} ${settings.baseCurrencySymbol}", 250f, y + 20f, boldTextPaint)
        canvas.drawText("تاريخ الكشف: ${dateFormat.format(Date())}", 250f, y + 38f, textPaint)

        y += 70f
        canvas.drawRect(30f, y, 565f, y + 24f, tableHeaderPaint)
        canvas.drawRect(30f, y, 565f, y + 24f, borderPaint)

        val colPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("التاريخ", 530f, y + 16f, colPaint)
        canvas.drawText("المستند", 450f, y + 16f, colPaint)
        canvas.drawText("البيان", 320f, y + 16f, colPaint)
        canvas.drawText("مدين (+)", 190f, y + 16f, colPaint)
        canvas.drawText("دائن (-)", 120f, y + 16f, colPaint)
        canvas.drawText("الرصيد", 55f, y + 16f, colPaint)

        y += 24f
        var totalDebit = 0.0
        var totalCredit = 0.0

        for (tx in transactions) {
            totalDebit += tx.debit
            totalCredit += tx.credit

            canvas.drawRect(30f, y, 565f, y + 20f, borderPaint)
            val valPaint = Paint().apply {
                color = Color.rgb(51, 65, 85)
                textSize = 8.5f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(dateFormat.format(Date(tx.date)), 530f, y + 14f, valPaint)
            canvas.drawText(tx.documentNumber, 450f, y + 14f, valPaint)
            canvas.drawText(tx.statement.take(24), 320f, y + 14f, valPaint)
            canvas.drawText(if (tx.debit > 0) String.format(Locale.US, "%.1f", tx.debit) else "-", 190f, y + 14f, valPaint)
            canvas.drawText(if (tx.credit > 0) String.format(Locale.US, "%.1f", tx.credit) else "-", 120f, y + 14f, valPaint)
            canvas.drawText(String.format(Locale.US, "%.1f", tx.balanceAfter), 55f, y + 14f, valPaint)

            y += 20f
        }

        // Summary Row
        canvas.drawRect(30f, y, 565f, y + 25f, tableHeaderPaint)
        canvas.drawRect(30f, y, 565f, y + 25f, borderPaint)
        canvas.drawText("الإجماليات", 320f, y + 17f, colPaint)
        canvas.drawText(String.format(Locale.US, "%.2f", totalDebit), 190f, y + 17f, colPaint)
        canvas.drawText(String.format(Locale.US, "%.2f", totalCredit), 120f, y + 17f, colPaint)
        canvas.drawText(String.format(Locale.US, "%.2f", account.currentBalance), 55f, y + 17f, colPaint)
    }

    private fun drawVoucherContent(
        canvas: Canvas,
        voucher: Voucher,
        settings: CompanySettings
    ) {
        val titleStr = if (voucher.type == "RECEIPT") "سند قبض مالي" else "سند صرف مالي"
        val borderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 11f
            textAlign = Paint.Align.RIGHT
        }
        val boldPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        // Draw Voucher Frame
        canvas.drawRoundRect(20f, 20f, 575f, 401f, 8f, 8f, borderPaint)
        canvas.drawText(settings.companyName, 595f / 2, 45f, headerPaint)
        canvas.drawText(titleStr, 595f / 2, 68f, Paint().apply {
            color = if (voucher.type == "RECEIPT") Color.rgb(5, 150, 105) else Color.rgb(220, 38, 38)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        })

        var y = 110f
        canvas.drawText("رقم السند: ${voucher.voucherNumber}", 550f, y, boldPaint)
        canvas.drawText("التاريخ: ${dateFormat.format(Date(voucher.date))}", 250f, y, textPaint)

        y += 35f
        val partyLabel = if (voucher.type == "RECEIPT") "استلمنا من الأخ/السادة:" else "صرفنا للأخ/السادة:"
        canvas.drawText("$partyLabel ${voucher.accountName}", 550f, y, boldPaint)

        y += 35f
        canvas.drawText("مبلغ وقدره: ${String.format(Locale.US, "%.2f", voucher.amount)} ${voucher.currency}", 550f, y, boldPaint)

        y += 35f
        canvas.drawText("وذلك عن: ${voucher.statement}", 550f, y, textPaint)

        y += 35f
        canvas.drawText("الحساب المالي: ${voucher.cashAccountName}", 550f, y, textPaint)

        y += 60f
        canvas.drawText("المستلم: _________________", 550f, y, textPaint)
        canvas.drawText("أمين الصندوق: _________________", 320f, y, textPaint)
        canvas.drawText("المدير المالي: _________________", 140f, y, textPaint)
    }

    fun getShareableUri(file: File): android.net.Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}

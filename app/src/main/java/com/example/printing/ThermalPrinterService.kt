package com.example.printing

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.example.data.local.entities.CompanySettings
import com.example.data.local.entities.Invoice
import com.example.data.local.entities.InvoiceItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ThermalPrinterService(private val context: Context) {

    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    // ESC/POS Commands
    private val ESC: Byte = 0x1B
    private val GS: Byte = 0x1D
    private val CMD_INIT = byteArrayOf(ESC, 0x40)
    private val CMD_ALIGN_CENTER = byteArrayOf(ESC, 0x61, 0x01)
    private val CMD_ALIGN_LEFT = byteArrayOf(ESC, 0x61, 0x00)
    private val CMD_ALIGN_RIGHT = byteArrayOf(ESC, 0x61, 0x02)
    private val CMD_BOLD_ON = byteArrayOf(ESC, 0x45, 0x01)
    private val CMD_BOLD_OFF = byteArrayOf(ESC, 0x45, 0x00)
    private val CMD_DOUBLE_HEIGHT = byteArrayOf(GS, 0x21, 0x01)
    private val CMD_NORMAL_SIZE = byteArrayOf(GS, 0x21, 0x00)
    private val CMD_CUT = byteArrayOf(GS, 0x56, 0x41, 0x10) // Cut paper
    private val CMD_FEED_3 = byteArrayOf(ESC, 0x64, 0x03)

    /**
     * Builds standard thermal receipt text for display / preview in UI.
     */
    fun buildThermalReceiptPreview(
        invoice: Invoice,
        items: List<InvoiceItem>,
        settings: CompanySettings
    ): String {
        val width = if (settings.thermalPaperWidth == 58) 32 else 48
        val divider = "=".repeat(width)
        val subDivider = "-".repeat(width)

        val sb = StringBuilder()
        sb.appendLine(centerText(settings.companyName, width))
        sb.appendLine(centerText(settings.activityType, width))
        sb.appendLine(centerText("هاتف: ${settings.phone}", width))
        if (settings.taxNumber.isNotEmpty()) {
            sb.appendLine(centerText("الرقم الضريبي: ${settings.taxNumber}", width))
        }
        sb.appendLine(divider)
        val typeTitle = when (invoice.invoiceType) {
            "SALE" -> "فاتورة مبيعات نقدية/آجلة"
            "PURCHASE" -> "فاتورة مشتريات"
            "SALE_RETURN" -> "فاتورة مردودات مبيعات"
            else -> "فاتورة مردودات مشتريات"
        }
        sb.appendLine(centerText(typeTitle, width))
        sb.appendLine(divider)

        sb.appendLine("رقم الفاتورة: ${invoice.invoiceNumber}")
        sb.appendLine("التاريخ: ${dateFormat.format(Date(invoice.date))}")
        sb.appendLine("العميل: ${invoice.accountName}")
        sb.appendLine("طريقة الدفع: ${if (invoice.paymentType == "CASH") "نقداً" else if (invoice.paymentType == "CREDIT") "آجل" else "شبكة"}")
        sb.appendLine("المستخدم: ${invoice.createdBy}")
        sb.appendLine(subDivider)

        // Column Header
        if (width == 32) {
            sb.appendLine(formatRow("الصنف", "الكمية", "السعر", "الإجمالي", 32))
        } else {
            sb.appendLine(formatRowWide("الصنف والوحدة", "الكمية", "السعر", "الإجمالي", 48))
        }
        sb.appendLine(subDivider)

        for (item in items) {
            val nameAndUnit = "${item.productName} (${item.unitName})"
            val qtyStr = String.format(Locale.US, "%.1f", item.quantity)
            val priceStr = String.format(Locale.US, "%.1f", item.unitPrice)
            val totalStr = String.format(Locale.US, "%.1f", item.total)

            if (width == 32) {
                sb.appendLine(item.productName.take(30))
                sb.appendLine("  $qtyStr x $priceStr = $totalStr")
            } else {
                sb.appendLine(formatRowWide(nameAndUnit, qtyStr, priceStr, totalStr, 48))
            }
        }

        sb.appendLine(subDivider)
        sb.appendLine(formatKeyValue("المجموع:", String.format(Locale.US, "%.2f %s", invoice.subtotal, invoice.currency), width))
        if (invoice.discount > 0) {
            sb.appendLine(formatKeyValue("الخصم:", String.format(Locale.US, "%.2f %s", invoice.discount, invoice.currency), width))
        }
        if (invoice.tax > 0) {
            sb.appendLine(formatKeyValue("الضريبة:", String.format(Locale.US, "%.2f %s", invoice.tax, invoice.currency), width))
        }
        sb.appendLine(formatKeyValue("الصافي المطلوب:", String.format(Locale.US, "%.2f %s", invoice.grandTotal, invoice.currency), width))
        sb.appendLine(formatKeyValue("المدفوع:", String.format(Locale.US, "%.2f %s", invoice.paidAmount, invoice.currency), width))
        sb.appendLine(formatKeyValue("المتبقي:", String.format(Locale.US, "%.2f %s", invoice.remainingAmount, invoice.currency), width))
        sb.appendLine(divider)

        if (settings.invoiceFooter.isNotEmpty()) {
            sb.appendLine(centerText(settings.invoiceFooter, width))
        }
        sb.appendLine(centerText("المحاسب الذكي - نظام غير متصل بالإنترنت", width))
        return sb.toString()
    }

    /**
     * Sends ESC/POS commands to network thermal printer.
     */
    suspend fun printOverNetwork(
        ipAddress: String,
        port: Int = 9100,
        content: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ipAddress, port), 4000)
                socket.getOutputStream().use { out ->
                    out.write(CMD_INIT)
                    out.write(content.toByteArray(charset("CP864"))) // Arabic code page or UTF-8
                    out.write(CMD_FEED_3)
                    out.write(CMD_CUT)
                    out.flush()
                }
            }
            true
        }
    }

    /**
     * Sends ESC/POS commands to paired Bluetooth thermal printer.
     */
    suspend fun printOverBluetooth(
        deviceAddress: String,
        content: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: error("Bluetooth غير متوفر")
            val device = adapter.getRemoteDevice(deviceAddress)
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            socket.outputStream.use { out ->
                out.write(CMD_INIT)
                out.write(content.toByteArray(charset("UTF-8")))
                out.write(CMD_FEED_3)
                out.write(CMD_CUT)
                out.flush()
            }
            socket.close()
            true
        }
    }

    private fun centerText(text: String, width: Int): String {
        if (text.length >= width) return text.take(width)
        val pad = (width - text.length) / 2
        return " ".repeat(pad) + text
    }

    private fun formatKeyValue(key: String, value: String, width: Int): String {
        val spaces = width - key.length - value.length
        return if (spaces > 0) key + " ".repeat(spaces) + value else "$key: $value"
    }

    private fun formatRow(col1: String, col2: String, col3: String, col4: String, width: Int): String {
        return String.format("%-10s %5s %7s %8s", col1.take(10), col2.take(5), col3.take(7), col4.take(8))
    }

    private fun formatRowWide(col1: String, col2: String, col3: String, col4: String, width: Int): String {
        return String.format("%-20s %6s %9s %11s", col1.take(20), col2.take(6), col3.take(9), col4.take(11))
    }
}

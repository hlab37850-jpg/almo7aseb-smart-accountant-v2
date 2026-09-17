package com.example.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entities.CompanySettings
import com.example.presentation.viewmodels.MainViewModel
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val currentSettings by viewModel.settings.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()

    var companyName by remember(currentSettings) { mutableStateOf(currentSettings.companyName) }
    var activityType by remember(currentSettings) { mutableStateOf(currentSettings.activityType) }
    var phone by remember(currentSettings) { mutableStateOf(currentSettings.phone) }
    var taxNumber by remember(currentSettings) { mutableStateOf(currentSettings.taxNumber) }
    var address by remember(currentSettings) { mutableStateOf(currentSettings.address) }
    var currencySymbol by remember(currentSettings) { mutableStateOf(currentSettings.baseCurrencySymbol) }
    var thermalWidth by remember(currentSettings) { mutableStateOf(currentSettings.thermalPaperWidth) }

    var lastBackupFile by remember { mutableStateOf<File?>(null) }
    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Company Information Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("بيانات المنشأة والفواتير", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)

                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("اسم المؤسسة / المتجر") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = activityType,
                        onValueChange = { activityType = it },
                        label = { Text("نوع النشاط التجاري") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("رقم الهاتف") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = taxNumber,
                            onValueChange = { taxNumber = it },
                            label = { Text("الرقم الضريبي") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("العنوان") },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = currencySymbol,
                            onValueChange = { currencySymbol = it },
                            label = { Text("رمز العملة") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.updateSettings(
                                currentSettings.copy(
                                    companyName = companyName,
                                    activityType = activityType,
                                    phone = phone,
                                    taxNumber = taxNumber,
                                    address = address,
                                    baseCurrencySymbol = currencySymbol,
                                    thermalPaperWidth = thermalWidth
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ التعديلات")
                    }
                }
            }
        }

        // Printer Options
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("إعدادات الطباعة الحرارية (ESC/POS)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Text("عرض ورق الإيصالات الحرارية:", fontSize = 12.sp, color = TextSecondary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = thermalWidth == 58,
                            onClick = { thermalWidth = 58 },
                            label = { Text("58 ملم (قياسي صغير)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = thermalWidth == 80,
                            onClick = { thermalWidth = 80 },
                            label = { Text("80 ملم (كبير نقط بيع)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Backup and Data Integrity
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("النسخ الاحتياطي وسلامة البيانات", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.createBackup { file ->
                                    lastBackupFile = file
                                    viewModel.sharePdf(file, context, "نسخة احتياطية لتطبيق المحاسب الذكي")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ احتياطي", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                if (lastBackupFile != null && lastBackupFile!!.exists()) {
                                    viewModel.restoreBackup(lastBackupFile!!) {}
                                } else {
                                    viewModel.postMessage("يرجى عمل نسخة احتياطية أولاً أو اختيار ملف")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استعادة نسخة", fontSize = 12.sp)
                        }
                    }

                    Divider()

                    // Integrity check button
                    OutlinedButton(
                        onClick = { viewModel.recalculateBalances() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إعادة احتساب وتحديث مطابقة الأرصدة (Integrity Check)")
                    }
                }
            }
        }

        // Audit Logs (سجل العمليات والرقابة)
        item {
            Text("سجل التدقيق والرقابة (Audit Log)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
        }

        items(auditLogs.take(15)) { log ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                elevation = CardDefaults.cardElevation(0.5.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(log.action, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BluePrimary)
                        Text(dateFormat.format(Date(log.timestamp)), fontSize = 10.sp, color = TextMuted)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(log.details, fontSize = 11.sp, color = TextSecondary)
                    Text("المستخدم: ${log.user}", fontSize = 10.sp, color = TextMuted)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

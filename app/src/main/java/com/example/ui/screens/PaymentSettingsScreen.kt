package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StorePaymentConfig
import com.example.ui.theme.SotraError
import com.example.ui.theme.SotraPrimary
import com.example.ui.theme.SotraSuccess
import com.example.viewmodel.OrdersViewModel

@Composable
fun PaymentSettingsScreen(
    viewModel: OrdersViewModel,
    modifier: Modifier = Modifier
) {
    val currentConfig by viewModel.paymentConfig.collectAsState()

    var vcEnabled by remember { mutableStateOf(currentConfig.vodafoneCashEnabled) }
    var vcNumber by remember { mutableStateOf(currentConfig.vodafoneCashNumber) }
    var vcName by remember { mutableStateOf(currentConfig.vodafoneCashAccountName) }
    var vcInstructions by remember { mutableStateOf(currentConfig.vodafoneCashInstructionsAr) }

    var ipEnabled by remember { mutableStateOf(currentConfig.instaPayEnabled) }
    var ipId by remember { mutableStateOf(currentConfig.instaPayId) }
    var ipName by remember { mutableStateOf(currentConfig.instaPayAccountName) }
    var ipInstructions by remember { mutableStateOf(currentConfig.instaPayInstructionsAr) }

    var isSaving by remember { mutableStateOf(false) }

    // Keep fields updated if initial remote fetch completes
    var hasLoadedInitial by remember { mutableStateOf(false) }
    LaunchedEffect(currentConfig) {
        if (!hasLoadedInitial) {
            vcEnabled = currentConfig.vodafoneCashEnabled
            vcNumber = currentConfig.vodafoneCashNumber
            vcName = currentConfig.vodafoneCashAccountName
            vcInstructions = currentConfig.vodafoneCashInstructionsAr
            ipEnabled = currentConfig.instaPayEnabled
            ipId = currentConfig.instaPayId
            ipName = currentConfig.instaPayAccountName
            ipInstructions = currentConfig.instaPayInstructionsAr
            hasLoadedInitial = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Info Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SotraPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SyncAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        text = "مزامنة فورية لأرقام الدفع",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1E3A8A)
                    )
                    Text(
                        text = "الأرقام المحفوظة تظهر للعميل فوراً عند اختيار الدفع بمتجر سترة فاشون.",
                        fontSize = 12.sp,
                        color = Color(0xFF3B82F6)
                    )
                }
            }
        }

        // Vodafone Cash Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = SotraError, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "فودافون كاش (Vodafone Cash)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = SotraError
                            )
                            Text(
                                text = if (vcEnabled) "مفعل ومتاح في المتجر" else "معطل حالياً",
                                fontSize = 11.sp,
                                color = if (vcEnabled) SotraSuccess else Color.Gray
                            )
                        }
                    }
                    Switch(
                        checked = vcEnabled,
                        onCheckedChange = { vcEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SotraError)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = vcNumber,
                    onValueChange = { vcNumber = it },
                    label = { Text("رقم محفظة فودافون كاش للتحويل") },
                    placeholder = { Text("مثال: 01012345678") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(Icons.Default.Dialpad, contentDescription = null, tint = SotraError) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFFAFAFA),
                        unfocusedContainerColor = Color(0xFFFAFAFA),
                        focusedBorderColor = SotraError
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = vcName,
                    onValueChange = { vcName = it },
                    label = { Text("اسم صاحب الحساب أو المحفظة (اختياري)") },
                    placeholder = { Text("سترة فاشون") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFFAFAFA),
                        unfocusedContainerColor = Color(0xFFFAFAFA),
                        focusedBorderColor = SotraError
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = vcInstructions,
                    onValueChange = { vcInstructions = it },
                    label = { Text("تعليمات الدفع للعميل بصفحة الطلب") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFFAFAFA),
                        unfocusedContainerColor = Color(0xFFFAFAFA),
                        focusedBorderColor = SotraError
                    )
                )
            }
        }

        // InstaPay Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEEF2FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "إنستاباي (InstaPay)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF4F46E5)
                            )
                            Text(
                                text = if (ipEnabled) "مفعل ومتاح في المتجر" else "معطل حالياً",
                                fontSize = 11.sp,
                                color = if (ipEnabled) SotraSuccess else Color.Gray
                            )
                        }
                    }
                    Switch(
                        checked = ipEnabled,
                        onCheckedChange = { ipEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF4F46E5))
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = ipId,
                    onValueChange = { ipId = it },
                    label = { Text("عنوان الدفع اللحظي (IPA) أو رقم الحساب") },
                    placeholder = { Text("مثال: sotra.fashion@instapay أو 01012345678") },
                    leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = Color(0xFF4F46E5)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFFAFAFA),
                        unfocusedContainerColor = Color(0xFFFAFAFA),
                        focusedBorderColor = Color(0xFF4F46E5)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = ipName,
                    onValueChange = { ipName = it },
                    label = { Text("اسم صاحب حساب إنستاباي (اختياري)") },
                    placeholder = { Text("سترة فاشون") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFFAFAFA),
                        unfocusedContainerColor = Color(0xFFFAFAFA),
                        focusedBorderColor = Color(0xFF4F46E5)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = ipInstructions,
                    onValueChange = { ipInstructions = it },
                    label = { Text("تعليمات الدفع عبر إنستاباي") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFFAFAFA),
                        unfocusedContainerColor = Color(0xFFFAFAFA),
                        focusedBorderColor = Color(0xFF4F46E5)
                    )
                )
            }
        }

        // Save Button
        Button(
            onClick = {
                isSaving = true
                viewModel.savePaymentConfig(
                    StorePaymentConfig(
                        vodafoneCashEnabled = vcEnabled,
                        vodafoneCashNumber = vcNumber.trim(),
                        vodafoneCashAccountName = vcName.trim(),
                        vodafoneCashInstructionsAr = vcInstructions.trim(),
                        instaPayEnabled = ipEnabled,
                        instaPayId = ipId.trim(),
                        instaPayAccountName = ipName.trim(),
                        instaPayInstructionsAr = ipInstructions.trim(),
                        advanceShippingFeeOnly = true
                    )
                )
                isSaving = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SotraPrimary)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("حفظ وتحديث أرقام الدفع فورياً", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

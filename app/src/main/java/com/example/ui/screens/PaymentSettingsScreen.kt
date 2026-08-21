package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StorePaymentConfig
import com.example.ui.theme.SotraError
import com.example.ui.theme.SotraPrimary
import com.example.viewmodel.OrdersViewModel

@Composable
fun PaymentSettingsScreen(
    viewModel: OrdersViewModel,
    modifier: Modifier = Modifier
) {
    val currentConfig by viewModel.paymentConfig.collectAsState()

    var vcEnabled by remember(currentConfig) { mutableStateOf(currentConfig.vodafoneCashEnabled) }
    var vcNumber by remember(currentConfig) { mutableStateOf(currentConfig.vodafoneCashNumber) }
    var vcName by remember(currentConfig) { mutableStateOf(currentConfig.vodafoneCashAccountName) }
    var vcInstructions by remember(currentConfig) { mutableStateOf(currentConfig.vodafoneCashInstructionsAr) }

    var ipEnabled by remember(currentConfig) { mutableStateOf(currentConfig.instaPayEnabled) }
    var ipId by remember(currentConfig) { mutableStateOf(currentConfig.instaPayId) }
    var ipName by remember(currentConfig) { mutableStateOf(currentConfig.instaPayAccountName) }
    var ipInstructions by remember(currentConfig) { mutableStateOf(currentConfig.instaPayInstructionsAr) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "إعدادات الدفع وتأكيدات التحويل",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        // Vodafone Cash Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = SotraError)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "فودافون كاش (Vodafone Cash)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = SotraError
                        )
                    }
                    Switch(
                        checked = vcEnabled,
                        onCheckedChange = { vcEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SotraError)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = vcNumber,
                    onValueChange = { vcNumber = it },
                    label = { Text("رقم المحفظة (التحويل إليه)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = vcName,
                    onValueChange = { vcName = it },
                    label = { Text("اسم صاحب الحساب (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = vcInstructions,
                    onValueChange = { vcInstructions = it },
                    label = { Text("تعليمات الدفع للعميل") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }
        }

        // InstaPay Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF4F46E5))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إنستاباي (InstaPay)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF4F46E5)
                        )
                    }
                    Switch(
                        checked = ipEnabled,
                        onCheckedChange = { ipEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF4F46E5))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = ipId,
                    onValueChange = { ipId = it },
                    label = { Text("عنوان الدفع (IPA)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = ipName,
                    onValueChange = { ipName = it },
                    label = { Text("اسم صاحب الحساب (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = ipInstructions,
                    onValueChange = { ipInstructions = it },
                    label = { Text("تعليمات الدفع للعميل") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }
        }

        // Save Button
        Button(
            onClick = {
                viewModel.savePaymentConfig(
                    StorePaymentConfig(
                        vodafoneCashEnabled = vcEnabled,
                        vodafoneCashNumber = vcNumber,
                        vodafoneCashAccountName = vcName,
                        vodafoneCashInstructionsAr = vcInstructions,
                        instaPayEnabled = ipEnabled,
                        instaPayId = ipId,
                        instaPayAccountName = ipName,
                        instaPayInstructionsAr = ipInstructions,
                        advanceShippingFeeOnly = true
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 80.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SotraPrimary)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("حفظ إعدادات الدفع", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

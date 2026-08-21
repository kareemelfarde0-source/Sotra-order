package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.model.GovernorateShipping
import com.example.ui.theme.SotraError
import com.example.ui.theme.SotraPrimary
import com.example.ui.theme.SotraSuccess
import com.example.viewmodel.OrdersViewModel

@Composable
fun ShippingSettingsScreen(
    viewModel: OrdersViewModel,
    modifier: Modifier = Modifier
) {
    val governorates by viewModel.governorates.collectAsState()
    var editableList by remember(governorates) { mutableStateOf(governorates) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "تسعير الشحن للمحافظات (${editableList.size})",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = SotraSuccess),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة محافظة", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(editableList, key = { _, item -> item.id }) { index, gov ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = gov.nameAr,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "مدة التوصيل: ${gov.deliveryDays}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = gov.shippingCost.toString(),
                                onValueChange = { input ->
                                    val newCost = input.toIntOrNull() ?: 0
                                    editableList = editableList.toMutableList().also {
                                        it[index] = it[index].copy(shippingCost = newCost)
                                    }
                                },
                                modifier = Modifier.width(90.dp),
                                singleLine = true,
                                trailingIcon = { Text("ج.م", fontSize = 11.sp, color = Color.Gray) },
                                shape = RoundedCornerShape(8.dp)
                            )

                            IconButton(
                                onClick = {
                                    editableList = editableList.filterIndexed { i, _ -> i != index }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = SotraError)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Save All Button
        Button(
            onClick = {
                viewModel.saveGovernorates(editableList)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 70.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SotraPrimary)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("حفظ أسعار الشحن", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (showAddDialog) {
        var newId by remember { mutableStateOf("") }
        var newName by remember { mutableStateOf("") }
        var newCost by remember { mutableStateOf("50") }
        var newDays by remember { mutableStateOf("2-3 أيام") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة محافظة جديدة", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = {
                            newName = it
                            if (newId.isBlank()) newId = it.take(4).lowercase()
                        },
                        label = { Text("اسم المحافظة (عربي)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCost,
                        onValueChange = { newCost = it },
                        label = { Text("سعر الشحن (ج.م)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDays,
                        onValueChange = { newDays = it },
                        label = { Text("مدة التوصيل") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            val gov = GovernorateShipping(
                                id = newId.ifBlank { UUIDRandom() },
                                nameAr = newName,
                                nameEn = newName,
                                shippingCost = newCost.toIntOrNull() ?: 50,
                                deliveryDays = newDays
                            )
                            editableList = listOf(gov) + editableList
                            viewModel.addGovernorate(gov)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SotraSuccess)
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

private fun UUIDRandom(): String = "gov_" + (1000..9999).random()

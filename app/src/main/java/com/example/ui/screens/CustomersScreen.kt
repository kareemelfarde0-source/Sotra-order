package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Customer
import com.example.ui.components.formatArabicDate
import com.example.ui.theme.SotraPrimary
import com.example.ui.theme.SotraPrimaryLight
import com.example.ui.theme.SotraSuccess
import com.example.viewmodel.OrdersViewModel
import java.net.URLEncoder

@Composable
fun CustomersScreen(
    viewModel: OrdersViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customers by viewModel.filteredCustomers.collectAsState()
    val searchQuery by viewModel.customersSearchQuery.collectAsState()

    val totalSpend = customers.sumOf { it.totalSpent }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Summary Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("إجمالي عدد العملاء", fontSize = 12.sp, color = Color(0xFF64748B))
                    Text("${customers.size} عميل", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                }
                VerticalDivider(modifier = Modifier.height(36.dp), color = Color(0xFFE2E8F0))
                Column {
                    Text("إجمالي المبيعات للعملاء", fontSize = 12.sp, color = Color(0xFF64748B))
                    Text("${totalSpend.toInt()} ج.م", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SotraSuccess)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setCustomersSearchQuery(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("بحث باسم العميل أو رقم الهاتف أو المحافظة...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "بحث", tint = Color.Gray)
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = SotraPrimary,
                unfocusedBorderColor = Color(0xFFE2E8F0)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (customers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("لا يوجد عملاء مسجلين مطابقين للبحث", color = Color.Gray, fontWeight = FontWeight.Medium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(customers, key = { it.phoneNumber }) { customer ->
                    CustomerCard(customer = customer, onCall = { dialPhone(context, it) }, onWhatsApp = { openWhatsApp(context, it) })
                }
            }
        }
    }
}

@Composable
private fun CustomerCard(
    customer: Customer,
    onCall: (String) -> Unit,
    onWhatsApp: (Customer) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SotraPrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = customer.fullName.take(1).ifBlank { "ع" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SotraPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = customer.fullName.ifBlank { "عميل" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = customer.phoneNumber,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Total Orders Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${customer.totalOrdersCount} طلبات",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Address & Spend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${customer.governorateNameAr} • ${customer.detailedAddress}",
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "إجمالي: ${customer.totalSpent.toInt()} ج.م",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SotraSuccess
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "آخر طلب: ${formatArabicDate(customer.lastOrderDate)}",
                fontSize = 11.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons (Call / WhatsApp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onCall(customer.phoneNumber) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("اتصال", fontSize = 12.sp)
                }

                Button(
                    onClick = { onWhatsApp(customer) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("واتساب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun dialPhone(context: Context, phone: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Ignored
    }
}

private fun openWhatsApp(context: Context, customer: Customer) {
    val phone = customer.phoneNumber.replace("+", "").replace(" ", "").trim()
    val formatted = if (phone.startsWith("0")) "20" + phone.substring(1) else "20$phone"
    val msg = URLEncoder.encode("مرحباً أستاذ/ة ${customer.fullName}، معكم إدارة متجر سترة فاشون...", "UTF-8")
    val url = "https://wa.me/$formatted?text=$msg"
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback
    }
}

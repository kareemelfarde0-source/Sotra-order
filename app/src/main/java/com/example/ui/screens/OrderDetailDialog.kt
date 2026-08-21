package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.ui.components.PaymentMethodBadge
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatArabicDate
import com.example.ui.theme.*
import com.example.viewmodel.OrdersViewModel
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailDialog(
    order: Order,
    viewModel: OrdersViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedStatus by remember(order) { mutableStateOf(order.trackingStatus) }
    var isStatusMenuExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Modal Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "تفاصيل الطلب #${order.orderId}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = SotraPrimaryDark,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = formatArabicDate(order.createdAt),
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", modifier = Modifier.size(20.dp))
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Acknowledge Action (if unacknowledged or ringing)
                    if (!order.isAcknowledged) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, SotraError),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = SotraError)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "طلب جديد! اضغط لتأكيد الاستلام وإيقاف الرنين",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SotraError
                                    )
                                }

                                Button(
                                    onClick = { viewModel.acknowledgeOrder(order.orderId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SotraSuccess),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("تم الاستلام", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 1. Customer Information Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = SotraPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("بيانات العميل والتوصيل", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))

                            InfoRow(label = "الاسم الكامل", value = order.customer.fullName)
                            InfoRow(label = "رقم الهاتف", value = order.customer.phoneNumber)
                            if (order.customer.secondaryPhone.isNotBlank()) {
                                InfoRow(label = "هاتف بديل", value = order.customer.secondaryPhone)
                            }
                            InfoRow(label = "المحافظة", value = order.customer.governorateNameAr)
                            InfoRow(label = "العنوان التفصيلي", value = order.customer.detailedAddress)

                            if (order.customer.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFEF3C7))
                                        .padding(10.dp)
                                ) {
                                    Row {
                                        Icon(Icons.Default.EditNote, contentDescription = null, tint = Color(0xFFB45309))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "ملاحظات العميل: ${order.customer.notes}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Quick Contact Action Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { dialDirectPhone(context, order.customer.phoneNumber) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("اتصال هاتفي", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { launchWhatsAppMessage(context, order) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مراسلة واتساب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 2. Payment & Verification Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Payment, contentDescription = null, tint = SotraPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("بيانات الدفع والحالة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                StatusBadge(status = order.trackingStatus)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("طريقة الدفع:", fontSize = 13.sp, color = Color.Gray)
                                PaymentMethodBadge(method = order.paymentMethod)
                            }

                            if (!order.senderPhoneOrInstaPayId.isNullOrBlank() || !order.transactionReference.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEEF2FF))
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("معلومات تأكيد التحويل:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF3730A3))
                                        if (!order.senderPhoneOrInstaPayId.isNullOrBlank()) {
                                            Text("• رقم المحول / الحساب: ${order.senderPhoneOrInstaPayId}", fontSize = 12.sp, color = Color(0xFF1E1B4B))
                                        }
                                        if (!order.transactionReference.isNullOrBlank()) {
                                            Text("• الرقم المرجعي (Ref): ${order.transactionReference}", fontSize = 12.sp, color = Color(0xFF1E1B4B))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Products Ordered
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = SotraPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("المنتجات المطلوبة (${order.items.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))

                            order.items.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.titleAr, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            text = "اللون: ${item.selectedColor.nameAr.ifBlank { "افتراضي" }} | المقاس: ${item.selectedSize.ifBlank { "ستاندرد" }} | الكمية: ${item.quantity}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    Text(
                                        text = "${(item.price * item.quantity).toInt()} ج.م",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                                HorizontalDivider(color = Color(0xFFF8FAFC))
                            }
                        }
                    }

                    // 4. Financial Calculations Summary
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CalculationRow(label = "المجموع الفرعي", value = "${order.subtotal.toInt()} ج.م")
                            CalculationRow(label = "رسوم الشحن للمحافظة", value = "${order.shippingCost.toInt()} ج.م")
                            if (order.discount > 0) {
                                CalculationRow(
                                    label = "الخصم (${order.appliedCouponCode ?: "كوبون"})",
                                    value = "-${order.discount.toInt()} ج.م",
                                    textColor = SotraSuccess
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFE2E8F0))
                            CalculationRow(
                                label = "الإجمالي الكلي",
                                value = "${order.total.toInt()} ج.م",
                                isBold = true,
                                fontSize = 16.sp
                            )
                            CalculationRow(
                                label = "المدفوع مقدماً (شحن)",
                                value = "${order.advanceShippingPaid.toInt()} ج.م",
                                textColor = SotraPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFDCFCE7))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "المطلوب تحصيله عند الاستلام:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF166534)
                                    )
                                    Text(
                                        text = "${order.remainingUponDelivery.toInt()} ج.م",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp,
                                        color = Color(0xFF166534)
                                    )
                                }
                            }
                        }
                    }

                    // 5. Status Changer Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("تحديث حالة الطلب", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            ExposedDropdownMenuBox(
                                expanded = isStatusMenuExpanded,
                                onExpandedChange = { isStatusMenuExpanded = !isStatusMenuExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedStatus.labelAr,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStatusMenuExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                ExposedDropdownMenu(
                                    expanded = isStatusMenuExpanded,
                                    onDismissRequest = { isStatusMenuExpanded = false }
                                ) {
                                    OrderStatus.entries.forEach { status ->
                                        DropdownMenuItem(
                                            text = { Text(status.labelAr, fontWeight = FontWeight.Medium) },
                                            onClick = {
                                                selectedStatus = status
                                                isStatusMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    viewModel.updateOrderStatus(order.orderId, selectedStatus)
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SotraPrimary)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("حفظ الحالة الجديدة", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Modal Footer
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("إغلاق")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "$label:", fontSize = 13.sp, color = Color(0xFF64748B))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
    }
}

@Composable
private fun CalculationRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    textColor: Color = Color(0xFF1E293B),
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = fontSize,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = Color(0xFF64748B)
        )
        Text(
            text = value,
            fontSize = fontSize,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = textColor
        )
    }
}

private fun dialDirectPhone(context: Context, phone: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Ignored
    }
}

private fun launchWhatsAppMessage(context: Context, order: Order) {
    val phone = order.customer.phoneNumber.replace("+", "").replace(" ", "").trim()
    val formattedPhone = if (phone.startsWith("0")) "20" + phone.substring(1) else if (phone.startsWith("20")) phone else "20$phone"
    val greeting = "مرحباً أستاذ/ة ${order.customer.fullName}، بخصوص طلبك رقم #${order.orderId} من سترة فاشون..."
    val encoded = URLEncoder.encode(greeting, "UTF-8")
    val url = "https://wa.me/$formattedPhone?text=$encoded"
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback
    }
}

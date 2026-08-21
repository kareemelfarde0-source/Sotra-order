package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.ui.theme.*
import java.net.URLEncoder

@Composable
fun OrderCard(
    order: Order,
    onViewDetails: (Order) -> Unit,
    onAcknowledge: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isUnacknowledged = !order.isAcknowledged

    val borderColor = if (isUnacknowledged) SotraError else Color(0xFFE2E8F0)
    val cardBg = if (isUnacknowledged) Color(0xFFFEF2F2) else Color.White

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isUnacknowledged) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onViewDetails(order) },
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnacknowledged) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Order ID, Date, Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#${order.orderId}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = SotraPrimaryDark,
                            fontFamily = FontFamily.Monospace
                        )
                        if (isUnacknowledged) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SotraError)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "جديد!",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = formatArabicDate(order.createdAt),
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                StatusBadge(status = order.trackingStatus)
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = Color(0xFFF1F5F9)
            )

            // Customer Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SotraPrimaryLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = SotraPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.customer.fullName.ifBlank { "عميل غير محدد" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "${order.customer.governorateNameAr.ifBlank { "المحافظة" }} • ${order.customer.detailedAddress}",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (order.customer.phoneNumber.isNotBlank()) {
                        Text(
                            text = "📞 ${order.customer.phoneNumber}",
                            fontSize = 12.sp,
                            color = Color(0xFF475569),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Products list preview
            if (order.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(8.dp)
                ) {
                    order.items.take(2).forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "• ${item.titleAr} (${item.quantity}x)",
                                fontSize = 12.sp,
                                color = Color(0xFF334155),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(item.price * item.quantity).toInt()} ج.م",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                    if (order.items.size > 2) {
                        Text(
                            text = "+ ${order.items.size - 2} منتجات أخرى...",
                            fontSize = 11.sp,
                            color = SotraPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pricing summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "الإجمالي: ${order.total.toInt()} ج.م",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = SotraSecondary
                    )
                    Text(
                        text = "المتبقي للاستلام: ${order.remainingUponDelivery.toInt()} ج.م",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SotraSuccess
                    )
                }

                // Payment Method Tag
                PaymentMethodBadge(method = order.paymentMethod)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // If unacknowledged, show "تم استلام الطلب" button
                if (isUnacknowledged) {
                    Button(
                        onClick = { onAcknowledge(order.orderId) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SotraSuccess,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "تم الاستلام", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // WhatsApp Direct Chat Button
                OutlinedButton(
                    onClick = { launchWhatsApp(context, order) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF16A34A)),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF86EFAC))),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "واتساب",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF16A34A)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "واتساب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Details Button
                Button(
                    onClick = { onViewDetails(order) },
                    colors = ButtonDefaults.buttonColors(containerColor = SotraPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "التفاصيل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: OrderStatus, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(status.badgeBgColor))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status.labelAr,
            color = Color(status.badgeTextColor),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PaymentMethodBadge(method: String, modifier: Modifier = Modifier) {
    val (label, bg, fg, icon) = when (method.lowercase()) {
        "vodafone_cash" -> Quadruple("فودافون كاش", SotraErrorLight, SotraError, "VF")
        "instapay" -> Quadruple("إنستاباي", Color(0xFFEEF2FF), Color(0xFF4F46E5), "IP")
        else -> Quadruple("الدفع عند الاستلام", Color(0xFFF1F5F9), Color(0xFF475569), "COD")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "$icon • $label",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun launchWhatsApp(context: Context, order: Order) {
    val phone = order.customer.phoneNumber.replace("+", "").replace(" ", "").trim()
    val formattedPhone = if (phone.startsWith("0")) "20" + phone.substring(1) else if (phone.startsWith("20")) phone else "20$phone"
    val greeting = "مرحباً أستاذ/ة ${order.customer.fullName}، بخصوص طلبك رقم #${order.orderId} من سترة فاشون..."
    val encoded = URLEncoder.encode(greeting, "UTF-8")
    val url = "https://wa.me/$formattedPhone?text=$encoded"
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback to web browser
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(browserIntent)
    }
}

fun formatArabicDate(isoString: String): String {
    if (isoString.isBlank()) return "اليوم"
    return try {
        // Just return a friendly string if format matches
        if (isoString.contains("T")) {
            val datePart = isoString.substringBefore("T")
            val timePart = isoString.substringAfter("T").take(5)
            "$datePart • $timePart"
        } else {
            isoString
        }
    } catch (e: Exception) {
        isoString
    }
}

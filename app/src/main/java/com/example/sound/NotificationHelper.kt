package com.example.sound

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.model.Order

object NotificationHelper {
    private const val CHANNEL_ID = "sotra_orders_channel_v2"
    private const val CHANNEL_NAME = "تنبيهات طلبات سترة فاشون"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات وصول الطلبات الجديدة وتأكيد الدفع"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showNewOrderNotification(context: Context, order: Order) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("order_id", order.orderId)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                order.orderId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val totalStr = if (order.total > 0) "${order.total.toInt()} ج.م" else "قيد المراجعة"
            val customerName = order.customer.fullName.ifBlank { "عميل جديد" }
            val gov = order.customer.governorateNameAr.ifBlank { "" }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle("🛍️ طلب جديد: ${order.orderId}")
                .setContentText("العميل: $customerName - $totalStr ($gov)")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "طلب جديد #${order.orderId}\n" +
                                "العميل: $customerName (${order.customer.phoneNumber})\n" +
                                "المحافظة: $gov\n" +
                                "الإجمالي: $totalStr | طريقة الدفع: ${if (order.paymentMethod == "instapay") "إنستاباي" else "فودافون كاش"}"
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 400, 200, 400))

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(order.orderId.hashCode(), builder.build())
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Error posting notification", e)
        }
    }
}

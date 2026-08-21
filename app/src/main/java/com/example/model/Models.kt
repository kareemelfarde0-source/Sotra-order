package com.example.model

import androidx.compose.ui.graphics.Color

enum class OrderStatus(
    val code: String,
    val labelAr: String,
    val badgeBgColor: Long,
    val badgeTextColor: Long
) {
    PENDING_PAYMENT("pending_payment", "في انتظار الدفع", 0xFFFEF3C7, 0xFFB45309),
    PAYMENT_CONFIRMED("payment_confirmed", "تم تأكيد الدفع (جديد)", 0xFFDBEAFE, 0xFF1E40AF),
    PREPARING("preparing", "قيد التجهيز", 0xFFE0E7FF, 0xFF1E3A8A),
    SHIPPED("shipped", "تم الشحن", 0xFFF3E8FF, 0xFF3730A3),
    OUT_FOR_DELIVERY("out_for_delivery", "في الطريق للعميل", 0xFFFAE8FF, 0xFF6B21A8),
    DELIVERED("delivered", "تم التوصيل بنجاح", 0xFFD1FAE5, 0xFF065F46),
    CANCELLED("cancelled", "ملغي / مسترجع", 0xFFFEE2E2, 0xFF991B1B);

    companion object {
        fun fromCode(code: String): OrderStatus {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: PAYMENT_CONFIRMED
        }
    }
}

enum class RingtoneOption(
    val id: String,
    val titleAr: String,
    val descriptionAr: String,
    val iconName: String
) {
    CASH_REGISTER("cash_register", "رنين الكاشير والخزينة", "نغمة كاشير رقمية متتابعة مبهجة ومميزة للطلبات", "Cash"),
    URGENT_ALARM("urgent_alarm", "صفارة إنذار الطلبات العاجلة", "نغمة إنذار متكررة عالية ومستمرة حتى الانتباه", "Alert"),
    STORE_BELL("store_bell", "جرس المتجر الكلاسيكي", "دقات جرس المحل الكلاسيكية المزدوجة المتواصلة", "Bell"),
    DIGITAL_CHIME("digital_chime", "نغمة إلكترونية سريعة", "تنبيه إلكتروني حديث متكرر بلحن مميز", "Tune"),
    SYSTEM_DEFAULT("system_default", "نغمة تنبيه النظام", "استخدام النغمة الافتراضية لجهاز الأندرويد", "Phone");

    companion object {
        fun fromId(id: String): RingtoneOption {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: CASH_REGISTER
        }
    }
}

data class ItemColor(
    val nameAr: String = "",
    val nameEn: String = "",
    val hex: String = "#000000",
    val image: String = ""
)

data class OrderItem(
    val id: String = "",
    val titleAr: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val selectedColor: ItemColor = ItemColor(),
    val selectedSize: String = ""
)

data class CustomerInfo(
    val fullName: String = "",
    val phoneNumber: String = "",
    val secondaryPhone: String = "",
    val governorateNameAr: String = "",
    val detailedAddress: String = "",
    val notes: String = ""
)

data class Order(
    val orderId: String = "",
    val customer: CustomerInfo = CustomerInfo(),
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val shippingCost: Double = 0.0,
    val discount: Double = 0.0,
    val appliedCouponCode: String? = null,
    val total: Double = 0.0,
    val advanceShippingPaid: Double = 0.0,
    val remainingUponDelivery: Double = 0.0,
    val paymentMethod: String = "vodafone_cash", // vodafone_cash, instapay, cod
    val senderPhoneOrInstaPayId: String? = null,
    val transactionReference: String? = null,
    val vodafoneSenderPhone: String? = null,
    val shippingTransferNumber: String? = null,
    val trackingStatus: OrderStatus = OrderStatus.PAYMENT_CONFIRMED,
    val createdAt: String = "",
    val updatedAt: String = "",
    val isAcknowledged: Boolean = true // False when new unread order is sounding alarm
)

data class Customer(
    val id: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val secondaryPhone: String = "",
    val governorateNameAr: String = "",
    val detailedAddress: String = "",
    val totalOrdersCount: Int = 0,
    val totalSpent: Double = 0.0,
    val lastOrderDate: String = ""
)

data class StorePaymentConfig(
    val vodafoneCashEnabled: Boolean = true,
    val vodafoneCashNumber: String = "01098765432",
    val vodafoneCashAccountName: String = "سترة فاشون للملابس",
    val vodafoneCashInstructionsAr: String = "برجاء تحويل رسوم الشحن لتأكيد حجز الطلب على محفظة فودافون كاش.",
    val instaPayEnabled: Boolean = true,
    val instaPayId: String = "sotra.fashion@instapay",
    val instaPayAccountName: String = "سترة فاشون",
    val instaPayInstructionsAr: String = "برجاء إرسال مبلغ الشحن عبر تطبيق إنستاباي مع كتابة رقم الطلب.",
    val advanceShippingFeeOnly: Boolean = true
)

data class GovernorateShipping(
    val id: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val shippingCost: Int = 50,
    val deliveryDays: String = "2-3 أيام"
)

data class AppSoundSettings(
    val soundEnabled: Boolean = true,
    val selectedRingtone: RingtoneOption = RingtoneOption.CASH_REGISTER,
    val alarmVolume: Float = 0.9f,
    val vibrationEnabled: Boolean = true
)

package com.example.model

enum class OrderStatus(
    val code: String,
    val labelAr: String,
    val badgeBgColor: Long,
    val badgeTextColor: Long,
    val stageNumber: Int
) {
    ORDER_RECEIVED("order_received", "تم استلام الطلب", 0xFFFEF3C7, 0xFFB45309, 1),
    PAYMENT_CONFIRMED("payment_confirmed", "تأكيد الدفع والتجهيز", 0xFFDBEAFE, 0xFF1E40AF, 2),
    SHIPPED("shipped", "تم تسليمه للشحن", 0xFFF3E8FF, 0xFF3730A3, 3),
    DELIVERED("delivered", "تم التوصيل بنجاح", 0xFFD1FAE5, 0xFF065F46, 4),
    CANCELLED("cancelled", "تم إلغاء الطلب", 0xFFFEE2E2, 0xFF991B1B, 0);

    companion object {
        fun fromCode(code: String): OrderStatus {
            val normalized = code.trim().lowercase()
            return when {
                normalized.contains("received") || normalized.contains("استلام") || normalized == "1" || normalized.contains("pending") -> ORDER_RECEIVED
                normalized.contains("confirmed") || normalized.contains("preparing") || normalized.contains("تأكيد") || normalized.contains("تجهيز") || normalized == "2" || normalized.contains("processing") || normalized.contains("paid") -> PAYMENT_CONFIRMED
                normalized.contains("shipped") || normalized.contains("شحن") || normalized.contains("تسليم") || normalized.contains("delivery") || normalized == "3" || normalized.contains("transit") || normalized.contains("carrier") -> SHIPPED
                normalized.contains("delivered") || normalized.contains("توصيل") || normalized.contains("completed") || normalized.contains("done") || normalized == "4" || normalized.contains("نجاح") -> DELIVERED
                normalized.contains("cancel") || normalized.contains("إلغاء") || normalized.contains("الغاء") || normalized.contains("مسترجع") || normalized == "0" -> CANCELLED
                else -> entries.find { it.code.equals(normalized, ignoreCase = true) } ?: ORDER_RECEIVED
            }
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
    val docId: String = "",
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

enum class SalesTimeFilter(val id: String, val titleAr: String) {
    TODAY("today", "اليوم"),
    YESTERDAY("yesterday", "أمس"),
    LAST_7_DAYS("7_days", "آخر 7 أيام"),
    THIS_MONTH("this_month", "هذا الشهر (30 يوم)"),
    ALL_TIME("all", "جميع الأوقات")
}

data class ProductSalesSummary(
    val productKey: String,
    val titleAr: String,
    val color: String,
    val size: String,
    val categoryAr: String,
    val totalQuantitySold: Int,
    val unitSellingPrice: Double,
    val unitWholesalePrice: Double,
    val totalRevenue: Double = totalQuantitySold * unitSellingPrice,
    val totalCost: Double = totalQuantitySold * unitWholesalePrice,
    val netProfit: Double = totalRevenue - totalCost,
    val profitMarginPercent: Double = if (totalRevenue > 0) ((netProfit / totalRevenue) * 100.0) else 0.0,
    val ordersCount: Int = 1,
    val lastSoldDate: String = ""
)

object CategoryHelper {
    val ALL_CATEGORIES = listOf(
        "الكل",
        "فساتين ودريسات",
        "عبايات وكيمونو",
        "بلوزات وقمصان",
        "أطقم وسوتس",
        "بناطيل وجيب",
        "كارديجان وجواكت",
        "طرح وإكسسوارات",
        "أزياء متنوعة"
    )

    fun detectCategory(title: String): String {
        val t = title.lowercase()
        return when {
            t.contains("فستان") || t.contains("دريس") || t.contains("dress") -> "فساتين ودريسات"
            t.contains("عباية") || t.contains("عبايه") || t.contains("كيمونو") || t.contains("abaya") -> "عبايات وكيمونو"
            t.contains("بلوزة") || t.contains("بلوزه") || t.contains("توب") || t.contains("قميص") || t.contains("شميز") || t.contains("blouse") || t.contains("shirt") -> "بلوزات وقمصان"
            t.contains("طقم") || t.contains("سوت") || t.contains("بدلة") || t.contains("بدله") || t.contains("suit") || t.contains("set") -> "أطقم وسوتس"
            t.contains("بنطلون") || t.contains("جيب") || t.contains("تنورة") || t.contains("تنوره") || t.contains("pants") || t.contains("skirt") -> "بناطيل وجيب"
            t.contains("كارديجان") || t.contains("جاكيت") || t.contains("كاردي") || t.contains("بالطو") || t.contains("jacket") || t.contains("cardigan") -> "كارديجان وجواكت"
            t.contains("طرحة") || t.contains("طرحه") || t.contains("شال") || t.contains("سكارف") || t.contains("حجاب") || t.contains("إكسسوار") || t.contains("حزام") -> "طرح وإكسسوارات"
            else -> "أزياء متنوعة"
        }
    }
}


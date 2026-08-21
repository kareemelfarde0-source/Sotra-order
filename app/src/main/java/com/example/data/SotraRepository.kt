package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class SotraRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("sotra_prefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // Firebase Project Configuration
    companion object {
        const val FIREBASE_API_KEY = "AIzaSyBE6ZxUtxJB4XmO0jZTNmum1bwn8Gh0-AE"
        const val FIREBASE_PROJECT_ID = "sotra-45262"
        const val FIREBASE_RTDB_URL = "https://sotra-45262-default-rtdb.firebaseio.com"
        const val FIRESTORE_BASE_URL = "https://firestore.googleapis.com/v1/projects/sotra-45262/databases/(default)/documents"
    }

    // Observable States
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    val customers: StateFlow<List<Customer>> = _customers.asStateFlow()

    private val _paymentConfig = MutableStateFlow(StorePaymentConfig())
    val paymentConfig: StateFlow<StorePaymentConfig> = _paymentConfig.asStateFlow()

    private val _governorates = MutableStateFlow<List<GovernorateShipping>>(emptyList())
    val governorates: StateFlow<List<GovernorateShipping>> = _governorates.asStateFlow()

    private val _soundSettings = MutableStateFlow(AppSoundSettings())
    val soundSettings: StateFlow<AppSoundSettings> = _soundSettings.asStateFlow()

    private val _wholesalePrices = MutableStateFlow<Map<String, Double>>(emptyMap())
    val wholesalePrices: StateFlow<Map<String, Double>> = _wholesalePrices.asStateFlow()

    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Listener for new order alert trigger callback
    var onNewOrderReceived: ((Order) -> Unit)? = null

    private val knownOrderIds = mutableSetOf<String>()
    private var isInitialLoadComplete = false

    init {
        loadSettingsFromPrefs()
        initializeDefaultData()
        startPeriodicSync()
    }

    private fun loadSettingsFromPrefs() {
        val soundOn = prefs.getBoolean("sound_enabled", true)
        val ringtoneId = prefs.getString("ringtone_id", RingtoneOption.CASH_REGISTER.id) ?: RingtoneOption.CASH_REGISTER.id
        val volume = prefs.getFloat("alarm_volume", 0.95f)
        val vibration = prefs.getBoolean("vibration_enabled", true)

        _soundSettings.value = AppSoundSettings(
            soundEnabled = soundOn,
            selectedRingtone = RingtoneOption.fromId(ringtoneId),
            alarmVolume = volume,
            vibrationEnabled = vibration
        )

        // Load cached payment config
        _paymentConfig.value = StorePaymentConfig(
            vodafoneCashEnabled = prefs.getBoolean("vc_enabled", true),
            vodafoneCashNumber = prefs.getString("vc_number", "01098765432") ?: "01098765432",
            vodafoneCashAccountName = prefs.getString("vc_name", "سترة فاشون للملابس") ?: "سترة فاشون للملابس",
            vodafoneCashInstructionsAr = prefs.getString("vc_inst", "برجاء تحويل رسوم الشحن لتأكيد حجز الطلب على محفظة فودافون كاش.") ?: "",
            instaPayEnabled = prefs.getBoolean("ip_enabled", true),
            instaPayId = prefs.getString("ip_id", "sotra.fashion@instapay") ?: "sotra.fashion@instapay",
            instaPayAccountName = prefs.getString("ip_name", "سترة فاشون") ?: "سترة فاشون",
            instaPayInstructionsAr = prefs.getString("ip_inst", "برجاء إرسال مبلغ الشحن عبر تطبيق إنستاباي مع كتابة رقم الطلب.") ?: ""
        )

        // Load cached wholesale prices
        try {
            val wholesaleJson = prefs.getString("wholesale_prices_map", "{}") ?: "{}"
            val jsonObj = JSONObject(wholesaleJson)
            val map = mutableMapOf<String, Double>()
            val keys = jsonObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = jsonObj.optDouble(k, 0.0)
            }
            _wholesalePrices.value = map
        } catch (e: Exception) {
            Log.w("SotraRepository", "Wholesale prices load note: ${e.message}")
        }
    }

    fun updateSoundSettings(newSettings: AppSoundSettings) {
        _soundSettings.value = newSettings
        prefs.edit()
            .putBoolean("sound_enabled", newSettings.soundEnabled)
            .putString("ringtone_id", newSettings.selectedRingtone.id)
            .putFloat("alarm_volume", newSettings.alarmVolume)
            .putBoolean("vibration_enabled", newSettings.vibrationEnabled)
            .apply()
    }

    private fun initializeDefaultData() {
        val initialGovs = listOf(
            GovernorateShipping("cairo", "القاهرة", "Cairo", 45, "1-2 يوم"),
            GovernorateShipping("giza", "الجيزة", "Giza", 45, "1-2 يوم"),
            GovernorateShipping("alex", "الإسكندرية", "Alexandria", 55, "2-3 أيام"),
            GovernorateShipping("dakahlia", "الدقهلية (المنصورة)", "Dakahlia", 60, "2-3 أيام"),
            GovernorateShipping("sharqia", "الشرقية (الزقازيق)", "Sharqia", 60, "2-3 أيام"),
            GovernorateShipping("gharbia", "الغربية (طنطا)", "Gharbia", 60, "2-3 أيام"),
            GovernorateShipping("monufia", "المنوفية", "Monufia", 60, "2-3 أيام"),
            GovernorateShipping("qalyubia", "القليوبية", "Qalyubia", 50, "1-2 يوم"),
            GovernorateShipping("beheira", "البحيرة", "Beheira", 65, "3-4 أيام"),
            GovernorateShipping("ismailia", "الإسماعيلية", "Ismailia", 65, "2-3 أيام"),
            GovernorateShipping("suez", "السويس", "Suez", 65, "2-3 أيام"),
            GovernorateShipping("port_said", "بورسعيد", "Port Said", 65, "2-3 أيام"),
            GovernorateShipping("asyut", "أسيوط", "Asyut", 75, "3-4 أيام"),
            GovernorateShipping("sohag", "سوهاج", "Sohag", 80, "3-5 أيام"),
            GovernorateShipping("qena", "قنا", "Qena", 85, "3-5 أيام"),
            GovernorateShipping("luxor", "الأقصر", "Luxor", 90, "4-5 أيام"),
            GovernorateShipping("aswan", "أسوان", "Aswan", 95, "4-5 أيام")
        )
        _governorates.value = initialGovs
        _orders.value = emptyList()
        _customers.value = emptyList()

        // Immediate initial sync with Firebase
        coroutineScope.launch {
            syncWithFirebase()
        }
    }

    private fun recalculateCustomers(orderList: List<Order>) {
        val custMap = mutableMapOf<String, Customer>()
        orderList.forEach { o ->
            val phone = o.customer.phoneNumber.trim()
            if (phone.isNotBlank()) {
                val existing = custMap[phone]
                if (existing == null) {
                    custMap[phone] = Customer(
                        id = phone,
                        fullName = o.customer.fullName,
                        phoneNumber = phone,
                        secondaryPhone = o.customer.secondaryPhone,
                        governorateNameAr = o.customer.governorateNameAr,
                        detailedAddress = o.customer.detailedAddress,
                        totalOrdersCount = 1,
                        totalSpent = if (o.trackingStatus != OrderStatus.CANCELLED) o.total else 0.0,
                        lastOrderDate = o.createdAt
                    )
                } else {
                    custMap[phone] = existing.copy(
                        totalOrdersCount = existing.totalOrdersCount + 1,
                        totalSpent = existing.totalSpent + if (o.trackingStatus != OrderStatus.CANCELLED) o.total else 0.0,
                        lastOrderDate = if (o.createdAt > existing.lastOrderDate) o.createdAt else existing.lastOrderDate
                    )
                }
            }
        }
        _customers.value = custMap.values.sortedByDescending { it.lastOrderDate }
    }

    private fun startPeriodicSync() {
        coroutineScope.launch {
            while (true) {
                syncWithFirebase()
                delay(10_000) // Poll every 10 seconds for real-time order tracking
            }
        }
    }

    suspend fun syncWithFirebase() = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        var syncSuccessful = false

        // 1. Fetch Orders from Firebase Realtime Database
        try {
            val rtdbUrl = "$FIREBASE_RTDB_URL/orders.json?key=$FIREBASE_API_KEY"
            val rtdbRequest = Request.Builder().url(rtdbUrl).get().build()
            client.newCall(rtdbRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank() && bodyString != "null") {
                        parseRealtimeDbOrders(bodyString)
                        syncSuccessful = true
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("SotraRepository", "RTDB sync check: ${e.message}")
        }

        // 2. Fetch Orders from Firestore Documents
        try {
            val firestoreUrl = "$FIRESTORE_BASE_URL/orders?key=$FIREBASE_API_KEY"
            val firestoreRequest = Request.Builder().url(firestoreUrl).get().build()
            client.newCall(firestoreRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank()) {
                        parseFirestoreOrders(bodyString)
                        syncSuccessful = true
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("SotraRepository", "Firestore sync check: ${e.message}")
        }

        // 3. Sync Settings & Wholesale prices from Firebase Realtime Database
        try {
            val paymentUrl = "$FIREBASE_RTDB_URL/settings/payment.json?key=$FIREBASE_API_KEY"
            val req = Request.Builder().url(paymentUrl).get().build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank() && body != "null") {
                        val pObj = JSONObject(body)
                        val rVcNumber = getStringFromObject(pObj, "vodafoneCashNumber", "vodafone_cash_number", "vodafoneNumber", "vodafonePhone", "number", "walletNumber")
                        val rVcName = getStringFromObject(pObj, "vodafoneCashAccountName", "vodafone_cash_account_name", "vodafoneName", "accountName")
                        val rVcInst = getStringFromObject(pObj, "vodafoneCashInstructionsAr", "vodafone_cash_instructions_ar", "instructions", "instructionsAr")
                        val rIpId = getStringFromObject(pObj, "instaPayId", "instapay_id", "instapayId", "ipa", "address", "id")
                        val rIpName = getStringFromObject(pObj, "instaPayAccountName", "instapay_account_name", "instaPayName")
                        val rIpInst = getStringFromObject(pObj, "instaPayInstructionsAr", "instapay_instructions_ar")

                        val current = _paymentConfig.value
                        val updated = current.copy(
                            vodafoneCashEnabled = if (pObj.has("vodafoneCashEnabled")) pObj.optBoolean("vodafoneCashEnabled") else (pObj.optBoolean("vodafone_cash_enabled", current.vodafoneCashEnabled)),
                            vodafoneCashNumber = if (rVcNumber.isNotBlank()) rVcNumber else current.vodafoneCashNumber,
                            vodafoneCashAccountName = if (rVcName.isNotBlank()) rVcName else current.vodafoneCashAccountName,
                            vodafoneCashInstructionsAr = if (rVcInst.isNotBlank()) rVcInst else current.vodafoneCashInstructionsAr,
                            instaPayEnabled = if (pObj.has("instaPayEnabled")) pObj.optBoolean("instaPayEnabled") else (pObj.optBoolean("instapay_enabled", current.instaPayEnabled)),
                            instaPayId = if (rIpId.isNotBlank()) rIpId else current.instaPayId,
                            instaPayAccountName = if (rIpName.isNotBlank()) rIpName else current.instaPayAccountName,
                            instaPayInstructionsAr = if (rIpInst.isNotBlank()) rIpInst else current.instaPayInstructionsAr,
                            advanceShippingFeeOnly = pObj.optBoolean("advanceShippingFeeOnly", true)
                        )
                        _paymentConfig.value = updated
                        // Persist to local prefs
                        prefs.edit()
                            .putBoolean("vc_enabled", updated.vodafoneCashEnabled)
                            .putString("vc_number", updated.vodafoneCashNumber)
                            .putString("vc_name", updated.vodafoneCashAccountName)
                            .putString("vc_inst", updated.vodafoneCashInstructionsAr)
                            .putBoolean("ip_enabled", updated.instaPayEnabled)
                            .putString("ip_id", updated.instaPayId)
                            .putString("ip_name", updated.instaPayAccountName)
                            .putString("ip_inst", updated.instaPayInstructionsAr)
                            .apply()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("SotraRepository", "Payment settings sync note: ${e.message}")
        }

        // 4. Sync Wholesale prices from Firebase if available
        try {
            val wholesaleUrl = "$FIREBASE_RTDB_URL/settings/wholesale_prices.json?key=$FIREBASE_API_KEY"
            val reqW = Request.Builder().url(wholesaleUrl).get().build()
            client.newCall(reqW).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank() && body != "null") {
                        val wObj = JSONObject(body)
                        val map = _wholesalePrices.value.toMutableMap()
                        val keys = wObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            map[k] = wObj.optDouble(k, map[k] ?: 0.0)
                        }
                        _wholesalePrices.value = map
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("SotraRepository", "Wholesale prices sync note: ${e.message}")
        }

        _isConnected.value = syncSuccessful || _orders.value.isNotEmpty()
        _isSyncing.value = false
        isInitialLoadComplete = true
    }

    // ==========================================
    // Robust Helper Methods for Pricing Parsing
    // ==========================================

    private fun parseNumberSafely(value: Any?): Double {
        if (value == null) return 0.0
        return when (value) {
            is Number -> value.toDouble()
            is String -> {
                val clean = value.trim()
                    .replace("EGP", "", ignoreCase = true)
                    .replace("ج.م", "")
                    .replace("جم", "")
                    .replace("LE", "", ignoreCase = true)
                    .replace("L.E", "", ignoreCase = true)
                    .replace("جنيه", "")
                    .replace(",", "")
                    .replace(" ", "")
                    .trim()
                clean.toDoubleOrNull() ?: 0.0
            }
            else -> 0.0
        }
    }

    private fun getDoubleFromObject(obj: JSONObject?, vararg keys: String): Double {
        if (obj == null) return 0.0
        for (k in keys) {
            if (obj.has(k)) {
                val raw = obj.opt(k)
                val parsed = parseNumberSafely(raw)
                if (parsed > 0.0) return parsed
            }
        }
        // Also check nested containers (e.g. pricing, totals, summary, payment, financials)
        val nestedContainers = listOf("pricing", "totals", "summary", "payment", "financials", "billing", "orderSummary")
        for (container in nestedContainers) {
            val nestedObj = obj.optJSONObject(container)
            if (nestedObj != null) {
                for (k in keys) {
                    if (nestedObj.has(k)) {
                        val raw = nestedObj.opt(k)
                        val parsed = parseNumberSafely(raw)
                        if (parsed > 0.0) return parsed
                    }
                }
            }
        }
        return 0.0
    }

    private fun getStringFromObject(obj: JSONObject?, vararg keys: String): String {
        if (obj == null) return ""
        for (k in keys) {
            if (obj.has(k)) {
                val str = obj.optString(k, "").trim()
                if (str.isNotBlank() && str != "null") return str
            }
        }
        val nestedContainers = listOf("customer", "customerInfo", "shippingAddress", "address", "payment", "paymentDetails")
        for (container in nestedContainers) {
            val nestedObj = obj.optJSONObject(container)
            if (nestedObj != null) {
                for (k in keys) {
                    if (nestedObj.has(k)) {
                        val str = nestedObj.optString(k, "").trim()
                        if (str.isNotBlank() && str != "null") return str
                    }
                }
            }
        }
        return ""
    }

    private fun getFirestoreDouble(fields: JSONObject?, vararg keys: String): Double {
        if (fields == null) return 0.0
        for (k in keys) {
            val fieldObj = fields.optJSONObject(k) ?: continue
            if (fieldObj.has("doubleValue")) {
                val d = fieldObj.optDouble("doubleValue", 0.0)
                if (d > 0.0) return d
            }
            if (fieldObj.has("integerValue")) {
                val raw = fieldObj.opt("integerValue")
                val d = parseNumberSafely(raw)
                if (d > 0.0) return d
            }
            if (fieldObj.has("stringValue")) {
                val raw = fieldObj.optString("stringValue")
                val d = parseNumberSafely(raw)
                if (d > 0.0) return d
            }
        }
        // Check nested maps in Firestore: pricing, summary, totals, payment, financials
        val nestedContainers = listOf("pricing", "summary", "totals", "payment", "financials", "billing", "orderSummary")
        for (container in nestedContainers) {
            val nestedMap = fields.optJSONObject(container)?.optJSONObject("mapValue")?.optJSONObject("fields")
            if (nestedMap != null) {
                for (k in keys) {
                    val fieldObj = nestedMap.optJSONObject(k) ?: continue
                    if (fieldObj.has("doubleValue")) {
                        val d = fieldObj.optDouble("doubleValue", 0.0)
                        if (d > 0.0) return d
                    }
                    if (fieldObj.has("integerValue")) {
                        val raw = fieldObj.opt("integerValue")
                        val d = parseNumberSafely(raw)
                        if (d > 0.0) return d
                    }
                    if (fieldObj.has("stringValue")) {
                        val raw = fieldObj.optString("stringValue")
                        val d = parseNumberSafely(raw)
                        if (d > 0.0) return d
                    }
                }
            }
        }
        return 0.0
    }

    private fun getFirestoreString(fields: JSONObject?, vararg keys: String): String {
        if (fields == null) return ""
        for (k in keys) {
            val fieldObj = fields.optJSONObject(k) ?: continue
            if (fieldObj.has("stringValue")) {
                val str = fieldObj.optString("stringValue", "").trim()
                if (str.isNotBlank() && str != "null") return str
            }
            if (fieldObj.has("integerValue")) {
                val raw = fieldObj.optString("integerValue", "").trim()
                if (raw.isNotBlank() && raw != "null") return raw
            }
        }
        // Check nested maps in Firestore
        val nestedContainers = listOf("customer", "customerInfo", "shippingAddress", "payment", "paymentDetails", "address")
        for (container in nestedContainers) {
            val nestedMap = fields.optJSONObject(container)?.optJSONObject("mapValue")?.optJSONObject("fields")
            if (nestedMap != null) {
                for (k in keys) {
                    val fieldObj = nestedMap.optJSONObject(k) ?: continue
                    if (fieldObj.has("stringValue")) {
                        val str = fieldObj.optString("stringValue", "").trim()
                        if (str.isNotBlank() && str != "null") return str
                    }
                    if (fieldObj.has("integerValue")) {
                        val raw = fieldObj.optString("integerValue", "").trim()
                        if (raw.isNotBlank() && raw != "null") return raw
                    }
                }
            }
        }
        return ""
    }

    // ==========================================
    // Realtime Database Parser
    // ==========================================

    private fun parseRealtimeDbOrders(jsonStr: String) {
        try {
            val root = JSONObject(jsonStr)
            val fetchedOrders = mutableListOf<Order>()
            var newlyArrivedOrder: Order? = null

            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val itemObj = root.optJSONObject(key) ?: continue

                val orderId = getStringFromObject(itemObj, "orderId", "id", "orderNumber", "order_id", "order_number").ifBlank { key }
                val statusStr = getStringFromObject(itemObj, "trackingStatus", "status", "orderStatus", "state").ifBlank { "payment_confirmed" }
                val status = OrderStatus.fromCode(statusStr)

                // Financial values extraction with all potential aliases
                val rawTotal = getDoubleFromObject(itemObj, "total", "totalPrice", "orderTotal", "totalAmount", "finalTotal", "grandTotal", "amount", "price", "cartTotal", "cost", "order_total", "total_price")
                val rawSubtotal = getDoubleFromObject(itemObj, "subtotal", "subTotal", "sub_total", "itemsTotal", "productsTotal", "cartSubtotal", "itemsPrice")
                val rawShipping = getDoubleFromObject(itemObj, "shippingCost", "shipping", "shipping_cost", "shippingFee", "shipping_fee", "deliveryFee", "deliveryCost", "deliveryPrice", "delivery_fee")
                val rawDiscount = getDoubleFromObject(itemObj, "discount", "discountAmount", "discount_amount", "discountValue", "couponDiscount", "coupon_discount")
                val rawAdvance = getDoubleFromObject(itemObj, "advanceShippingPaid", "shippingPaid", "depositPaid", "advancePayment", "advancePaid", "deposit", "downPayment", "paidAmount")
                val rawRemaining = getDoubleFromObject(itemObj, "remainingUponDelivery", "remainingAmount", "codAmount", "remainingBalance", "remaining", "dueAmount", "cashOnDelivery")

                val appliedCoupon = getStringFromObject(itemObj, "appliedCouponCode", "couponCode", "coupon", "coupon_code").ifBlank { null }
                val paymentMethod = getStringFromObject(itemObj, "paymentMethod", "payment_method", "paymentType", "method").ifBlank { "vodafone_cash" }
                val createdAt = getStringFromObject(itemObj, "createdAt", "date", "created_at", "orderDate")
                val updatedAt = getStringFromObject(itemObj, "updatedAt", "updated_at").ifBlank { createdAt }
                val senderPhone = getStringFromObject(itemObj, "senderPhoneOrInstaPayId", "vodafoneSenderPhone", "senderPhone", "sender_phone", "instaPayId", "senderNumber")
                val transRef = getStringFromObject(itemObj, "transactionReference", "shippingTransferNumber", "referenceNumber", "transaction_ref", "transRef", "refNumber", "reference")

                // Customer info
                val custObj = itemObj.optJSONObject("customer") ?: itemObj.optJSONObject("customerInfo") ?: itemObj.optJSONObject("shippingAddress")
                val customerInfo = CustomerInfo(
                    fullName = getStringFromObject(custObj ?: itemObj, "fullName", "name", "customerName", "clientName", "user_name").ifBlank { "عميل" },
                    phoneNumber = getStringFromObject(custObj ?: itemObj, "phoneNumber", "phone", "customerPhone", "mobile", "tel", "phone_number"),
                    secondaryPhone = getStringFromObject(custObj ?: itemObj, "secondaryPhone", "altPhone", "secondary_phone", "backupPhone"),
                    governorateNameAr = getStringFromObject(custObj ?: itemObj, "governorateNameAr", "governorate", "gov", "city", "province"),
                    detailedAddress = getStringFromObject(custObj ?: itemObj, "detailedAddress", "address", "fullAddress", "streetAddress", "street", "details"),
                    notes = getStringFromObject(custObj ?: itemObj, "notes", "customerNotes", "orderNotes", "comment")
                )

                // Items list extraction (support both JSONArray and JSONObject dictionary)
                val itemsList = mutableListOf<OrderItem>()
                val itemsArr = itemObj.optJSONArray("items") ?: itemObj.optJSONArray("products") ?: itemObj.optJSONArray("cart") ?: itemObj.optJSONArray("cartItems") ?: itemObj.optJSONArray("orderItems")
                if (itemsArr != null) {
                    for (idx in 0 until itemsArr.length()) {
                        val itmObj = itemsArr.optJSONObject(idx) ?: continue
                        val itemColorObj = itmObj.optJSONObject("selectedColor") ?: itmObj.optJSONObject("color")
                        val itemColor = if (itemColorObj != null) {
                            ItemColor(
                                nameAr = getStringFromObject(itemColorObj, "nameAr", "name", "title", "color").ifBlank { "افتراضي" },
                                nameEn = getStringFromObject(itemColorObj, "nameEn", "en"),
                                hex = getStringFromObject(itemColorObj, "hex", "colorHex").ifBlank { "#000000" },
                                image = getStringFromObject(itemColorObj, "image", "img", "imageUrl")
                            )
                        } else {
                            val colorStr = getStringFromObject(itmObj, "color", "selectedColor", "colour")
                            ItemColor(nameAr = colorStr.ifBlank { "افتراضي" })
                        }

                        val itemPrice = getDoubleFromObject(itmObj, "price", "unitPrice", "itemPrice", "cost", "productPrice", "sellingPrice", "regularPrice", "total")
                        val itemQty = (getDoubleFromObject(itmObj, "quantity", "qty", "count", "amount", "itemQuantity")).toInt().coerceAtLeast(1)

                        itemsList.add(
                            OrderItem(
                                id = getStringFromObject(itmObj, "id", "itemId", "productId", "sku").ifBlank { "item-$idx" },
                                titleAr = getStringFromObject(itmObj, "titleAr", "title", "name", "productName", "itemTitle", "title_ar", "arabicName").ifBlank { "منتج من سترة فاشون" },
                                price = itemPrice,
                                quantity = itemQty,
                                selectedColor = itemColor,
                                selectedSize = getStringFromObject(itmObj, "selectedSize", "size", "sizeName", "selected_size").ifBlank { "ستاندرد" }
                            )
                        )
                    }
                } else {
                    // Try parsing items from JSONObject map
                    val itemsObj = itemObj.optJSONObject("items") ?: itemObj.optJSONObject("products") ?: itemObj.optJSONObject("cart")
                    if (itemsObj != null) {
                        val iKeys = itemsObj.keys()
                        var idx = 0
                        while (iKeys.hasNext()) {
                            val iKey = iKeys.next()
                            val itmObj = itemsObj.optJSONObject(iKey) ?: continue
                            val itemPrice = getDoubleFromObject(itmObj, "price", "unitPrice", "itemPrice", "cost", "productPrice", "sellingPrice", "total")
                            val itemQty = (getDoubleFromObject(itmObj, "quantity", "qty", "count", "amount")).toInt().coerceAtLeast(1)
                            itemsList.add(
                                OrderItem(
                                    id = iKey,
                                    titleAr = getStringFromObject(itmObj, "titleAr", "title", "name", "productName", "itemTitle").ifBlank { "منتج" },
                                    price = itemPrice,
                                    quantity = itemQty,
                                    selectedColor = ItemColor(nameAr = getStringFromObject(itmObj, "color", "selectedColor")),
                                    selectedSize = getStringFromObject(itmObj, "selectedSize", "size")
                                )
                            )
                            idx++
                        }
                    }
                }

                // Automatic Pricing Resolution & Fallbacks (Ensuring no zero prices if items or shipping exist)
                val calculatedItemsSum = itemsList.sumOf { it.price * it.quantity }
                val resolvedSubtotal = if (rawSubtotal > 0.0) rawSubtotal else if (calculatedItemsSum > 0.0) calculatedItemsSum else rawTotal
                val resolvedShipping = if (rawShipping > 0.0) rawShipping else {
                    val foundGov = _governorates.value.find {
                        it.nameAr.contains(customerInfo.governorateNameAr.trim(), ignoreCase = true) ||
                                (customerInfo.governorateNameAr.isNotBlank() && customerInfo.governorateNameAr.contains(it.nameAr.trim(), ignoreCase = true))
                    }
                    foundGov?.shippingCost?.toDouble() ?: 0.0
                }
                val resolvedTotal = if (rawTotal > 0.0) rawTotal else (resolvedSubtotal + resolvedShipping - rawDiscount).coerceAtLeast(0.0)
                val resolvedAdvance = if (rawAdvance > 0.0) rawAdvance else resolvedShipping
                val resolvedRemaining = if (rawRemaining > 0.0) rawRemaining else (resolvedTotal - resolvedAdvance).coerceAtLeast(0.0)

                val isNew = isInitialLoadComplete && !knownOrderIds.contains(orderId) &&
                        (status == OrderStatus.ORDER_RECEIVED || status == OrderStatus.PAYMENT_CONFIRMED)

                val parsedOrder = Order(
                    orderId = orderId,
                    docId = key,
                    customer = customerInfo,
                    items = itemsList,
                    subtotal = resolvedSubtotal,
                    shippingCost = resolvedShipping,
                    discount = rawDiscount,
                    appliedCouponCode = appliedCoupon,
                    total = resolvedTotal,
                    advanceShippingPaid = resolvedAdvance,
                    remainingUponDelivery = resolvedRemaining,
                    paymentMethod = paymentMethod,
                    senderPhoneOrInstaPayId = senderPhone.ifBlank { null },
                    transactionReference = transRef.ifBlank { null },
                    trackingStatus = status,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    isAcknowledged = !isNew
                )

                fetchedOrders.add(parsedOrder)
                knownOrderIds.add(orderId)

                if (isNew && newlyArrivedOrder == null) {
                    newlyArrivedOrder = parsedOrder
                }
            }

            if (fetchedOrders.isNotEmpty()) {
                val merged = (_orders.value.filter { local -> fetchedOrders.none { it.orderId == local.orderId } } + fetchedOrders)
                    .sortedByDescending { it.createdAt }
                _orders.value = merged
                recalculateCustomers(merged)

                newlyArrivedOrder?.let {
                    onNewOrderReceived?.invoke(it)
                }
            }
        } catch (e: Exception) {
            Log.e("SotraRepository", "RTDB Parse error", e)
        }
    }

    // ==========================================
    // Cloud Firestore Parser
    // ==========================================

    private fun parseFirestoreOrders(jsonStr: String) {
        try {
            val root = JSONObject(jsonStr)
            val documents = root.optJSONArray("documents") ?: return
            val fetchedOrders = mutableListOf<Order>()
            var newlyArrivedOrder: Order? = null

            for (i in 0 until documents.length()) {
                val doc = documents.getJSONObject(i)
                val fields = doc.optJSONObject("fields") ?: continue
                val name = doc.optString("name")
                val docId = name.substringAfterLast("/")

                val orderId = getFirestoreString(fields, "orderId", "id", "orderNumber", "order_id").ifBlank { docId }
                val statusStr = getFirestoreString(fields, "trackingStatus", "status", "orderStatus", "state").ifBlank { "order_received" }
                val status = OrderStatus.fromCode(statusStr)

                // Financial values
                val rawTotal = getFirestoreDouble(fields, "total", "totalPrice", "orderTotal", "totalAmount", "finalTotal", "grandTotal", "amount", "price", "cartTotal", "cost")
                val rawSubtotal = getFirestoreDouble(fields, "subtotal", "subTotal", "sub_total", "itemsTotal", "productsTotal", "cartSubtotal", "itemsPrice")
                val rawShipping = getFirestoreDouble(fields, "shippingCost", "shipping", "shipping_cost", "shippingFee", "shipping_fee", "deliveryFee", "deliveryCost", "deliveryPrice")
                val rawDiscount = getFirestoreDouble(fields, "discount", "discountAmount", "discount_amount", "discountValue", "couponDiscount")
                val rawAdvance = getFirestoreDouble(fields, "advanceShippingPaid", "shippingPaid", "depositPaid", "advancePayment", "advancePaid", "deposit", "downPayment")
                val rawRemaining = getFirestoreDouble(fields, "remainingUponDelivery", "remainingAmount", "codAmount", "remainingBalance", "remaining", "dueAmount")

                val appliedCoupon = getFirestoreString(fields, "appliedCouponCode", "couponCode", "coupon").ifBlank { null }
                val paymentMethod = getFirestoreString(fields, "paymentMethod", "payment_method", "paymentType").ifBlank { "vodafone_cash" }
                val createdAt = getFirestoreString(fields, "createdAt", "date", "created_at", "orderDate")
                val updatedAt = getFirestoreString(fields, "updatedAt", "updated_at").ifBlank { createdAt }
                val senderPhone = getFirestoreString(fields, "senderPhoneOrInstaPayId", "vodafoneSenderPhone", "senderPhone", "instaPayId", "senderNumber")
                val transRef = getFirestoreString(fields, "transactionReference", "shippingTransferNumber", "referenceNumber", "transaction_ref", "refNumber")

                // Customer Object
                val custMap = fields.optJSONObject("customer")?.optJSONObject("mapValue")?.optJSONObject("fields")
                    ?: fields.optJSONObject("customerInfo")?.optJSONObject("mapValue")?.optJSONObject("fields")
                    ?: fields.optJSONObject("shippingAddress")?.optJSONObject("mapValue")?.optJSONObject("fields")

                val customerInfo = CustomerInfo(
                    fullName = getFirestoreString(custMap ?: fields, "fullName", "name", "customerName", "clientName").ifBlank { "عميل" },
                    phoneNumber = getFirestoreString(custMap ?: fields, "phoneNumber", "phone", "customerPhone", "mobile"),
                    secondaryPhone = getFirestoreString(custMap ?: fields, "secondaryPhone", "altPhone", "secondary_phone"),
                    governorateNameAr = getFirestoreString(custMap ?: fields, "governorateNameAr", "governorate", "gov", "city"),
                    detailedAddress = getFirestoreString(custMap ?: fields, "detailedAddress", "address", "fullAddress", "streetAddress", "street"),
                    notes = getFirestoreString(custMap ?: fields, "notes", "customerNotes", "orderNotes")
                )

                // Items Array from Firestore
                val itemsList = mutableListOf<OrderItem>()
                val itemsArrayValues = fields.optJSONObject("items")?.optJSONObject("arrayValue")?.optJSONArray("values")
                    ?: fields.optJSONObject("products")?.optJSONObject("arrayValue")?.optJSONArray("values")
                    ?: fields.optJSONObject("cart")?.optJSONObject("arrayValue")?.optJSONArray("values")

                if (itemsArrayValues != null) {
                    for (idx in 0 until itemsArrayValues.length()) {
                        val itmFields = itemsArrayValues.optJSONObject(idx)?.optJSONObject("mapValue")?.optJSONObject("fields") ?: continue
                        val colorMap = itmFields.optJSONObject("selectedColor")?.optJSONObject("mapValue")?.optJSONObject("fields")
                            ?: itmFields.optJSONObject("color")?.optJSONObject("mapValue")?.optJSONObject("fields")

                        val color = if (colorMap != null) {
                            ItemColor(
                                nameAr = getFirestoreString(colorMap, "nameAr", "name", "title").ifBlank { "افتراضي" },
                                nameEn = getFirestoreString(colorMap, "nameEn", "en"),
                                hex = getFirestoreString(colorMap, "hex", "colorHex").ifBlank { "#000000" },
                                image = getFirestoreString(colorMap, "image", "img", "imageUrl")
                            )
                        } else {
                            ItemColor(nameAr = getFirestoreString(itmFields, "color", "selectedColor", "colour").ifBlank { "افتراضي" })
                        }

                        val itemPrice = getFirestoreDouble(itmFields, "price", "unitPrice", "itemPrice", "cost", "productPrice", "sellingPrice", "total")
                        val itemQty = (getFirestoreDouble(itmFields, "quantity", "qty", "count", "amount")).toInt().coerceAtLeast(1)

                        itemsList.add(
                            OrderItem(
                                id = getFirestoreString(itmFields, "id", "itemId", "productId", "sku").ifBlank { "item-$idx" },
                                titleAr = getFirestoreString(itmFields, "titleAr", "title", "name", "productName", "itemTitle").ifBlank { "منتج من سترة فاشون" },
                                price = itemPrice,
                                quantity = itemQty,
                                selectedColor = color,
                                selectedSize = getFirestoreString(itmFields, "selectedSize", "size", "sizeName").ifBlank { "ستاندرد" }
                            )
                        )
                    }
                }

                // Automatic Pricing Resolution & Fallbacks
                val calculatedItemsSum = itemsList.sumOf { it.price * it.quantity }
                val resolvedSubtotal = if (rawSubtotal > 0.0) rawSubtotal else if (calculatedItemsSum > 0.0) calculatedItemsSum else rawTotal
                val resolvedShipping = if (rawShipping > 0.0) rawShipping else {
                    val foundGov = _governorates.value.find {
                        it.nameAr.contains(customerInfo.governorateNameAr.trim(), ignoreCase = true) ||
                                (customerInfo.governorateNameAr.isNotBlank() && customerInfo.governorateNameAr.contains(it.nameAr.trim(), ignoreCase = true))
                    }
                    foundGov?.shippingCost?.toDouble() ?: 0.0
                }
                val resolvedTotal = if (rawTotal > 0.0) rawTotal else (resolvedSubtotal + resolvedShipping - rawDiscount).coerceAtLeast(0.0)
                val resolvedAdvance = if (rawAdvance > 0.0) rawAdvance else resolvedShipping
                val resolvedRemaining = if (rawRemaining > 0.0) rawRemaining else (resolvedTotal - resolvedAdvance).coerceAtLeast(0.0)

                val isNew = isInitialLoadComplete && !knownOrderIds.contains(orderId) &&
                        (status == OrderStatus.ORDER_RECEIVED || status == OrderStatus.PAYMENT_CONFIRMED)

                val parsedOrder = Order(
                    orderId = orderId,
                    docId = docId,
                    customer = customerInfo,
                    items = itemsList,
                    subtotal = resolvedSubtotal,
                    shippingCost = resolvedShipping,
                    discount = rawDiscount,
                    appliedCouponCode = appliedCoupon,
                    total = resolvedTotal,
                    advanceShippingPaid = resolvedAdvance,
                    remainingUponDelivery = resolvedRemaining,
                    paymentMethod = paymentMethod,
                    senderPhoneOrInstaPayId = senderPhone.ifBlank { null },
                    transactionReference = transRef.ifBlank { null },
                    trackingStatus = status,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    isAcknowledged = !isNew
                )

                fetchedOrders.add(parsedOrder)
                knownOrderIds.add(orderId)

                if (isNew && newlyArrivedOrder == null) {
                    newlyArrivedOrder = parsedOrder
                }
            }

            if (fetchedOrders.isNotEmpty()) {
                val merged = (_orders.value.filter { local -> fetchedOrders.none { it.orderId == local.orderId } } + fetchedOrders)
                    .sortedByDescending { it.createdAt }
                _orders.value = merged
                recalculateCustomers(merged)

                newlyArrivedOrder?.let {
                    onNewOrderReceived?.invoke(it)
                }
            }
        } catch (e: Exception) {
            Log.e("SotraRepository", "Firestore Parse error", e)
        }
    }

    /**
     * Mark an order as acknowledged, silencing the continuous alarm.
     */
    fun acknowledgeOrder(orderId: String) {
        val currentList = _orders.value.toMutableList()
        val index = currentList.indexOfFirst { it.orderId == orderId || it.docId == orderId }
        if (index != -1) {
            currentList[index] = currentList[index].copy(isAcknowledged = true)
            _orders.value = currentList
        }
    }

    /**
     * Update order status on local state and push update to Firebase (Firestore & RTDB).
     * Synchronizes all status fields (trackingStatus, status, orderStatus, state, stage, statusText, statusAr)
     * so that customer portal and store dashboards update in real time.
     */
    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        val currentList = _orders.value.toMutableList()
        val index = currentList.indexOfFirst { it.orderId == orderId || it.docId == orderId }
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())

        var targetDocId = orderId
        if (index != -1) {
            val existing = currentList[index]
            targetDocId = existing.docId.ifBlank { existing.orderId }
            currentList[index] = existing.copy(
                trackingStatus = newStatus,
                updatedAt = now,
                isAcknowledged = true
            )
            _orders.value = currentList
            recalculateCustomers(currentList)
        }

        // Push update to Firebase in background
        coroutineScope.launch {
            // 1. RTDB Update
            try {
                val rtdbJson = JSONObject().apply {
                    put("trackingStatus", newStatus.code)
                    put("status", newStatus.code)
                    put("orderStatus", newStatus.code)
                    put("stage", newStatus.code)
                    put("state", newStatus.code)
                    put("currentStatus", newStatus.code)
                    put("stageNumber", newStatus.stageNumber)
                    put("trackingStatusText", newStatus.labelAr)
                    put("statusText", newStatus.labelAr)
                    put("statusAr", newStatus.labelAr)
                    put("statusLabel", newStatus.labelAr)
                    put("statusTitle", newStatus.labelAr)
                    put("updatedAt", now)
                }
                val rtdbBody = rtdbJson.toString().toRequestBody("application/json".toMediaType())

                val rtdbUrl1 = "$FIREBASE_RTDB_URL/orders/$targetDocId.json?key=$FIREBASE_API_KEY"
                client.newCall(Request.Builder().url(rtdbUrl1).patch(rtdbBody).build()).execute().close()

                if (targetDocId != orderId && orderId.isNotBlank()) {
                    val rtdbUrl2 = "$FIREBASE_RTDB_URL/orders/$orderId.json?key=$FIREBASE_API_KEY"
                    client.newCall(Request.Builder().url(rtdbUrl2).patch(rtdbBody).build()).execute().close()
                }
            } catch (e: Exception) {
                Log.w("SotraRepository", "RTDB status push note: ${e.message}")
            }

            // 2. Firestore Update
            try {
                val fields = JSONObject().apply {
                    put("trackingStatus", JSONObject().put("stringValue", newStatus.code))
                    put("status", JSONObject().put("stringValue", newStatus.code))
                    put("orderStatus", JSONObject().put("stringValue", newStatus.code))
                    put("stage", JSONObject().put("stringValue", newStatus.code))
                    put("state", JSONObject().put("stringValue", newStatus.code))
                    put("currentStatus", JSONObject().put("stringValue", newStatus.code))
                    put("stageNumber", JSONObject().put("integerValue", "${newStatus.stageNumber}"))
                    put("trackingStatusText", JSONObject().put("stringValue", newStatus.labelAr))
                    put("statusText", JSONObject().put("stringValue", newStatus.labelAr))
                    put("statusAr", JSONObject().put("stringValue", newStatus.labelAr))
                    put("statusLabel", JSONObject().put("stringValue", newStatus.labelAr))
                    put("statusTitle", JSONObject().put("stringValue", newStatus.labelAr))
                    put("updatedAt", JSONObject().put("stringValue", now))
                }
                val firestoreJson = JSONObject().apply {
                    put("fields", fields)
                }
                val firestoreBody = firestoreJson.toString().toRequestBody("application/json".toMediaType())

                val queryMask = "updateMask.fieldPaths=trackingStatus&updateMask.fieldPaths=status&updateMask.fieldPaths=orderStatus&updateMask.fieldPaths=stage&updateMask.fieldPaths=state&updateMask.fieldPaths=currentStatus&updateMask.fieldPaths=stageNumber&updateMask.fieldPaths=trackingStatusText&updateMask.fieldPaths=statusText&updateMask.fieldPaths=statusAr&updateMask.fieldPaths=statusLabel&updateMask.fieldPaths=statusTitle&updateMask.fieldPaths=updatedAt"

                val firestoreUrl1 = "$FIRESTORE_BASE_URL/orders/$targetDocId?key=$FIREBASE_API_KEY&$queryMask"
                client.newCall(Request.Builder().url(firestoreUrl1).patch(firestoreBody).build()).execute().close()

                if (targetDocId != orderId && orderId.isNotBlank()) {
                    val firestoreUrl2 = "$FIRESTORE_BASE_URL/orders/$orderId?key=$FIREBASE_API_KEY&$queryMask"
                    client.newCall(Request.Builder().url(firestoreUrl2).patch(firestoreBody).build()).execute().close()
                }
            } catch (e: Exception) {
                Log.w("SotraRepository", "Firestore status push note: ${e.message}")
            }
        }
    }

    fun savePaymentConfig(config: StorePaymentConfig) {
        _paymentConfig.value = config
        prefs.edit()
            .putBoolean("vc_enabled", config.vodafoneCashEnabled)
            .putString("vc_number", config.vodafoneCashNumber)
            .putString("vc_name", config.vodafoneCashAccountName)
            .putString("vc_inst", config.vodafoneCashInstructionsAr)
            .putBoolean("ip_enabled", config.instaPayEnabled)
            .putString("ip_id", config.instaPayId)
            .putString("ip_name", config.instaPayAccountName)
            .putString("ip_inst", config.instaPayInstructionsAr)
            .apply()

        // Sync payment settings to Firebase RTDB & Firestore under all common store endpoints
        coroutineScope.launch {
            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
            
            // 1. RTDB Payload with full aliases for compatibility with all web and client versions
            try {
                val json = JSONObject().apply {
                    put("vodafoneCashEnabled", config.vodafoneCashEnabled)
                    put("vodafone_cash_enabled", config.vodafoneCashEnabled)
                    put("vodafoneCashNumber", config.vodafoneCashNumber)
                    put("vodafone_cash_number", config.vodafoneCashNumber)
                    put("vodafoneNumber", config.vodafoneCashNumber)
                    put("vodafonePhone", config.vodafoneCashNumber)
                    put("vodafone_phone", config.vodafoneCashNumber)
                    put("vodafoneCashAccountName", config.vodafoneCashAccountName)
                    put("vodafone_cash_account_name", config.vodafoneCashAccountName)
                    put("vodafoneCashInstructionsAr", config.vodafoneCashInstructionsAr)
                    put("vodafone_cash_instructions_ar", config.vodafoneCashInstructionsAr)
                    put("instaPayEnabled", config.instaPayEnabled)
                    put("instapay_enabled", config.instaPayEnabled)
                    put("instaPayId", config.instaPayId)
                    put("instapay_id", config.instaPayId)
                    put("instaPayAddress", config.instaPayId)
                    put("instaPayAccountName", config.instaPayAccountName)
                    put("instapay_account_name", config.instaPayAccountName)
                    put("instaPayInstructionsAr", config.instaPayInstructionsAr)
                    put("instapay_instructions_ar", config.instaPayInstructionsAr)
                    put("advanceShippingFeeOnly", config.advanceShippingFeeOnly)
                    put("updatedAt", now)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())

                val paths = listOf("/settings/payment.json", "/payment.json", "/payment_settings.json", "/config/payment.json")
                for (p in paths) {
                    try {
                        val url = "$FIREBASE_RTDB_URL$p?key=$FIREBASE_API_KEY"
                        client.newCall(Request.Builder().url(url).put(body).build()).execute().close()
                    } catch (e: Exception) {
                        // ignore individual path failures
                    }
                }
            } catch (e: Exception) {
                Log.w("SotraRepository", "Payment RTDB sync note: ${e.message}")
            }

            // 2. Firestore Sync
            try {
                val fields = JSONObject().apply {
                    put("vodafoneCashEnabled", JSONObject().put("booleanValue", config.vodafoneCashEnabled))
                    put("vodafoneCashNumber", JSONObject().put("stringValue", config.vodafoneCashNumber))
                    put("vodafone_cash_number", JSONObject().put("stringValue", config.vodafoneCashNumber))
                    put("vodafoneCashAccountName", JSONObject().put("stringValue", config.vodafoneCashAccountName))
                    put("vodafoneCashInstructionsAr", JSONObject().put("stringValue", config.vodafoneCashInstructionsAr))
                    put("instaPayEnabled", JSONObject().put("booleanValue", config.instaPayEnabled))
                    put("instaPayId", JSONObject().put("stringValue", config.instaPayId))
                    put("instapay_id", JSONObject().put("stringValue", config.instaPayId))
                    put("instaPayAccountName", JSONObject().put("stringValue", config.instaPayAccountName))
                    put("instaPayInstructionsAr", JSONObject().put("stringValue", config.instaPayInstructionsAr))
                    put("advanceShippingFeeOnly", JSONObject().put("booleanValue", config.advanceShippingFeeOnly))
                    put("updatedAt", JSONObject().put("stringValue", now))
                }
                val firestoreJson = JSONObject().put("fields", fields)
                val firestoreBody = firestoreJson.toString().toRequestBody("application/json".toMediaType())

                val fsUrls = listOf(
                    "$FIRESTORE_BASE_URL/settings/payment?key=$FIREBASE_API_KEY",
                    "$FIRESTORE_BASE_URL/config/payment?key=$FIREBASE_API_KEY"
                )
                for (fUrl in fsUrls) {
                    try {
                        client.newCall(Request.Builder().url(fUrl).patch(firestoreBody).build()).execute().close()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            } catch (e: Exception) {
                Log.w("SotraRepository", "Payment Firestore sync note: ${e.message}")
            }
        }
    }

    fun setWholesalePrice(productKey: String, price: Double) {
        val map = _wholesalePrices.value.toMutableMap()
        map[productKey] = price
        _wholesalePrices.value = map

        val jsonObj = JSONObject()
        map.forEach { (k, v) -> jsonObj.put(k, v) }
        prefs.edit().putString("wholesale_prices_map", jsonObj.toString()).apply()

        // Sync wholesale prices to RTDB
        coroutineScope.launch {
            try {
                val url = "$FIREBASE_RTDB_URL/settings/wholesale_prices.json?key=$FIREBASE_API_KEY"
                val body = jsonObj.toString().toRequestBody("application/json".toMediaType())
                client.newCall(Request.Builder().url(url).put(body).build()).execute().close()
            } catch (e: Exception) {
                Log.w("SotraRepository", "Wholesale price sync note: ${e.message}")
            }
        }
    }

    fun saveGovernorates(govs: List<GovernorateShipping>) {
        _governorates.value = govs
        coroutineScope.launch {
            try {
                val url = "$FIREBASE_RTDB_URL/settings/shipping.json?key=$FIREBASE_API_KEY"
                val arr = JSONArray()
                govs.forEach { g ->
                    arr.put(JSONObject().apply {
                        put("id", g.id)
                        put("nameAr", g.nameAr)
                        put("nameEn", g.nameEn)
                        put("shippingCost", g.shippingCost)
                        put("deliveryDays", g.deliveryDays)
                    })
                }
                val body = arr.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(url).put(body).build()
                client.newCall(request).execute().close()
            } catch (e: Exception) {
                Log.w("SotraRepository", "Shipping sync to Firebase note: ${e.message}")
            }
        }
    }
}

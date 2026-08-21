package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SotraRepository
import com.example.model.*
import com.example.sound.OrderSoundManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class OrdersViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SotraRepository(application.applicationContext)
    val soundManager = OrderSoundManager(application.applicationContext)

    // Navigation Tab Index (0: Active, 1: Sales Log, 2: Archive, 3: Customers, 4: Payment, 5: Shipping, 6: Sound Settings)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Filters and Search Queries
    private val _activeSearchQuery = MutableStateFlow("")
    val activeSearchQuery: StateFlow<String> = _activeSearchQuery.asStateFlow()

    private val _activeStatusFilter = MutableStateFlow<String>("all")
    val activeStatusFilter: StateFlow<String> = _activeStatusFilter.asStateFlow()

    private val _archiveSearchQuery = MutableStateFlow("")
    val archiveSearchQuery: StateFlow<String> = _archiveSearchQuery.asStateFlow()

    private val _archiveStatusFilter = MutableStateFlow<String>("all")
    val archiveStatusFilter: StateFlow<String> = _archiveStatusFilter.asStateFlow()

    private val _customersSearchQuery = MutableStateFlow("")
    val customersSearchQuery: StateFlow<String> = _customersSearchQuery.asStateFlow()

    // Sales Log Filters
    private val _salesTimeFilter = MutableStateFlow(SalesTimeFilter.ALL_TIME)
    val salesTimeFilter: StateFlow<SalesTimeFilter> = _salesTimeFilter.asStateFlow()

    private val _salesCategoryFilter = MutableStateFlow("الكل")
    val salesCategoryFilter: StateFlow<String> = _salesCategoryFilter.asStateFlow()

    private val _salesSearchQuery = MutableStateFlow("")
    val salesSearchQuery: StateFlow<String> = _salesSearchQuery.asStateFlow()

    // Selected order for detailed modal view
    private val _selectedOrder = MutableStateFlow<Order?>(null)
    val selectedOrder: StateFlow<Order?> = _selectedOrder.asStateFlow()

    // Toast / Feedback message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Repositories Data
    val orders = repository.orders
    val customers = repository.customers
    val paymentConfig = repository.paymentConfig
    val governorates = repository.governorates
    val soundSettings = repository.soundSettings
    val wholesalePrices = repository.wholesalePrices
    val isConnected = repository.isConnected
    val isSyncing = repository.isSyncing

    // Sound states
    val isAlarmPlaying = soundManager.isAlarmPlaying
    val activeAlarmOrder = soundManager.activeAlarmOrder

    // Filtered Active Orders (excluding delivered & cancelled)
    val filteredActiveOrders: StateFlow<List<Order>> = combine(
        orders,
        _activeSearchQuery,
        _activeStatusFilter
    ) { orderList, query, filter ->
        var list = orderList.filter { it.trackingStatus != OrderStatus.DELIVERED && it.trackingStatus != OrderStatus.CANCELLED }
        
        if (filter != "all") {
            list = list.filter { it.trackingStatus.code == filter }
        }
        
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter { o ->
                o.orderId.lowercase().contains(q) ||
                o.customer.fullName.lowercase().contains(q) ||
                o.customer.phoneNumber.contains(q) ||
                o.customer.governorateNameAr.lowercase().contains(q)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Archive Orders (only delivered & cancelled)
    val filteredArchiveOrders: StateFlow<List<Order>> = combine(
        orders,
        _archiveSearchQuery,
        _archiveStatusFilter
    ) { orderList, query, filter ->
        var list = orderList.filter { it.trackingStatus == OrderStatus.DELIVERED || it.trackingStatus == OrderStatus.CANCELLED }
        
        if (filter != "all") {
            list = list.filter { it.trackingStatus.code == filter }
        }
        
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter { o ->
                o.orderId.lowercase().contains(q) ||
                o.customer.fullName.lowercase().contains(q) ||
                o.customer.phoneNumber.contains(q) ||
                o.customer.governorateNameAr.lowercase().contains(q)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Customers
    val filteredCustomers: StateFlow<List<Customer>> = combine(
        customers,
        _customersSearchQuery
    ) { custList, query ->
        if (query.isBlank()) {
            custList
        } else {
            val q = query.trim().lowercase()
            custList.filter { c ->
                c.fullName.lowercase().contains(q) ||
                c.phoneNumber.contains(q) ||
                c.governorateNameAr.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Product Sales Log aggregated from valid orders
    val filteredSalesLog: StateFlow<List<ProductSalesSummary>> = combine(
        orders,
        wholesalePrices,
        _salesTimeFilter,
        _salesCategoryFilter,
        _salesSearchQuery
    ) { orderList, wholesaleMap, timeFilter, categoryFilter, searchQuery ->
        // Exclude cancelled orders
        val validOrders = orderList.filter { it.trackingStatus != OrderStatus.CANCELLED && isOrderInTimeFilter(it, timeFilter) }

        val aggregatedMap = mutableMapOf<String, ProductSalesSummary>()

        for (order in validOrders) {
            for (item in order.items) {
                if (item.titleAr.isBlank() && item.price <= 0.0) continue

                val colorName = item.selectedColor.nameAr.ifBlank { "اللون الأساسي" }
                val sizeName = item.selectedSize.ifBlank { "مقاس موحد" }
                val title = item.titleAr.ifBlank { "قطعة ملابس سترة" }
                val category = CategoryHelper.detectCategory(title)

                val productKey = "${title}_${colorName}_${sizeName}".replace(" ", "_")
                val unitSell = if (item.price > 0) item.price else 0.0
                
                // Wholesale price: look up by specific variant key, then by title, or default to 0.0
                val unitWholesale = wholesaleMap[productKey]
                    ?: wholesaleMap[title.replace(" ", "_")]
                    ?: wholesaleMap[title]
                    ?: 0.0

                val existing = aggregatedMap[productKey]
                if (existing != null) {
                    val newQty = existing.totalQuantitySold + item.quantity
                    val newOrdersCount = existing.ordersCount + 1
                    aggregatedMap[productKey] = existing.copy(
                        totalQuantitySold = newQty,
                        unitWholesalePrice = unitWholesale,
                        ordersCount = newOrdersCount,
                        lastSoldDate = order.createdAt.ifBlank { existing.lastSoldDate }
                    )
                } else {
                    aggregatedMap[productKey] = ProductSalesSummary(
                        productKey = productKey,
                        titleAr = title,
                        color = colorName,
                        size = sizeName,
                        categoryAr = category,
                        totalQuantitySold = item.quantity,
                        unitSellingPrice = unitSell,
                        unitWholesalePrice = unitWholesale,
                        ordersCount = 1,
                        lastSoldDate = order.createdAt
                    )
                }
            }
        }

        var result = aggregatedMap.values.toList()

        // Filter by Category
        if (categoryFilter != "الكل") {
            result = result.filter { it.categoryAr == categoryFilter }
        }

        // Filter by Search query
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            result = result.filter { item ->
                item.titleAr.lowercase().contains(q) ||
                item.color.lowercase().contains(q) ||
                item.size.lowercase().contains(q) ||
                item.categoryAr.lowercase().contains(q)
            }
        }

        // Sort by quantity sold descending
        result.sortedByDescending { it.totalQuantitySold }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        com.example.sound.NotificationHelper.initNotificationChannel(application.applicationContext)

        // Set up callback when a new order arrives from repository:
        repository.onNewOrderReceived = { newOrder ->
            viewModelScope.launch {
                com.example.sound.NotificationHelper.showNewOrderNotification(application.applicationContext, newOrder)
                soundManager.startContinuousAlarm(newOrder, repository.soundSettings.value)
                _snackbarMessage.value = "🔔 طلب جديد وصل: #${newOrder.orderId} (${newOrder.customer.fullName})"
            }
        }
    }

    fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun setActiveSearchQuery(q: String) {
        _activeSearchQuery.value = q
    }

    fun setActiveStatusFilter(filter: String) {
        _activeStatusFilter.value = filter
    }

    fun setArchiveSearchQuery(q: String) {
        _archiveSearchQuery.value = q
    }

    fun setArchiveStatusFilter(filter: String) {
        _archiveStatusFilter.value = filter
    }

    fun setCustomersSearchQuery(q: String) {
        _customersSearchQuery.value = q
    }

    fun setSalesTimeFilter(filter: SalesTimeFilter) {
        _salesTimeFilter.value = filter
    }

    fun setSalesCategoryFilter(category: String) {
        _salesCategoryFilter.value = category
    }

    fun setSalesSearchQuery(query: String) {
        _salesSearchQuery.value = query
    }

    fun setProductWholesalePrice(productKey: String, cost: Double) {
        repository.setWholesalePrice(productKey, cost)
        _snackbarMessage.value = "تم حفظ سعر الجملة وتحديث حسابات الأرباح فوراً"
    }

    private fun isOrderInTimeFilter(order: Order, filter: SalesTimeFilter): Boolean {
        if (filter == SalesTimeFilter.ALL_TIME) return true
        val dateStr = order.createdAt.trim()
        if (dateStr.isBlank()) return true

        val calOrder = Calendar.getInstance()
        var parsed = false
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("yyyy/MM/dd", Locale.US),
            SimpleDateFormat("dd-MM-yyyy", Locale.US),
            SimpleDateFormat("dd/MM/yyyy", Locale.US)
        )
        for (f in formats) {
            try {
                val d = f.parse(dateStr)
                if (d != null) {
                    calOrder.time = d
                    parsed = true
                    break
                }
            } catch (e: Exception) {
                // Try next format
            }
        }

        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return when (filter) {
            SalesTimeFilter.TODAY -> {
                if (!parsed) true else calOrder.timeInMillis >= todayStart.timeInMillis
            }
            SalesTimeFilter.YESTERDAY -> {
                val yesterdayStart = (todayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
                if (!parsed) false else (calOrder.timeInMillis >= yesterdayStart.timeInMillis && calOrder.timeInMillis < todayStart.timeInMillis)
            }
            SalesTimeFilter.LAST_7_DAYS -> {
                val days7Ago = (todayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7) }
                if (!parsed) true else calOrder.timeInMillis >= days7Ago.timeInMillis
            }
            SalesTimeFilter.THIS_MONTH -> {
                val days30Ago = (todayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -30) }
                if (!parsed) true else calOrder.timeInMillis >= days30Ago.timeInMillis
            }
            SalesTimeFilter.ALL_TIME -> true
        }
    }

    fun openOrderDetail(order: Order) {
        _selectedOrder.value = order
    }

    fun closeOrderDetail() {
        _selectedOrder.value = null
    }

    /**
     * CRITICAL USER REQUIREMENT:
     * Clicking "تم استلام الطلب" (Acknowledge Order) stops the continuous alarm sound immediately
     * and marks the order as acknowledged.
     */
    fun acknowledgeOrder(orderId: String) {
        soundManager.stopAlarm()
        repository.acknowledgeOrder(orderId)
        _selectedOrder.value?.let { current ->
            if (current.orderId == orderId) {
                _selectedOrder.value = current.copy(isAcknowledged = true)
            }
        }
        _snackbarMessage.value = "✅ تم تأكيد استلام الطلب رقم #$orderId وإيقاف التنبيه الصوتي"
    }

    fun stopAlarmDirectly() {
        soundManager.stopAlarm()
        _snackbarMessage.value = "تم إيقاف صوت التنبيه"
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        soundManager.stopAlarm()
        repository.updateOrderStatus(orderId, newStatus)
        _selectedOrder.value?.let { current ->
            if (current.orderId == orderId) {
                _selectedOrder.value = current.copy(trackingStatus = newStatus, isAcknowledged = true)
            }
        }
        _snackbarMessage.value = "تم تحديث حالة الطلب #$orderId إلى '${newStatus.labelAr}'"
    }

    fun previewRingtone(option: RingtoneOption) {
        soundManager.previewRingtone(option, soundSettings.value.alarmVolume)
    }

    fun saveSoundSettings(settings: AppSoundSettings) {
        repository.updateSoundSettings(settings)
        _snackbarMessage.value = "تم حفظ إعدادات النغمات بنجاح"
    }

    fun savePaymentConfig(config: StorePaymentConfig) {
        repository.savePaymentConfig(config)
        _snackbarMessage.value = "تم حفظ إعدادات الدفع بنجاح"
    }

    fun saveGovernorates(list: List<GovernorateShipping>) {
        repository.saveGovernorates(list)
        _snackbarMessage.value = "تم حفظ أسعار الشحن للمحافظات بنجاح"
    }

    fun addGovernorate(gov: GovernorateShipping) {
        val current = governorates.value.toMutableList()
        current.add(0, gov)
        repository.saveGovernorates(current)
        _snackbarMessage.value = "تمت إضافة محافظة ${gov.nameAr}"
    }

    fun deleteGovernorate(id: String) {
        val current = governorates.value.filter { it.id != id }
        repository.saveGovernorates(current)
        _snackbarMessage.value = "تم حذف المحافظة"
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun testNotificationAndSound() {
        val testOrder = Order(
            orderId = "TEST-${(1000..9999).random()}",
            customer = CustomerInfo(
                fullName = "عميل تجربة سترة",
                phoneNumber = "01012345678",
                governorateNameAr = "القاهرة"
            ),
            total = 750.0,
            paymentMethod = "vodafone_cash",
            trackingStatus = OrderStatus.ORDER_RECEIVED
        )
        viewModelScope.launch {
            val appCtx = getApplication<android.app.Application>().applicationContext
            com.example.sound.NotificationHelper.showNewOrderNotification(appCtx, testOrder)
            soundManager.startContinuousAlarm(testOrder, repository.soundSettings.value)
            _snackbarMessage.value = "🔔 جاري تشغيل إشعار ورنين تجريبي! اضغط [تم استلام الطلب] لإيقاف الصوت"
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            repository.syncWithFirebase()
            _snackbarMessage.value = "تم تحديث ومزامنة البيانات من قاعدة بيانات سترة فاشون"
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.stopAlarm()
    }
}

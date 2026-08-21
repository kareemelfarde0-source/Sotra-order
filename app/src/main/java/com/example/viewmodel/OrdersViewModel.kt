package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SotraRepository
import com.example.model.*
import com.example.sound.OrderSoundManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class OrdersViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SotraRepository(application.applicationContext)
    val soundManager = OrderSoundManager(application.applicationContext)

    // Navigation Tab Index (0: Active, 1: Archive, 2: Customers, 3: Payment, 4: Shipping, 5: Sound Settings)
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

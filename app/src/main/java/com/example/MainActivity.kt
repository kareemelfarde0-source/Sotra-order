package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AlarmBanner
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.OrdersViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    SotraAppMain()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SotraAppMain(viewModel: OrdersViewModel = viewModel()) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isAlarmPlaying by viewModel.isAlarmPlaying.collectAsState()
    val activeAlarmOrder by viewModel.activeAlarmOrder.collectAsState()
    val selectedOrder by viewModel.selectedOrder.collectAsState()
    val activeOrders by viewModel.filteredActiveOrders.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Request notification permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val tabTitles = listOf(
        "الطلبات الحالية",
        "أرشيف الطلبات",
        "العملاء",
        "إعدادات الدفع",
        "تسعير الشحن",
        "نغمات التنبيه"
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SotraPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "سترة فاشون",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = SotraSecondary
                            )
                            Text(
                                text = tabTitles.getOrElse(selectedTab) { "إدارة الطلبات" },
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                },
                actions = {
                    // Connection Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isConnected) Color(0xFFDCFCE7) else Color(0xFFFEF3C7))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) SotraSuccess else SotraGold)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isConnected) "متصل" else "محلي",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) SotraSuccess else Color(0xFFB45309)
                            )
                        }
                    }

                    // Refresh Action
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث",
                            tint = Color(0xFF475569)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Tab 0: Active Orders
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (activeOrders.isNotEmpty()) {
                                    Badge(containerColor = SotraPrimary) {
                                        Text("${activeOrders.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Inbox, contentDescription = "الطلبات")
                        }
                    },
                    label = { Text("الطلبات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SotraPrimary,
                        selectedTextColor = SotraPrimary,
                        indicatorColor = SotraPrimaryLight
                    )
                )

                // Tab 1: Archive
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    icon = { Icon(Icons.Default.Archive, contentDescription = "الأرشيف") },
                    label = { Text("الأرشيف", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SotraPrimary,
                        selectedTextColor = SotraPrimary,
                        indicatorColor = SotraPrimaryLight
                    )
                )

                // Tab 2: Customers
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    icon = { Icon(Icons.Default.People, contentDescription = "العملاء") },
                    label = { Text("العملاء", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SotraPrimary,
                        selectedTextColor = SotraPrimary,
                        indicatorColor = SotraPrimaryLight
                    )
                )

                // Tab 3: Payment Settings
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.setSelectedTab(3) },
                    icon = { Icon(Icons.Default.Payments, contentDescription = "الدفع") },
                    label = { Text("الدفع", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SotraPrimary,
                        selectedTextColor = SotraPrimary,
                        indicatorColor = SotraPrimaryLight
                    )
                )

                // Tab 4: Shipping Settings
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { viewModel.setSelectedTab(4) },
                    icon = { Icon(Icons.Default.LocalShipping, contentDescription = "الشحن") },
                    label = { Text("الشحن", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SotraPrimary,
                        selectedTextColor = SotraPrimary,
                        indicatorColor = SotraPrimaryLight
                    )
                )

                // Tab 5: Sound & Ringtone Settings
                NavigationBarItem(
                    selected = selectedTab == 5,
                    onClick = { viewModel.setSelectedTab(5) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (isAlarmPlaying) {
                                    Badge(containerColor = SotraError) {
                                        Text("!")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "النغمات")
                        }
                    },
                    label = { Text("النغمات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SotraPrimary,
                        selectedTextColor = SotraPrimary,
                        indicatorColor = SotraPrimaryLight
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // UNCEASING ALARM BANNER (Always visible when order alarm is playing)
            AlarmBanner(
                isAlarmPlaying = isAlarmPlaying,
                activeOrder = activeAlarmOrder,
                onAcknowledge = { orderId ->
                    if (orderId.isNotBlank()) {
                        viewModel.acknowledgeOrder(orderId)
                    } else {
                        viewModel.stopAlarmDirectly()
                    }
                },
                onViewDetails = { order ->
                    viewModel.openOrderDetail(order)
                }
            )

            // Screen Content according to Selected Tab
            when (selectedTab) {
                0 -> ActiveOrdersScreen(viewModel = viewModel)
                1 -> ArchiveOrdersScreen(viewModel = viewModel)
                2 -> CustomersScreen(viewModel = viewModel)
                3 -> PaymentSettingsScreen(viewModel = viewModel)
                4 -> ShippingSettingsScreen(viewModel = viewModel)
                5 -> SoundSettingsScreen(viewModel = viewModel)
            }
        }
    }

    // Modal Order Detail Dialog
    selectedOrder?.let { order ->
        OrderDetailDialog(
            order = order,
            viewModel = viewModel,
            onDismiss = { viewModel.closeOrderDetail() }
        )
    }
}

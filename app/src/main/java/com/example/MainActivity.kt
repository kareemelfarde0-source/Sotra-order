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

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    // Request notification permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission) {
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
        "سجل المبيعات والأرباح",
        "أرشيف الطلبات",
        "العملاء",
        "إعدادات الدفع",
        "إعدادات المتجر والتنبيهات"
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

                // Tab 1: Sales Log (سجل المبيعات)
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    icon = { Icon(Icons.Default.PointOfSale, contentDescription = "المبيعات") },
                    label = { Text("المبيعات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SotraPrimary,
                        selectedTextColor = SotraPrimary,
                        indicatorColor = SotraPrimaryLight
                    )
                )

                // Tab 2: Archive
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    icon = { Icon(Icons.Default.Archive, contentDescription = "الأرشيف") },
                    label = { Text("الأرشيف", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SotraPrimary,
                        selectedTextColor = SotraPrimary,
                        indicatorColor = SotraPrimaryLight
                    )
                )

                // Tab 3: Customers
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.setSelectedTab(3) },
                    icon = { Icon(Icons.Default.People, contentDescription = "العملاء") },
                    label = { Text("العملاء", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SotraPrimary,
                        selectedTextColor = SotraPrimary,
                        indicatorColor = SotraPrimaryLight
                    )
                )

                // Tab 4: Payment Settings
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { viewModel.setSelectedTab(4) },
                    icon = { Icon(Icons.Default.Payments, contentDescription = "الدفع") },
                    label = { Text("الدفع", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SotraPrimary,
                        selectedTextColor = SotraPrimary,
                        indicatorColor = SotraPrimaryLight
                    )
                )

                // Tab 5: Settings (Shipping & Sound Ringtone)
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
                            Icon(Icons.Default.Settings, contentDescription = "الإعدادات")
                        }
                    },
                    label = { Text("الإعدادات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
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

            // NOTIFICATION PERMISSION PROMPT (If permission not granted on Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "إذن الإشعارات والأصوات مطلوب",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "لتلقي رنين وتنبيه الطلبات الجديدة فور وصولها",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                        Button(
                            onClick = {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("منح الإذن", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Screen Content according to Selected Tab
            when (selectedTab) {
                0 -> ActiveOrdersScreen(viewModel = viewModel)
                1 -> SalesLogScreen(viewModel = viewModel)
                2 -> ArchiveOrdersScreen(viewModel = viewModel)
                3 -> CustomersScreen(viewModel = viewModel)
                4 -> PaymentSettingsScreen(viewModel = viewModel)
                5 -> StoreSettingsScreen(viewModel = viewModel)
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

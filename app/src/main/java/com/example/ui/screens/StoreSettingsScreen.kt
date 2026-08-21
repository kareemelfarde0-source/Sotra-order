package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SotraPrimary
import com.example.viewmodel.OrdersViewModel

@Composable
fun StoreSettingsScreen(
    viewModel: OrdersViewModel,
    modifier: Modifier = Modifier
) {
    var subTab by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Tab Selector between Shipping and Sounds
        TabRow(
            selectedTabIndex = subTab,
            containerColor = Color.White,
            contentColor = SotraPrimary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("تسعير الشحن للمحافظات", fontWeight = if (subTab == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                    }
                }
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("نغمات وأصوات التنبيه", fontWeight = if (subTab == 1) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                    }
                }
            )
        }

        when (subTab) {
            0 -> ShippingSettingsScreen(viewModel = viewModel)
            1 -> SoundSettingsScreen(viewModel = viewModel)
        }
    }
}

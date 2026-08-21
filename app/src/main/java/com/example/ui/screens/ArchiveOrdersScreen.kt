package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OrderStatus
import com.example.ui.components.OrderCard
import com.example.ui.theme.SotraError
import com.example.ui.theme.SotraPrimary
import com.example.ui.theme.SotraSuccess
import com.example.viewmodel.OrdersViewModel

@Composable
fun ArchiveOrdersScreen(
    viewModel: OrdersViewModel,
    modifier: Modifier = Modifier
) {
    val archiveOrders by viewModel.filteredArchiveOrders.collectAsState()
    val searchQuery by viewModel.archiveSearchQuery.collectAsState()
    val statusFilter by viewModel.archiveStatusFilter.collectAsState()

    val deliveredCount = archiveOrders.count { it.trackingStatus == OrderStatus.DELIVERED }
    val deliveredTotal = archiveOrders.filter { it.trackingStatus == OrderStatus.DELIVERED }.sumOf { it.total }
    val cancelledCount = archiveOrders.count { it.trackingStatus == OrderStatus.CANCELLED }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Summary metric cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("تم التوصيل بنجاح", fontSize = 12.sp, color = SotraSuccess, fontWeight = FontWeight.Bold)
                    Text("$deliveredCount طلب", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = SotraSuccess)
                    Text("${deliveredTotal.toInt()} ج.م", fontSize = 11.sp, color = Color(0xFF047857))
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("الملغي والمسترجع", fontSize = 12.sp, color = SotraError, fontWeight = FontWeight.Bold)
                    Text("$cancelledCount طلب", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = SotraError)
                    Text("طلبات ملغية", fontSize = 11.sp, color = Color(0xFF991B1B))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setArchiveSearchQuery(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("بحث في الأرشيف برقم الطلب أو العميل...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "بحث", tint = Color.Gray)
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = SotraPrimary,
                unfocusedBorderColor = Color(0xFFE2E8F0)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Tabs (All, Delivered, Cancelled)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = statusFilter == "all",
                onClick = { viewModel.setArchiveStatusFilter("all") },
                label = { Text("الكل") },
                shape = RoundedCornerShape(20.dp)
            )
            FilterChip(
                selected = statusFilter == OrderStatus.DELIVERED.code,
                onClick = { viewModel.setArchiveStatusFilter(OrderStatus.DELIVERED.code) },
                label = { Text("تم التوصيل ($deliveredCount)") },
                shape = RoundedCornerShape(20.dp)
            )
            FilterChip(
                selected = statusFilter == OrderStatus.CANCELLED.code,
                onClick = { viewModel.setArchiveStatusFilter(OrderStatus.CANCELLED.code) },
                label = { Text("ملغي ($cancelledCount)") },
                shape = RoundedCornerShape(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Archive List
        if (archiveOrders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("لا توجد طلبات في الأرشيف", fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(archiveOrders, key = { it.orderId }) { order ->
                    OrderCard(
                        order = order,
                        onViewDetails = { viewModel.openOrderDetail(it) },
                        onAcknowledge = { viewModel.acknowledgeOrder(it) }
                    )
                }
            }
        }
    }
}

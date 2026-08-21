package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.CategoryHelper
import com.example.model.ProductSalesSummary
import com.example.model.SalesTimeFilter
import com.example.ui.theme.*
import com.example.viewmodel.OrdersViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesLogScreen(
    viewModel: OrdersViewModel,
    modifier: Modifier = Modifier
) {
    val salesList by viewModel.filteredSalesLog.collectAsState()
    val currentTimeFilter by viewModel.salesTimeFilter.collectAsState()
    val currentCategoryFilter by viewModel.salesCategoryFilter.collectAsState()
    val searchQuery by viewModel.salesSearchQuery.collectAsState()

    var productForPriceEdit by remember { mutableStateOf<ProductSalesSummary?>(null) }

    // Aggregate statistics from current filtered list
    val totalUnits = salesList.sumOf { it.totalQuantitySold }
    val totalRevenue = salesList.sumOf { it.totalRevenue }
    val totalCost = salesList.sumOf { it.totalCost }
    val netProfit = totalRevenue - totalCost
    val profitMargin = if (totalRevenue > 0) ((netProfit / totalRevenue) * 100.0) else 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Sticky Header / Search & Filter Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(bottom = 12.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSalesSearchQuery(it) },
                placeholder = { Text("بحث باسم القطعة، الموديل، اللون، أو المقاس...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SotraPrimary) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSalesSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.Gray)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF1F5F9),
                    unfocusedContainerColor = Color(0xFFF1F5F9),
                    focusedBorderColor = SotraPrimary,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )

            // 1. Time / Days Filter Chips (فلتر الأيام)
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = SotraPrimary, modifier = Modifier.size(16.dp))
                    Text("فلتر الأيام والفترة الزمنية:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SalesTimeFilter.entries.forEach { filter ->
                        val isSelected = currentTimeFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSalesTimeFilter(filter) },
                            label = { Text(filter.titleAr, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SotraPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFF1F5F9),
                                labelColor = Color(0xFF334155)
                            ),
                            border = null,
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            // 2. Category / Department Filter Chips (فلتر الأقسام)
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(Icons.Default.Category, contentDescription = null, tint = SotraSecondary, modifier = Modifier.size(16.dp))
                    Text("فلتر الأقسام والتصنيفات:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryHelper.ALL_CATEGORIES.forEach { category ->
                        val isSelected = currentCategoryFilter == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSalesCategoryFilter(category) },
                            label = { Text(category, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SotraSecondary,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFF1F5F9),
                                labelColor = Color(0xFF334155)
                            ),
                            border = null,
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        }

        // Summary KPI Banner Cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Total Sales
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(SotraPrimaryLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AttachMoney, contentDescription = null, tint = SotraPrimary, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إجمالي المبيعات", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${String.format(Locale.US, "%,.0f", totalRevenue)} ج.م",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SotraPrimary
                        )
                        Text("$totalUnits قطعة مباعة", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    }
                }

                // Card 2: Total Cost / Wholesale
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إجمالي الجملة", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${String.format(Locale.US, "%,.0f", totalCost)} ج.م",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFB45309)
                        )
                        Text("التكلفة الإجمالية", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    }
                }

                // Card 3: Net Profit
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDCFCE7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = SotraSuccess, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("صافي الربح", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${String.format(Locale.US, "%,.0f", netProfit)} ج.م",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (netProfit >= 0) SotraSuccess else SotraError
                        )
                        Text("${String.format(Locale.US, "%.1f", profitMargin)}% هامش ربح", fontSize = 10.sp, color = if (netProfit >= 0) SotraSuccess else SotraError)
                    }
                }
            }
        }

        // Section Title & Items Count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "تفاصيل مبيعات القطع (${salesList.size} موديل)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "اضغط على سعر الجملة لتعديله",
                fontSize = 11.sp,
                color = SotraPrimary,
                fontWeight = FontWeight.Medium
            )
        }

        // List of Product Sales Cards
        if (salesList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا توجد مبيعات مسجلة في هذه الفترة أو القسم",
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "جرب اختيار فترة زمنية أخرى أو قسم مختلف",
                        color = Color.DarkGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(salesList, key = { it.productKey }) { item ->
                    ProductSalesCard(
                        item = item,
                        onEditWholesale = {
                            productForPriceEdit = item
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Modal Dialog to Edit Wholesale Price
    productForPriceEdit?.let { item ->
        EditWholesalePriceDialog(
            item = item,
            onDismiss = { productForPriceEdit = null },
            onSave = { newPrice ->
                viewModel.setProductWholesalePrice(item.productKey, newPrice)
                productForPriceEdit = null
            }
        )
    }
}

@Composable
fun ProductSalesCard(
    item: ProductSalesSummary,
    onEditWholesale: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Product Title & Category Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.titleAr,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Category Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFEDE9FE))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(item.categoryAr, fontSize = 10.sp, color = Color(0xFF6D28D9), fontWeight = FontWeight.Bold)
                        }

                        // Color Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("اللون: ${item.color}", fontSize = 10.sp, color = Color(0xFF475569))
                        }

                        // Size Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("المقاس: ${item.size}", fontSize = 10.sp, color = Color(0xFF475569))
                        }
                    }
                }

                // Quantity Sold Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SotraPrimary)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${item.totalQuantitySold}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "قطعة مباعة",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // 4-Quadrant Metrics Grid:
            // 1. سعر البيع | 2. سعر الجملة (قابل للتعديل)
            // 3. إجمالي المبيعات | 4. صافي الربح
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Column 1: سعر البيع
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(8.dp)
                ) {
                    Column {
                        Text("سعر البيع (للقطعة)", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            text = "${String.format(Locale.US, "%.0f", item.unitSellingPrice)} ج.م",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF1E293B)
                        )
                    }
                }

                // Column 2: سعر الجملة (Interactive to Edit)
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFFBEB))
                        .clickable { onEditWholesale() }
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("سعر الجملة (التكلفة)", fontSize = 10.sp, color = Color(0xFFB45309))
                            Text(
                                text = if (item.unitWholesalePrice > 0) "${String.format(Locale.US, "%.0f", item.unitWholesalePrice)} ج.م" else "غير محدد ✏️",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (item.unitWholesalePrice > 0) Color(0xFF92400E) else Color(0xFFDC2626)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل سعر الجملة",
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Column 3: إجمالي المبيعات
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SotraPrimaryLight.copy(alpha = 0.5f))
                        .padding(8.dp)
                ) {
                    Column {
                        Text("إجمالي المبيعات", fontSize = 10.sp, color = SotraPrimary)
                        Text(
                            text = "${String.format(Locale.US, "%,.0f", item.totalRevenue)} ج.م",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = SotraPrimary
                        )
                    }
                }

                // Column 4: صافي الربح
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (item.netProfit >= 0) Color(0xFFDCFCE7) else Color(0xFFFEE2E2))
                        .padding(8.dp)
                ) {
                    Column {
                        Text("صافي الأرباح", fontSize = 10.sp, color = if (item.netProfit >= 0) SotraSuccess else SotraError)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${String.format(Locale.US, "%,.0f", item.netProfit)} ج.م",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = if (item.netProfit >= 0) SotraSuccess else SotraError
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            if (item.unitWholesalePrice > 0 && item.profitMarginPercent > 0) {
                                Text(
                                    text = "(${String.format(Locale.US, "%.0f", item.profitMarginPercent)}%)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SotraSuccess
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditWholesalePriceDialog(
    item: ProductSalesSummary,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var priceText by remember { mutableStateOf(if (item.unitWholesalePrice > 0) String.format(Locale.US, "%.0f", item.unitWholesalePrice) else "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            text = "تحديد سعر الجملة / التكلفة",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = item.titleAr,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detail badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("اللون: ${item.color}", fontSize = 11.sp, color = Color(0xFF334155))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("المقاس: ${item.size}", fontSize = 11.sp, color = Color(0xFF334155))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SotraPrimaryLight)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("سعر البيع: ${String.format(Locale.US, "%.0f", item.unitSellingPrice)} ج.م", fontSize = 11.sp, color = SotraPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("سعر الجملة للقطعة (ج.م)") },
                    placeholder = { Text("مثال: 250") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedBorderColor = SotraPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Instant calculation preview
                val enteredPrice = priceText.toDoubleOrNull() ?: 0.0
                if (enteredPrice > 0) {
                    val profitPerPiece = item.unitSellingPrice - enteredPrice
                    val totalItemProfit = profitPerPiece * item.totalQuantitySold
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (profitPerPiece >= 0) Color(0xFFDCFCE7) else Color(0xFFFEE2E2))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "معاينة الحسابات فورياً:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (profitPerPiece >= 0) Color(0xFF166534) else Color(0xFF991B1B)
                            )
                            Text(
                                text = "• ربح القطعة الواحدة: ${String.format(Locale.US, "%.0f", profitPerPiece)} ج.م",
                                fontSize = 12.sp,
                                color = if (profitPerPiece >= 0) SotraSuccess else SotraError
                            )
                            Text(
                                text = "• إجمالي أرباح هذه القطعة (${item.totalQuantitySold} قطعة): ${String.format(Locale.US, "%,.0f", totalItemProfit)} ج.م",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (profitPerPiece >= 0) SotraSuccess else SotraError
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إلغاء", color = Color.Gray)
                    }

                    Button(
                        onClick = {
                            val price = priceText.toDoubleOrNull() ?: 0.0
                            onSave(price)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SotraPrimary)
                    ) {
                        Text("حفظ السعر", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

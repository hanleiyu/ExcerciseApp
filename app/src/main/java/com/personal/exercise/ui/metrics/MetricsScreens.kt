package com.personal.exercise.ui.metrics

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.exercise.domain.formatSigned
import com.personal.exercise.domain.toZhDate
import com.personal.exercise.ui.MetricDetailViewModel
import com.personal.exercise.ui.MetricListItem
import com.personal.exercise.ui.MetricsViewModel
import com.personal.exercise.ui.components.AppCard
import com.personal.exercise.ui.components.EmptyHint
import com.personal.exercise.ui.components.LineChart
import com.personal.exercise.ui.components.PrimaryButton
import com.personal.exercise.ui.components.ScreenHeader
import com.personal.exercise.ui.components.Segmented
import com.personal.exercise.ui.components.StatusPill
import com.personal.exercise.ui.theme.ChartGreen
import com.personal.exercise.ui.theme.Gold
import com.personal.exercise.ui.theme.Green
import com.personal.exercise.ui.theme.PillGreen
import com.personal.exercise.ui.theme.PillGreenText
import com.personal.exercise.ui.theme.ScreenPad
import com.personal.exercise.ui.theme.TextMain
import com.personal.exercise.ui.theme.TextMuted
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs

@Composable
fun MetricsScreen(
    vm: MetricsViewModel,
    onOpen: (Long) -> Unit,
) {
    val items by vm.items.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPad)
            .padding(top = 20.dp, bottom = 100.dp),
    ) {
        ScreenHeader("体能指标", "${items.size} 项指标 · 点进查看趋势")
        Spacer(Modifier.height(16.dp))
        if (items.isEmpty()) {
            EmptyHint("还没有指标")
        }
        items.forEach { item ->
            MetricRow(item) { onOpen(item.metric.id) }
            Spacer(Modifier.height(10.dp))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, Green, RoundedCornerShape(22.dp))
                .clickable { showCreate = true }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("+ 新建指标", color = Green, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }

    if (showCreate) {
        MetricFormDialog(
            title = "新建指标",
            onDismiss = { showCreate = false },
            onConfirm = { name, unit, higher ->
                vm.add(name, unit, higher) {}
                showCreate = false
            },
        )
    }
}

@Composable
private fun MetricRow(item: MetricListItem, onClick: () -> Unit) {
    val latest = item.latest
    val previous = item.previous
    val delta = if (latest != null && previous != null) latest.value - previous.value else null
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.metric.name, color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                val best = item.best?.let { "最佳 ${formatValue(it.value)}${item.metric.unit}" } ?: "还没有测试"
                val last = latest?.let { "上次 ${it.date.toZhDate()}" } ?: ""
                Text(listOf(best, last).filter { it.isNotBlank() }.joinToString(" · "), color = TextMuted, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = latest?.let { "${formatValue(it.value)}${item.metric.unit}" } ?: "—",
                    color = TextMain,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (delta != null) {
                    Spacer(Modifier.height(4.dp))
                    StatusPill("较上次 ${formatSigned(delta)}", PillGreen, PillGreenText)
                } else if (latest != null) {
                    Spacer(Modifier.height(4.dp))
                    StatusPill("较上次 新测", PillGreen, PillGreenText)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricDetailScreen(
    vm: MetricDetailViewModel,
    onBack: () -> Unit,
) {
    val metric by vm.metric.collectAsStateWithLifecycle()
    val tests by vm.tests.collectAsStateWithLifecycle()
    var range by remember { mutableStateOf("全部") }
    var showTest by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    val m = metric ?: return

    val chronological = tests.sortedBy { it.date }
    val cutoff = when (range) {
        "1月" -> LocalDate.now().minusMonths(1)
        "3月" -> LocalDate.now().minusMonths(3)
        else -> null
    }
    val filtered = if (cutoff == null) chronological else chronological.filter { it.date >= cutoff }
    val latest = chronological.lastOrNull()
    val first = chronological.firstOrNull()
    val best = if (m.higherIsBetter) chronological.maxByOrNull { it.value } else chronological.minByOrNull { it.value }
    val vsFirst = if (latest != null && first != null && first.value != 0.0) {
        (latest.value - first.value) / abs(first.value) * 100.0
    } else null

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPad)
            .padding(top = 16.dp, bottom = 28.dp),
    ) {
        ScreenHeader(m.name, "${m.unit} · ${if (m.higherIsBetter) "越大越好" else "越小越好"}", onBack = onBack)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { showEdit = true }) { Text("编辑") }
            TextButton(onClick = { showDelete = true }) { Text("删除") }
        }

        AppCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatCell(latest?.let { formatValue(it.value) } ?: "—", "最近")
                StatCell(best?.let { formatValue(it.value) } ?: "—", "个人最佳 🏆", Gold)
                StatCell(vsFirst?.let { "${formatSigned(it, 0)}%" } ?: "—", "较首次", Green)
            }
        }

        Spacer(Modifier.height(12.dp))
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("进步轨迹", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Segmented(
                options = listOf("1月", "3月", "全部"),
                selected = range,
                onSelect = { range = it },
            )
            Spacer(Modifier.height(12.dp))
            LineChart(
                values = filtered.map { it.value.toFloat() },
                labels = if (filtered.isNotEmpty()) {
                    listOf(filtered.first().date.toZhDate(), filtered.last().date.toZhDate())
                } else emptyList(),
                color = ChartGreen,
                highlightLast = true,
            )
        }

        Spacer(Modifier.height(12.dp))
        AppCard {
            Text("测试记录", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            if (tests.isEmpty()) EmptyHint("还没有测试记录")
            tests.forEach { t ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(t.date.toZhDate(), color = TextMain, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    val isBest = best?.id == t.id
                    Text(
                        "${formatValue(t.value)}${m.unit}${if (isBest) " 🏆" else ""}",
                        color = TextMain,
                        fontSize = 15.sp,
                    )
                    TextButton(onClick = { vm.deleteTest(t.id) }) { Text("删除") }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        PrimaryButton("+ 记录一次测试") { showTest = true }
    }

    if (showTest) {
        AddTestDialog(
            unit = m.unit,
            onDismiss = { showTest = false },
            onConfirm = { date, value ->
                vm.addTest(date, value)
                showTest = false
            },
        )
    }
    if (showEdit) {
        MetricFormDialog(
            title = "编辑指标",
            initialName = m.name,
            initialUnit = m.unit,
            initialHigher = m.higherIsBetter,
            onDismiss = { showEdit = false },
            onConfirm = { name, unit, higher ->
                vm.updateMetric(name, unit, higher)
                showEdit = false
            },
        )
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("删除指标") },
            text = { Text("将同时删除该指标的全部测试记录。") },
            confirmButton = { TextButton(onClick = { vm.deleteMetric(onBack) }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun StatCell(value: String, label: String, color: androidx.compose.ui.graphics.Color = TextMain) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(label, color = TextMuted, fontSize = 12.sp)
    }
}

@Composable
private fun MetricFormDialog(
    title: String,
    initialName: String = "",
    initialUnit: String = "",
    initialHigher: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Boolean) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var unit by remember { mutableStateOf(initialUnit) }
    var higher by remember { mutableStateOf(initialHigher) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("名称") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(unit, { unit = it }, label = { Text("单位，如 秒 / 个") }, singleLine = true)
                Spacer(Modifier.height(12.dp))
                Segmented(
                    options = listOf("越大越好", "越小越好"),
                    selected = if (higher) "越大越好" else "越小越好",
                    onSelect = { higher = it == "越大越好" },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && unit.isNotBlank()) onConfirm(name.trim(), unit.trim(), higher)
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTestDialog(
    unit: String,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, Double) -> Unit,
) {
    var valueText by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var showDate by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录一次测试") },
        text = {
            Column {
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    label = { Text("数值（$unit）") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { showDate = true }) { Text("日期：${date.toZhDate()}") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                valueText.toDoubleOrNull()?.let { onConfirm(date, it) }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
    if (showDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDate = false
                }) { Text("确定") }
            },
        ) { DatePicker(state = state) }
    }
}

private fun formatValue(value: Double): String {
    return if (value == value.toLong().toDouble()) value.toLong().toString()
    else String.format(java.util.Locale.CHINA, "%.1f", value)
}

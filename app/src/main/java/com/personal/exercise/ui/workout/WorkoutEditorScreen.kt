package com.personal.exercise.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.exercise.domain.Category
import com.personal.exercise.domain.Intensity
import com.personal.exercise.domain.Presets
import com.personal.exercise.domain.Workout
import com.personal.exercise.domain.toZhDateWeek
import com.personal.exercise.ui.WorkoutEditorViewModel
import com.personal.exercise.ui.components.AppCard
import com.personal.exercise.ui.components.ChoiceChipRow
import com.personal.exercise.ui.components.DurationStepper
import com.personal.exercise.ui.components.PrimaryButton
import com.personal.exercise.ui.components.ScreenHeader
import com.personal.exercise.ui.components.Segmented
import com.personal.exercise.ui.theme.ChipBg
import com.personal.exercise.ui.theme.Green
import com.personal.exercise.ui.theme.GreenSoft
import com.personal.exercise.ui.theme.Line
import com.personal.exercise.ui.theme.ScreenPad
import com.personal.exercise.ui.theme.TextMain
import com.personal.exercise.ui.theme.TextMuted
import com.personal.exercise.ui.theme.Track
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutEditorScreen(
    vm: WorkoutEditorViewModel,
    initialDate: LocalDate? = null,
    onDone: () -> Unit,
) {
    val activities by vm.activities.collectAsStateWithLifecycle()
    val existing by vm.existing.collectAsStateWithLifecycle()

    var category by remember { mutableStateOf(Category.AEROBIC) }
    var activity by remember { mutableStateOf<String?>(null) }
    var minutes by remember { mutableIntStateOf(30) }
    var intensity by remember { mutableStateOf<Intensity?>(Intensity.MID) }
    var date by remember { mutableStateOf(initialDate ?: LocalDate.now()) }
    var note by remember { mutableStateOf("") }
    var isFree by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var showCustom by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var showDelete by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }
    var hydrated by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        val w = existing ?: return@LaunchedEffect
        if (hydrated) return@LaunchedEffect
        category = w.category
        activity = w.activityName
        minutes = w.durationMinutes
        intensity = w.intensity ?: if (w.category == Category.AEROBIC) Intensity.MID else null
        date = w.date
        note = w.note.orEmpty()
        isFree = w.isFree
        hydrated = true
    }

    val names = activities.filter { it.category == category && !it.hidden }.map { it.name }
    val canSave = !activity.isNullOrBlank() && minutes > 0 &&
        (category != Category.AEROBIC || intensity != null)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPad)
            .padding(top = 16.dp, bottom = 28.dp),
    ) {
        ScreenHeader(
            title = if (existing == null) "记录运动" else "编辑记录",
            subtitle = date.toZhDateWeek(),
            onBack = onDone,
        )
        Spacer(Modifier.height(16.dp))

        AppCard {
            Text("类别", color = TextMuted, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Segmented(
                options = Category.entries.map { it.label },
                selected = category.label,
                onSelect = { label ->
                    val next = Category.entries.first { it.label == label }
                    if (next != category) {
                        category = next
                        activity = null
                        minutes = Presets.durationChips[next]?.getOrNull(2) ?: 30
                        intensity = if (next == Category.AEROBIC) Intensity.MID else null
                    }
                },
            )
        }

        Spacer(Modifier.height(12.dp))
        AppCard {
            Text("项目", color = TextMuted, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            ChoiceChipRow(
                options = names + "+自定义",
                selected = activity,
                onSelect = { chosen ->
                    if (chosen == "+自定义") showCustom = true else activity = chosen
                },
            )
        }

        Spacer(Modifier.height(12.dp))
        AppCard {
            Text("时长", color = TextMuted, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            DurationStepper(
                value = minutes,
                onChange = { minutes = it },
                chips = Presets.durationChips[category] ?: emptyList(),
            )
        }

        if (category == Category.AEROBIC) {
            Spacer(Modifier.height(12.dp))
            AppCard {
                Text("强度（说话测试）", color = TextMuted, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Intensity.entries.forEach { item ->
                        val on = intensity == item
                        val shape = RoundedCornerShape(14.dp)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(shape)
                                .border(1.dp, if (on) Green else Line, shape)
                                .background(if (on) GreenSoft else ChipBg)
                                .clickable { intensity = item }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                item.label,
                                color = if (on) Green else TextMain,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(item.talkTest, color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        AppCard {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("免费", color = TextMain, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Switch(
                    checked = isFree,
                    onCheckedChange = { isFree = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Green,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Track,
                        uncheckedBorderColor = Line,
                    ),
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { showDate = true },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("日期", color = TextMain, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text("${date.toZhDateWeek()} ›", color = TextMuted, fontSize = 15.sp)
            }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { showNote = true },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("备注", color = TextMain, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text(if (note.isBlank()) "可选 ›" else "$note ›", color = TextMuted, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton("保存", enabled = canSave) {
            vm.save(
                Workout(
                    id = existing?.id ?: 0,
                    category = category,
                    activityName = activity.orEmpty(),
                    durationMinutes = minutes,
                    intensity = if (category == Category.AEROBIC) intensity else null,
                    date = date,
                    note = note.ifBlank { null },
                    isFree = isFree,
                ),
                onDone = onDone,
            )
        }
        if (existing != null) {
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("删除这条记录")
            }
        }
    }

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
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("取消") } },
        ) { DatePicker(state = state) }
    }

    if (showCustom) {
        AlertDialog(
            onDismissRequest = { showCustom = false },
            title = { Text("自定义项目") },
            text = {
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    singleLine = true,
                    label = { Text("名称") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = customName.trim()
                    if (name.isNotEmpty()) {
                        vm.addCustom(category, name) {
                            activity = name
                            customName = ""
                            showCustom = false
                        }
                    }
                }) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showCustom = false }) { Text("取消") } },
        )
    }

    if (showNote) {
        AlertDialog(
            onDismissRequest = { showNote = false },
            title = { Text("备注") },
            text = {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                )
            },
            confirmButton = { TextButton(onClick = { showNote = false }) { Text("确定") } },
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("删除记录") },
            text = { Text("删除后无法恢复。") },
            confirmButton = { TextButton(onClick = { vm.delete(onDone) }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("取消") } },
        )
    }
}

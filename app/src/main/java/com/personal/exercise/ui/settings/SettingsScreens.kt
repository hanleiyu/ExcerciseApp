package com.personal.exercise.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.exercise.domain.Category
import com.personal.exercise.domain.Stage
import com.personal.exercise.domain.TargetMode
import com.personal.exercise.ui.SettingsViewModel
import com.personal.exercise.ui.components.AppCard
import com.personal.exercise.ui.components.ScreenHeader
import com.personal.exercise.ui.components.Segmented
import com.personal.exercise.ui.components.SettingsRow
import com.personal.exercise.ui.theme.CardBg
import com.personal.exercise.ui.theme.ChipBg
import com.personal.exercise.ui.theme.Green
import com.personal.exercise.ui.theme.ScreenPad
import com.personal.exercise.ui.theme.StrengthBlue
import com.personal.exercise.ui.theme.TextMain
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onManageActivities: () -> Unit,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    var showRestore by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }
    var edit by remember { mutableStateOf<NumberEdit?>(null) }
    val targets = settings.stageTargets

    val createCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            writeText(context, uri, vm.exportCsv())
            snack.showSnackbar("CSV 已导出")
        }
    }
    val createJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            writeText(context, uri, vm.exportJson())
            snack.showSnackbar("备份已导出")
        }
    }
    val openJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingRestore = uri
        if (uri != null) showRestore = true
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPad)
                .padding(top = 20.dp, bottom = 100.dp),
        ) {
            ScreenHeader("设置")
            Spacer(Modifier.height(16.dp))
            Segmented(
                options = listOf("渐进计划", "固定目标"),
                selected = if (settings.targetMode == TargetMode.PLAN) "渐进计划" else "固定目标",
                onSelect = { vm.setMode(if (it == "渐进计划") TargetMode.PLAN else TargetMode.FIXED) },
            )
            Spacer(Modifier.height(12.dp))

            if (settings.targetMode == TargetMode.PLAN) {
                AppCard(border = Green) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("渐进计划目标", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(currentStageLabel(settings.plan.stage), color = Green, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    StageTargetRow(
                        label = "阶段1",
                        aerobic = targets.stage1Aerobic,
                        strength = targets.stage1Strength,
                        onAerobic = {
                            edit = minuteEdit("阶段 1 · 有氧", targets.stage1Aerobic) { vm.setStageAerobic(Stage.STAGE_1, it) }
                        },
                        onStrength = {
                            edit = minuteEdit("阶段 1 · 力量", targets.stage1Strength) { vm.setStageStrength(Stage.STAGE_1, it) }
                        },
                    )
                    StageTargetRow(
                        label = "阶段2",
                        aerobic = targets.stage2Aerobic,
                        strength = targets.stage2Strength,
                        onAerobic = {
                            edit = minuteEdit("阶段 2 · 有氧", targets.stage2Aerobic) { vm.setStageAerobic(Stage.STAGE_2, it) }
                        },
                        onStrength = {
                            edit = minuteEdit("阶段 2 · 力量", targets.stage2Strength) { vm.setStageStrength(Stage.STAGE_2, it) }
                        },
                    )
                    StageTargetRow(
                        label = "阶段3及维持",
                        aerobic = targets.stage3Aerobic,
                        strength = targets.stage3Strength,
                        onAerobic = {
                            edit = minuteEdit("阶段 3 · 有氧", targets.stage3Aerobic) { vm.setStageAerobic(Stage.STAGE_3, it) }
                        },
                        onStrength = {
                            edit = minuteEdit("阶段 3 · 力量", targets.stage3Strength) { vm.setStageStrength(Stage.STAGE_3, it) }
                        },
                    )
                    SettingsRow("升级条件", "连续达标${settings.weeksToAdvance}周") {
                        edit = NumberEdit(
                            title = "升级条件",
                            value = settings.weeksToAdvance,
                            unit = "周",
                            step = 1,
                            min = 1,
                            max = 12,
                            chips = listOf(1, 2, 3, 4, 5),
                            onSave = vm::setWeeksToAdvance,
                        )
                    }
                }
            } else {
                AppCard(border = Green) {
                    Text("固定目标", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    SettingsRow("有氧", "${settings.fixedAerobic}分钟/周") {
                        edit = minuteEdit("固定目标 · 有氧", settings.fixedAerobic, vm::setFixedAerobic)
                    }
                    SettingsRow("力量", "${settings.fixedStrength}分钟/周") {
                        edit = minuteEdit("固定目标 · 力量", settings.fixedStrength, vm::setFixedStrength)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            AppCard {
                SettingsRow("平衡（可选）", "${settings.balanceGoal}次/周") {
                    edit = NumberEdit(
                        title = "平衡（可选）",
                        value = settings.balanceGoal,
                        unit = "次/周",
                        step = 1,
                        min = 0,
                        max = 14,
                        chips = listOf(0, 1, 2, 3, 4),
                        onSave = vm::setBalanceGoal,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            AppCard {
                Text("数据", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                SettingsRow("管理运动项目", "") { onManageActivities() }
                SettingsRow("导出CSV", "") { createCsv.launch("exercise-workouts.csv") }
                SettingsRow("备份 JSON", "") { createJson.launch("exercise-backup.json") }
                SettingsRow("恢复 JSON", "") { openJson.launch(arrayOf("application/json", "text/*")) }
            }
        }
        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    if (showRestore && pendingRestore != null) {
        AlertDialog(
            onDismissRequest = { showRestore = false },
            title = { Text("恢复备份") },
            text = { Text("将覆盖当前全部运动、指标和设置，不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pendingRestore
                    showRestore = false
                    if (uri != null) scope.launch {
                        val text = readText(context, uri)
                        runCatching { vm.restore(text) }
                            .onSuccess { snack.showSnackbar("已恢复备份") }
                            .onFailure { snack.showSnackbar("恢复失败：${it.message}") }
                    }
                }) { Text("恢复") }
            },
            dismissButton = { TextButton(onClick = { showRestore = false }) { Text("取消") } },
        )
    }

    edit?.let { current ->
        TargetNumberSheet(
            edit = current,
            onDismiss = { edit = null },
            onSave = { value ->
                current.onSave(value)
                edit = null
            },
        )
    }
}

@Composable
fun ActivitiesScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit,
) {
    val activities by vm.activities.collectAsStateWithLifecycle()
    var category by remember { mutableStateOf(Category.AEROBIC) }
    var showAdd by remember { mutableStateOf(false) }
    var renameId by remember { mutableStateOf<Long?>(null) }
    var renameText by remember { mutableStateOf("") }
    val visible = activities.filter { it.category == category }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPad)
            .padding(top = 16.dp, bottom = 28.dp),
    ) {
        ScreenHeader("管理运动项目", onBack = onBack)
        Spacer(Modifier.height(16.dp))
        AppCard {
            Segmented(
                options = Category.entries.map { it.label },
                selected = category.label,
                onSelect = { label -> category = Category.entries.first { it.label == label } },
            )
        }
        Spacer(Modifier.height(12.dp))
        AppCard {
            visible.forEach { item ->
                SettingsRow(
                    label = buildString {
                        append(item.name)
                        if (item.isPreset) append("  · 预设")
                        if (item.hidden) append("  · 已隐藏")
                    },
                    value = if (item.isPreset) "" else if (item.hidden) "显示 ›" else "隐藏 ›",
                    onClick = if (item.isPreset) null else {
                        { vm.hide(item.id, !item.hidden) }
                    },
                )
                if (!item.isPreset) {
                    TextButton(onClick = {
                        renameId = item.id
                        renameText = item.name
                    }) { Text("改名") }
                }
            }
            TextButton(onClick = { showAdd = true }) { Text("+ 新增自定义") }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("新增 ${category.label} 项目") },
            text = { OutlinedTextField(name, { name = it }, label = { Text("名称") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        vm.addActivity(category, name)
                        showAdd = false
                    }
                }) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("取消") } },
        )
    }
    if (renameId != null) {
        AlertDialog(
            onDismissRequest = { renameId = null },
            title = { Text("改名") },
            text = { OutlinedTextField(renameText, { renameText = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    val id = renameId
                    if (id != null && renameText.isNotBlank()) vm.rename(id, renameText)
                    renameId = null
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { renameId = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun StageTargetRow(
    label: String,
    aerobic: Int,
    strength: Int,
    onAerobic: () -> Unit,
    onStrength: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TextMain, fontSize = 15.sp, modifier = Modifier.weight(1f))
        TargetPill("有氧", aerobic, Green, onAerobic)
        Spacer(Modifier.width(8.dp))
        TargetPill("力量", strength, StrengthBlue, onStrength)
    }
}

@Composable
private fun TargetPill(kind: String, value: Int, color: Color, onClick: () -> Unit) {
    Text(
        "$kind $value",
        color = TextMain,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .border(1.dp, color, RoundedCornerShape(99.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetNumberSheet(
    edit: NumberEdit,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var value by remember(edit) { mutableIntStateOf(edit.value) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardBg,
        contentColor = TextMain,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(edit.title, color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SheetStepButton(plus = false) { value = max(edit.min, value - edit.step) }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 28.dp),
                ) {
                    Text("$value", color = TextMain, fontSize = 44.sp, fontWeight = FontWeight.SemiBold)
                    Text(edit.unit, color = TextMain, fontSize = 14.sp)
                }
                SheetStepButton(plus = true) { value = min(edit.max, value + edit.step) }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                edit.chips.forEach { chip ->
                    val on = chip == value
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (on) Green else ChipBg)
                            .clickable { value = chip },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            chip.toString(),
                            color = if (on) Color.Black else TextMain,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(ChipBg)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("取消", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Green)
                        .clickable { onSave(value) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("保存", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SheetStepButton(plus: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(ChipBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (plus) Icons.Outlined.Add else Icons.Outlined.Remove,
            contentDescription = null,
            tint = if (plus) Green else TextMain,
            modifier = Modifier.size(26.dp),
        )
    }
}

private data class NumberEdit(
    val title: String,
    val value: Int,
    val unit: String,
    val step: Int,
    val min: Int,
    val max: Int,
    val chips: List<Int>,
    val onSave: (Int) -> Unit,
)

private fun minuteEdit(title: String, value: Int, onSave: (Int) -> Unit) = NumberEdit(
    title = title,
    value = value,
    unit = "分钟/周",
    step = 5,
    min = 5,
    max = 600,
    chips = minuteChips(value),
    onSave = onSave,
)

private fun minuteChips(current: Int): List<Int> {
    val base = ((current + 5) / 10) * 10
    return listOf(base - 20, base - 10, base, base + 10, base + 20)
        .map { it.coerceIn(20, 200) }
        .distinct()
}

private fun currentStageLabel(stage: Stage): String = when (stage) {
    Stage.STAGE_1 -> "当前阶段1"
    Stage.STAGE_2 -> "当前阶段2"
    Stage.STAGE_3 -> "当前阶段3"
    Stage.MAINTAIN -> "当前维持期"
}

private fun writeText(context: android.content.Context, uri: Uri, text: String) {
    context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
}

private fun readText(context: android.content.Context, uri: Uri): String {
    return context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText().orEmpty()
}

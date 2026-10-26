package com.personal.exercise.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.exercise.domain.TargetMode
import com.personal.exercise.domain.toZhDate
import com.personal.exercise.ui.HomeViewModel
import com.personal.exercise.ui.components.AppCard
import com.personal.exercise.ui.components.LineChart
import com.personal.exercise.ui.components.ProgressBar
import com.personal.exercise.ui.components.ScreenHeader
import com.personal.exercise.ui.theme.ChartBlue
import com.personal.exercise.ui.theme.Green
import com.personal.exercise.ui.theme.ScreenPad
import com.personal.exercise.ui.theme.StrengthBlue
import com.personal.exercise.ui.theme.TextMain
import com.personal.exercise.ui.theme.TextMuted

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onOpenPlan: () -> Unit,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPad)
            .padding(top = 20.dp, bottom = 100.dp),
    ) {
        ScreenHeader("本周进度", ui.stageLine)
        Spacer(Modifier.height(16.dp))

        AppCard {
            ProgressBlock(
                title = if (ui.mode == TargetMode.PLAN) "有氧阶段目标" else "有氧目标",
                current = ui.aerobic,
                target = ui.aerobicTarget,
                unit = "分钟",
                color = Green,
            )
            Spacer(Modifier.height(18.dp))
            ProgressBlock(
                title = "力量",
                current = ui.strength,
                target = ui.strengthTarget,
                unit = "分钟",
                color = StrengthBlue,
            )
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("平衡（可选）", color = TextMain, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text(
                    if (ui.balanceGoal > 0) "${ui.balance}/${ui.balanceGoal}次" else "${ui.balance}次",
                    color = TextMain,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        AppCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("本周总运动时长", color = TextMain, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text("${ui.rawTotal}分钟", color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "有氧 ${ui.rawAerobic} · 力量 ${ui.rawStrength} · 平衡 ${ui.rawBalance}",
                color = TextMuted,
                fontSize = 13.sp,
            )
        }

        Spacer(Modifier.height(12.dp))
        AppCard {
            Text("每周总时长趋势（近8周）", color = TextMain, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            val labels = if (ui.trend.isNotEmpty()) {
                listOf(ui.trend.first().weekStart.toZhDate(), "本周")
            } else emptyList()
            LineChart(
                values = ui.trend.map { it.rawTotal.toFloat() },
                labels = labels,
                color = ChartBlue,
            )
        }

        Spacer(Modifier.height(12.dp))
        AppCard(onClick = onOpenPlan) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ui.planEntry, color = TextMain, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.ChevronRight, null, tint = TextMuted)
            }
        }
    }

    ui.advanceMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = vm::dismissAdvance,
            confirmButton = { TextButton(onClick = vm::dismissAdvance) { Text("好的") } },
            title = { Text("阶段升级") },
            text = { Text(msg) },
        )
    }
}

@Composable
private fun ProgressBlock(
    title: String,
    current: Int,
    target: Int,
    unit: String,
    color: Color,
) {
    val remain = (target - current).coerceAtLeast(0)
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = TextMain, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text("$current/$target$unit", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(10.dp))
        ProgressBar(if (target == 0) 0f else current.toFloat() / target, color)
        if (remain > 0) {
            Spacer(Modifier.height(8.dp))
            Text("还差 ${remain}$unit", color = TextMuted, fontSize = 13.sp)
        }
    }
}

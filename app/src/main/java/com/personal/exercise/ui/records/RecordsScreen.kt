package com.personal.exercise.ui.records

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.exercise.domain.Category
import com.personal.exercise.domain.WeekCalculator
import com.personal.exercise.domain.Workout
import com.personal.exercise.domain.formatYuan
import com.personal.exercise.domain.toZhDateWeek
import com.personal.exercise.ui.RecordsViewModel
import com.personal.exercise.ui.components.AppCard
import com.personal.exercise.ui.components.ScreenHeader
import com.personal.exercise.ui.theme.BalanceOrange
import com.personal.exercise.ui.theme.CardBg
import com.personal.exercise.ui.theme.Danger
import com.personal.exercise.ui.theme.Green
import com.personal.exercise.ui.theme.Line
import com.personal.exercise.ui.theme.ScreenPad
import com.personal.exercise.ui.theme.StrengthBlue
import com.personal.exercise.ui.theme.TextHint
import com.personal.exercise.ui.theme.TextMain
import com.personal.exercise.ui.theme.TextMuted
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun RecordsScreen(
    vm: RecordsViewModel,
    onOpen: (Long) -> Unit,
) {
    val workouts by vm.workouts.collectAsStateWithLifecycle()
    val month by vm.month.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val monthStats = WeekCalculator.statsForMonth(workouts, month)
    val monthEarn = WeekCalculator.monthEarnYuan(workouts, month)
    val totalEarn = WeekCalculator.totalEarnYuan(workouts)
    val dayWorkouts = workouts.filter { it.date == selected }
    val dayTotal = dayWorkouts.filter { it.category.countsTowardStats }.sumOf { it.rawMinutes }
    val dotsByDay = workouts
        .filter { YearMonth.from(it.date) == month }
        .groupBy { it.date }
        .mapValues { (_, list) -> list.map { it.category }.toSet() }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = ScreenPad),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("本月记录") }
        item {
            AppCard {
                MonthHeader(month, onPrev = vm::prevMonth, onNext = vm::nextMonth)
                Spacer(Modifier.height(12.dp))
                WeekdayRow()
                Spacer(Modifier.height(6.dp))
                MonthGrid(
                    month = month,
                    selected = selected,
                    dotsByDay = dotsByDay,
                    onSelect = vm::select,
                )
            }
        }
        item {
            AppCard {
                Text("本月", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MonthStat("有氧", "${monthStats.rawAerobic}分钟")
                    MonthStat("力量", "${monthStats.rawStrength}分钟")
                    MonthStat("平衡", "${monthStats.balanceSessions}次")
                }
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Line),
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("本月净赚", color = TextMain, fontSize = 14.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            formatYuan(monthEarn),
                            color = Green,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("累计", color = TextMain, fontSize = 14.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            formatYuan(totalEarn),
                            color = TextMain,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(selected.toZhDateWeek(), color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("共${dayTotal}分钟", color = TextMain, fontSize = 14.sp)
            }
        }
        if (dayWorkouts.isEmpty()) {
            item {
                AppCard {
                    Text("没有记录", color = TextMain, fontSize = 15.sp)
                }
            }
        } else {
            items(dayWorkouts, key = { it.id }) { w ->
                DayWorkoutRow(w) { onOpen(w.id) }
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("‹", color = TextMain, fontSize = 22.sp, modifier = Modifier.clickable(onClick = onPrev).padding(8.dp))
        Text(
            "${month.year}年${month.monthValue}月",
            color = TextMain,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Text("›", color = TextMain, fontSize = 22.sp, modifier = Modifier.clickable(onClick = onNext).padding(8.dp))
    }
}

@Composable
private fun WeekdayRow() {
    Row(Modifier.fillMaxWidth()) {
        listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
            Text(
                label,
                color = TextHint,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    dotsByDay: Map<LocalDate, Set<Category>>,
    onSelect: (LocalDate) -> Unit,
) {
    val first = month.atDay(1)
    val shift = (first.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val start = first.minusDays(shift.toLong())
    val rows = WeekCalculator.weekRowsForMonth(month)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(rows) { week ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { dow ->
                    val date = start.plusDays((week * 7 + dow).toLong())
                    Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        if (YearMonth.from(date) == month) {
                            DayCell(
                                date = date,
                                selected = selected,
                                dots = dotsByDay[date].orEmpty(),
                                onSelect = onSelect,
                            )
                        } else {
                            Spacer(Modifier.height(48.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    selected: LocalDate,
    dots: Set<Category>,
    onSelect: (LocalDate) -> Unit,
) {
    val selectedDay = date == selected
    val period = Category.PERIOD in dots
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(date) }
            .padding(vertical = 2.dp),
    ) {
        Box(
            Modifier.size(34.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (period) {
                Box(
                    Modifier
                        .size(34.dp)
                        .border(1.5.dp, Danger, CircleShape),
                )
            }
            if (selectedDay) {
                Box(
                    Modifier
                        .size(if (period) 26.dp else 34.dp)
                        .border(1.5.dp, Green, CircleShape),
                )
            }
            Text(
                date.dayOfMonth.toString(),
                color = TextMain,
                fontSize = 15.sp,
                fontWeight = if (selectedDay) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            Category.entries.forEach { cat ->
                if (cat.showsCalendarDot && cat in dots) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(cat.dotColor()))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun MonthStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(label, color = TextMain, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DayWorkoutRow(w: Workout, onClick: () -> Unit) {
    val accent = w.category.dotColor()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(accent),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(w.activityName, color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(w.category.label, color = TextMuted, fontSize = 13.sp)
        }
        Text(w.listDurationLabel, color = TextMain, fontSize = 16.sp)
    }
}

private fun Category.dotColor(): Color = when (this) {
    Category.AEROBIC -> Green
    Category.STRENGTH -> StrengthBlue
    Category.BALANCE -> BalanceOrange
    Category.PERIOD -> Danger
}

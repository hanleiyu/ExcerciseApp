package com.personal.exercise.domain

import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class Category(val label: String) {
    AEROBIC("有氧"),
    STRENGTH("力量"),
    BALANCE("平衡"),
    PERIOD("经期");

    /** 有氧 / 力量 / 平衡计入时长与次数；经期只做日历标注。 */
    val countsTowardStats: Boolean get() = this != PERIOD

    /** 月历日期下彩点只给运动分类，经期靠红圈。 */
    val showsCalendarDot: Boolean get() = this != PERIOD
}

enum class Intensity(
    val label: String,
    val talkTest: String,
    val coefficient: Int,
) {
    LOW("低", "能唱歌", 0),
    MID("中", "能说话不能唱", 1),
    HIGH("高", "只能说几个字", 2);
}

enum class TargetMode { PLAN, FIXED }

enum class Stage(val label: String) {
    STAGE_1("阶段 1"),
    STAGE_2("阶段 2"),
    STAGE_3("阶段 3"),
    MAINTAIN("维持期");

    fun nextOrNull(): Stage? = when (this) {
        STAGE_1 -> STAGE_2
        STAGE_2 -> STAGE_3
        STAGE_3 -> MAINTAIN
        MAINTAIN -> null
    }

    fun compactLabel(): String = when (this) {
        STAGE_1 -> "阶段1"
        STAGE_2 -> "阶段2"
        STAGE_3 -> "阶段3"
        MAINTAIN -> "维持期"
    }
}

data class StageTargets(
    val stage1Aerobic: Int = 60,
    val stage1Strength: Int = 40,
    val stage2Aerobic: Int = 80,
    val stage2Strength: Int = 50,
    val stage3Aerobic: Int = 100,
    val stage3Strength: Int = 60,
) {
    fun aerobic(stage: Stage): Int = when (stage) {
        Stage.STAGE_1 -> stage1Aerobic
        Stage.STAGE_2 -> stage2Aerobic
        Stage.STAGE_3, Stage.MAINTAIN -> stage3Aerobic
    }

    fun strength(stage: Stage): Int = when (stage) {
        Stage.STAGE_1 -> stage1Strength
        Stage.STAGE_2 -> stage2Strength
        Stage.STAGE_3, Stage.MAINTAIN -> stage3Strength
    }

    fun withAerobic(stage: Stage, value: Int): StageTargets = when (stage) {
        Stage.STAGE_1 -> copy(stage1Aerobic = value)
        Stage.STAGE_2 -> copy(stage2Aerobic = value)
        Stage.STAGE_3, Stage.MAINTAIN -> copy(stage3Aerobic = value)
    }

    fun withStrength(stage: Stage, value: Int): StageTargets = when (stage) {
        Stage.STAGE_1 -> copy(stage1Strength = value)
        Stage.STAGE_2 -> copy(stage2Strength = value)
        Stage.STAGE_3, Stage.MAINTAIN -> copy(stage3Strength = value)
    }
}

data class Workout(
    val id: Long = 0,
    val category: Category,
    val activityName: String,
    val durationMinutes: Int,
    val intensity: Intensity?,
    val date: LocalDate,
    val note: String?,
    val isFree: Boolean = false,
) {
    val creditedMinutes: Int
        get() = when (category) {
            Category.AEROBIC -> durationMinutes * (intensity?.coefficient ?: 0)
            Category.STRENGTH -> durationMinutes
            Category.BALANCE, Category.PERIOD -> 0
        }

    /** 当日列表右侧：经期不显示「0分钟」。 */
    val listDurationLabel: String
        get() = if (category == Category.PERIOD) Category.PERIOD.label else "${durationMinutes}分钟"

    val rawMinutes: Int get() = durationMinutes
}

data class ActivityItem(
    val id: Long = 0,
    val category: Category,
    val name: String,
    val isPreset: Boolean,
    val hidden: Boolean,
)

data class Metric(
    val id: Long = 0,
    val name: String,
    val unit: String,
    val higherIsBetter: Boolean,
)

data class MetricTest(
    val id: Long = 0,
    val metricId: Long,
    val date: LocalDate,
    val value: Double,
)

data class MonthStats(
    val year: Int,
    val month: Int,
    val rawAerobic: Int,
    val rawStrength: Int,
    val balanceSessions: Int,
)

data class WeekStats(
    val weekStart: LocalDate,
    val aerobicCredited: Int,
    val strengthMinutes: Int,
    val balanceSessions: Int,
    val rawAerobic: Int,
    val rawStrength: Int,
    val rawBalance: Int,
) {
    val rawTotal: Int get() = rawAerobic + rawStrength + rawBalance

    fun isMet(aerobicTarget: Int, strengthTarget: Int): Boolean =
        aerobicCredited >= aerobicTarget && strengthMinutes >= strengthTarget
}

data class PlanState(
    val stage: Stage = Stage.STAGE_1,
    val consecutiveMetWeeks: Int = 0,
    val lastEvaluatedWeekStart: LocalDate? = null,
    val weeksSpent: Map<Stage, Int> = Stage.entries.associateWith { 0 },
    val pendingAdvanceMessage: String? = null,
)

data class AppSettings(
    val targetMode: TargetMode = TargetMode.PLAN,
    val fixedAerobic: Int = 100,
    val fixedStrength: Int = 60,
    val balanceGoal: Int = 2,
    val stageTargets: StageTargets = StageTargets(),
    val weeksToAdvance: Int = 2,
    val plan: PlanState = PlanState(),
)

fun LocalDate.weekStartMonday(): LocalDate = with(DayOfWeek.MONDAY)

fun LocalDate.toZhDate(): String = "${monthValue}月${dayOfMonth}日"

fun LocalDate.toZhDateWeek(): String {
    val w = when (dayOfWeek) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }
    return "${toZhDate()}$w"
}

fun LocalDate.toIso(): String = format(DateTimeFormatter.ISO_LOCAL_DATE)

fun formatYuan(amount: Int): String =
    "¥" + NumberFormat.getIntegerInstance(Locale.US).format(amount)

fun formatSigned(value: Double, decimals: Int = 0): String {
    val body = if (decimals <= 0) {
        value.toLong().toString()
    } else {
        String.format(Locale.CHINA, "%.${decimals}f", value)
    }
    return if (value > 0) "+$body" else body
}

object Presets {
    val activities: Map<Category, List<String>> = mapOf(
        Category.AEROBIC to listOf("快跑", "慢跑", "骑车", "游泳", "跳绳", "羽毛球", "爬楼梯"),
        Category.STRENGTH to listOf("全身力量", "深蹲", "俯卧撑", "平板支撑", "臀桥", "弓步蹲", "弹力带划船"),
        Category.BALANCE to listOf("瑜伽", "太极", "拉伸", "普拉提"),
        Category.PERIOD to listOf("经期"),
    )

    val durationChips: Map<Category, List<Int>> = mapOf(
        Category.AEROBIC to listOf(15, 20, 30, 45, 60),
        Category.STRENGTH to listOf(15, 20, 25, 30, 40),
        Category.BALANCE to listOf(10, 15, 20, 30, 45),
    )

    val metrics: List<Metric> = listOf(
        Metric(name = "平板支撑", unit = "秒", higherIsBetter = true),
        Metric(name = "深蹲", unit = "个", higherIsBetter = true),
        Metric(name = "俯卧撑", unit = "个", higherIsBetter = true),
    )
}

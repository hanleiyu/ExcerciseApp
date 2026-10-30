package com.personal.exercise.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personal.exercise.data.ExerciseRepository
import com.personal.exercise.domain.ActivityItem
import com.personal.exercise.domain.AppSettings
import com.personal.exercise.domain.Category
import com.personal.exercise.domain.Intensity
import com.personal.exercise.domain.Metric
import com.personal.exercise.domain.MetricTest
import com.personal.exercise.domain.PlanEngine
import com.personal.exercise.domain.Stage
import com.personal.exercise.domain.StageTargets
import com.personal.exercise.domain.TargetMode
import com.personal.exercise.domain.WeekCalculator
import com.personal.exercise.domain.WeekStats
import com.personal.exercise.domain.Workout
import com.personal.exercise.domain.weekStartMonday
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class HomeUi(
    val stageLine: String = "",
    val aerobic: Int = 0,
    val aerobicTarget: Int = 60,
    val strength: Int = 0,
    val strengthTarget: Int = 40,
    val balance: Int = 0,
    val balanceGoal: Int = 2,
    val rawTotal: Int = 0,
    val rawAerobic: Int = 0,
    val rawStrength: Int = 0,
    val rawBalance: Int = 0,
    val trend: List<WeekStats> = emptyList(),
    val planEntry: String = "",
    val upgradeHint: String? = null,
    val advanceMessage: String? = null,
    val mode: TargetMode = TargetMode.PLAN,
)

data class PlanUi(
    val stageLine: String = "",
    val stage: Stage = Stage.STAGE_1,
    val mode: TargetMode = TargetMode.PLAN,
    val weeksSpent: Map<Stage, Int> = emptyMap(),
    val upgradeHint: String? = null,
    val advanceMessage: String? = null,
    val aerobicTarget: Int = 60,
    val strengthTarget: Int = 40,
    val stageTargets: StageTargets = StageTargets(),
    val weeksToAdvance: Int = 2,
)

class HomeViewModel(private val repo: ExerciseRepository) : ViewModel() {
    val ui: StateFlow<HomeUi> = combine(repo.workouts, repo.settings) { workouts, settings ->
        buildHome(workouts, settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUi())

    init {
        viewModelScope.launch { repo.evaluatePlan() }
    }

    fun dismissAdvance() = viewModelScope.launch { repo.clearAdvanceMessage() }

    private fun buildHome(workouts: List<Workout>, settings: AppSettings): HomeUi {
        val today = LocalDate.now()
        val week = WeekCalculator.statsForWeek(workouts, today.weekStartMonday())
        val (aTarget, sTarget) = WeekCalculator.currentTargets(settings)
        val trend = WeekCalculator.weeklySeries(workouts, today.weekStartMonday(), 8)
        val hint = PlanEngine.upgradeHint(
            settings.plan, settings.targetMode, settings.stageTargets, settings.weeksToAdvance,
        )
        return HomeUi(
            stageLine = PlanEngine.stageLine(settings.plan, settings.targetMode),
            aerobic = week.aerobicCredited,
            aerobicTarget = aTarget,
            strength = week.strengthMinutes,
            strengthTarget = sTarget,
            balance = week.balanceSessions,
            balanceGoal = settings.balanceGoal,
            rawTotal = week.rawTotal,
            rawAerobic = week.rawAerobic,
            rawStrength = week.rawStrength,
            rawBalance = week.rawBalance,
            trend = trend,
            planEntry = if (settings.targetMode == TargetMode.PLAN) {
                "计划：本阶段已连续达标 ${settings.plan.consecutiveMetWeeks} 周"
            } else {
                "计划：当前为固定目标"
            },
            upgradeHint = hint,
            advanceMessage = settings.plan.pendingAdvanceMessage,
            mode = settings.targetMode,
        )
    }
}

class PlanViewModel(private val repo: ExerciseRepository) : ViewModel() {
    val ui: StateFlow<PlanUi> = repo.settings.map { settings ->
        val (a, s) = WeekCalculator.currentTargets(settings)
        PlanUi(
            stageLine = PlanEngine.stageLine(settings.plan, settings.targetMode),
            stage = settings.plan.stage,
            mode = settings.targetMode,
            weeksSpent = settings.plan.weeksSpent,
            upgradeHint = PlanEngine.upgradeHint(
                settings.plan, settings.targetMode, settings.stageTargets, settings.weeksToAdvance,
            ),
            advanceMessage = settings.plan.pendingAdvanceMessage,
            aerobicTarget = a,
            strengthTarget = s,
            stageTargets = settings.stageTargets,
            weeksToAdvance = settings.weeksToAdvance,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUi())

    init {
        viewModelScope.launch { repo.evaluatePlan() }
    }

    fun dismissAdvance() = viewModelScope.launch { repo.clearAdvanceMessage() }
}

class RecordsViewModel(repo: ExerciseRepository) : ViewModel() {
    val workouts: StateFlow<List<Workout>> = repo.workouts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
    )

    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    private val _selected = MutableStateFlow(LocalDate.now())
    val selected: StateFlow<LocalDate> = _selected.asStateFlow()

    fun select(date: LocalDate) {
        _selected.value = date
        _month.value = YearMonth.from(date)
    }

    fun prevMonth() = shiftMonth(-1)

    fun nextMonth() = shiftMonth(1)

    private fun shiftMonth(delta: Long) {
        val next = _month.value.plusMonths(delta)
        _month.value = next
        val today = LocalDate.now()
        _selected.value = when {
            YearMonth.from(today) == next -> today
            else -> next.atDay(1)
        }
    }
}

class WorkoutEditorViewModel(
    private val repo: ExerciseRepository,
    private val workoutId: Long?,
) : ViewModel() {
    val activities: StateFlow<List<ActivityItem>> = repo.activities.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
    )
    val settings: StateFlow<AppSettings> = repo.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings(),
    )
    val existing: StateFlow<Workout?> = kotlinx.coroutines.flow.flow {
        emit(workoutId?.let { repo.getWorkout(it) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(workout: Workout, onDone: () -> Unit) = viewModelScope.launch {
        repo.saveWorkout(workout)
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        workoutId?.let { repo.deleteWorkout(it) }
        onDone()
    }

    fun addCustom(category: Category, name: String, onId: (Long) -> Unit) = viewModelScope.launch {
        onId(repo.addCustomActivity(category, name))
    }
}

class SettingsViewModel(private val repo: ExerciseRepository) : ViewModel() {
    val settings: StateFlow<AppSettings> = repo.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings(),
    )
    val activities: StateFlow<List<ActivityItem>> = repo.activities.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
    )

    fun setMode(mode: TargetMode) = viewModelScope.launch { repo.setTargetMode(mode) }
    fun setFixedAerobic(v: Int) = viewModelScope.launch { repo.updateSettings { it.copy(fixedAerobic = v.coerceIn(5, 600)) } }
    fun setFixedStrength(v: Int) = viewModelScope.launch { repo.updateSettings { it.copy(fixedStrength = v.coerceIn(5, 600)) } }
    fun setBalanceGoal(v: Int) = viewModelScope.launch { repo.updateSettings { it.copy(balanceGoal = v.coerceIn(0, 14)) } }
    fun setWeeksToAdvance(v: Int) = viewModelScope.launch {
        repo.updateSettings { it.copy(weeksToAdvance = v.coerceIn(1, 12)) }
    }
    fun setStageAerobic(stage: Stage, v: Int) = viewModelScope.launch {
        repo.updateSettings { it.copy(stageTargets = it.stageTargets.withAerobic(stage, v.coerceIn(5, 600))) }
    }
    fun setStageStrength(stage: Stage, v: Int) = viewModelScope.launch {
        repo.updateSettings { it.copy(stageTargets = it.stageTargets.withStrength(stage, v.coerceIn(5, 600))) }
    }
    fun addActivity(category: Category, name: String) = viewModelScope.launch { repo.addCustomActivity(category, name) }
    fun rename(id: Long, name: String) = viewModelScope.launch { repo.renameActivity(id, name) }
    fun hide(id: Long, hidden: Boolean) = viewModelScope.launch { repo.setActivityHidden(id, hidden) }
    suspend fun exportJson() = repo.exportJson()
    suspend fun restore(text: String) = repo.restoreJson(text)
}

data class MetricListItem(
    val metric: Metric,
    val latest: MetricTest?,
    val previous: MetricTest?,
    val best: MetricTest?,
)

class MetricsViewModel(private val repo: ExerciseRepository) : ViewModel() {
    val items: StateFlow<List<MetricListItem>> = combine(repo.metrics, repo.tests) { metrics, tests ->
        metrics.map { m ->
            val series = tests.filter { it.metricId == m.id }.sortedBy { it.date }
            val latest = series.lastOrNull()
            val previous = series.dropLast(1).lastOrNull()
            val best = if (m.higherIsBetter) series.maxByOrNull { it.value } else series.minByOrNull { it.value }
            MetricListItem(m, latest, previous, best)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(name: String, unit: String, higher: Boolean, onId: (Long) -> Unit) = viewModelScope.launch {
        onId(repo.addMetric(name, unit, higher))
    }

    fun delete(id: Long) = viewModelScope.launch { repo.deleteMetric(id) }
}

class MetricDetailViewModel(
    private val repo: ExerciseRepository,
    private val metricId: Long,
) : ViewModel() {
    val metric: StateFlow<Metric?> = repo.metrics.map { list -> list.firstOrNull { it.id == metricId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val tests: StateFlow<List<MetricTest>> = repo.tests.map { list ->
        list.filter { it.metricId == metricId }.sortedByDescending { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addTest(date: LocalDate, value: Double) = viewModelScope.launch { repo.addTest(metricId, date, value) }
    fun deleteTest(id: Long) = viewModelScope.launch { repo.deleteTest(id) }
    fun updateMetric(name: String, unit: String, higher: Boolean) = viewModelScope.launch {
        repo.updateMetric(Metric(metricId, name, unit, higher))
    }

    fun deleteMetric(onDone: () -> Unit) = viewModelScope.launch {
        repo.deleteMetric(metricId)
        onDone()
    }
}

class VmFactory(
    private val repo: ExerciseRepository,
    private val longArg: Long? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val created: ViewModel = when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(repo)
            modelClass.isAssignableFrom(PlanViewModel::class.java) -> PlanViewModel(repo)
            modelClass.isAssignableFrom(RecordsViewModel::class.java) -> RecordsViewModel(repo)
            modelClass.isAssignableFrom(WorkoutEditorViewModel::class.java) -> WorkoutEditorViewModel(repo, longArg)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(repo)
            modelClass.isAssignableFrom(MetricsViewModel::class.java) -> MetricsViewModel(repo)
            modelClass.isAssignableFrom(MetricDetailViewModel::class.java) -> MetricDetailViewModel(repo, longArg ?: 0)
            else -> error("Unknown VM ${modelClass.name}")
        }
        return created as T
    }
}

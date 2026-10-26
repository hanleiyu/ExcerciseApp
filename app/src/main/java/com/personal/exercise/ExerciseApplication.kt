package com.personal.exercise

import android.app.Application
import com.personal.exercise.data.ExerciseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale

class ExerciseApplication : Application() {
    lateinit var repository: ExerciseRepository
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
        repository = ExerciseRepository.create(this)
        scope.launch {
            repository.seedIfNeeded()
            repository.evaluatePlan()
        }
    }
}

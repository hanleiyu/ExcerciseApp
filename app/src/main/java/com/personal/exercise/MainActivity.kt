package com.personal.exercise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.personal.exercise.ui.ExerciseApp
import com.personal.exercise.ui.theme.ExerciseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as ExerciseApplication).repository
        setContent {
            ExerciseTheme {
                ExerciseApp(repo)
            }
        }
    }
}

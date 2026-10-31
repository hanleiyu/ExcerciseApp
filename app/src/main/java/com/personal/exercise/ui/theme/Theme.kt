package com.personal.exercise.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Green = Color(0xFF00E676)
val GreenDark = Color(0xFF00C853)
val GreenSoft = Color(0xFF0B2A18)
val StrengthBlue = Color(0xFF8B82F6)
val BalanceOrange = Color(0xFFF5A524)
val ChartBlue = Color(0xFF5EB0FF)
val ChartGreen = Color(0xFF2EE59D)
val Gold = Color(0xFFFFC14A)
val GooseYellow = Color(0xFFFFDB58)
val PageBg = Color(0xFF000000)
val CardBg = Color(0xFF141414)
val TextMain = Color(0xFFF4F4F5)
val TextMuted = Color(0xFFB4B4BB)
val TextHint = Color(0xFF8A8A93)
val Line = Color(0xFF2A2A2E)
val Track = Color(0xFF2C2C31)
val ChipBg = Color(0xFF1C1C1F)
val Danger = Color(0xFFFF4D6D)
val PillGreen = Color(0xFF12351F)
val PillGreenText = Color(0xFF4ADE80)
val PillBlue = Color(0xFF1E2248)
val PillBlueText = Color(0xFFB4C0FF)
val PillGray = Color(0xFF222226)
val PillGrayText = Color(0xFFD4D4D8)

val CardRadius = 22.dp
val ScreenPad = 20.dp

val TitleStyle = TextStyle(
    fontSize = 26.sp,
    fontWeight = FontWeight.SemiBold,
    color = TextMain,
    letterSpacing = (-0.4).sp,
)

val SubtitleStyle = TextStyle(
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    color = TextMuted,
)

private val Colors = darkColorScheme(
    primary = Green,
    onPrimary = Color.Black,
    secondary = StrengthBlue,
    onSecondary = Color.Black,
    background = PageBg,
    onBackground = TextMain,
    surface = CardBg,
    onSurface = TextMain,
    surfaceVariant = ChipBg,
    onSurfaceVariant = TextMuted,
    outline = Line,
    error = Danger,
)

@Composable
fun ExerciseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        content = content,
    )
}

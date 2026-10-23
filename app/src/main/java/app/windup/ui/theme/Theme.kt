package app.windup.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.windup.R

/** The workshop's colours: price-tag paper, ink, tin red, brass, the teal of old wallpaper. */
@Immutable
object C {
    val paper = Color(0xFFEFE0BF)
    val paper2 = Color(0xFFE1CB9B)
    val light = Color(0xFFFBF3DC)
    val ink = Color(0xFF1E130C)
    val muted = Color(0xFF7A5A3E)
    val faint = Color(0xFFB09A78)
    val tin = Color(0xFFD24A32)
    val tinDeep = Color(0xFF962C1F)
    val tinHi = Color(0xFFF07A5C)
    val brass = Color(0xFFF0C040)
    val brassDeep = Color(0xFFC08A20)
    val brassHi = Color(0xFFFFF2A8)
    val teal = Color(0xFF3FA098)
    val tealDeep = Color(0xFF26665E)
    val wall = Color(0xFF1D3532)
    val wallText = Color(0xFF9AB8B2)
    val wood = Color(0xFF2B1D14)
    val woodHi = Color(0xFF4A3324)
    val rust = Color(0xFFB8662C)
    val shade = Color(0xB0120D0B)
}

/** Every text in the app: m6x11 by Daniel Linssen with Cyrillic by Natto Games (free with attribution). */
val Pixel = FontFamily(Font(R.font.m6x11, FontWeight.Normal))

object T {
    val Huge = TextStyle(fontFamily = Pixel, fontSize = 64.sp, lineHeight = 60.sp, color = C.ink)
    val Display = TextStyle(fontFamily = Pixel, fontSize = 32.sp, lineHeight = 32.sp, color = C.ink)
    val Title = TextStyle(fontFamily = Pixel, fontSize = 26.sp, lineHeight = 26.sp, color = C.ink)
    val Heading = TextStyle(fontFamily = Pixel, fontSize = 22.sp, lineHeight = 23.sp, color = C.ink)
    val Body = TextStyle(fontFamily = Pixel, fontSize = 20.sp, lineHeight = 22.sp, color = C.ink)
    val Sub = TextStyle(fontFamily = Pixel, fontSize = 17.sp, lineHeight = 18.sp, color = C.muted)
    val Small = TextStyle(fontFamily = Pixel, fontSize = 16.sp, lineHeight = 17.sp, color = C.muted)
    val Button = TextStyle(fontFamily = Pixel, fontSize = 22.sp, lineHeight = 22.sp)
    val Clock = TextStyle(fontFamily = Pixel, fontSize = 32.sp, lineHeight = 30.sp, color = C.brass)
}

@Composable
fun WindupTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.tin, onPrimary = C.light, background = C.paper, surface = C.light, onSurface = C.ink,
            onBackground = C.ink, outline = C.ink, error = C.tin,
        ),
        typography = MaterialTheme.typography.copy(bodyLarge = T.Body, bodyMedium = T.Body, bodySmall = T.Small, labelLarge = T.Button, titleLarge = T.Title),
        content = content,
    )
}

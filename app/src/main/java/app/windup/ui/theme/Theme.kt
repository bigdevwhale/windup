package app.windup.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.windup.R

/**
 * One light of the workshop. By day: old card paper, brown ink, brass. By evening: dark walnut on teal felt, cream
 * ink, the same brass.
 */
class Palette(
    val dark: Boolean,
    /** The page under everything, and the texture drawn over it. */
    val paper: Color, val paperTex: String, val paperTexAlpha: Float,
    /** Cards and panels, and their texture. */
    val surface: Color, val surfaceTex: String, val surfaceTexAlpha: Float,
    /** Tracks, grooves, disabled things. */
    val paper2: Color,
    /** A card that has something for you: a won record, a capsule's gift. */
    val glow: Color,
    val ink: Color, val muted: Color, val faint: Color,
    /** The hairline round cards. */
    val line: Color,
)

val DAY = Palette(
    dark = false,
    paper = Color(0xFFEADCBF), paperTex = "tex_paper", paperTexAlpha = .55f,
    surface = Color(0xFFF6EAD0), surfaceTex = "tex_paper", surfaceTexAlpha = .4f,
    paper2 = Color(0xFFDCC8A2),
    glow = Color(0xFFF6DB92),
    ink = Color(0xFF2A1A0E), muted = Color(0xFF76553A), faint = Color(0xFFA88F6B),
    line = Color(0x66B08A4A),
)

val EVENING = Palette(
    dark = true,
    paper = Color(0xFF14211F), paperTex = "tex_felt", paperTexAlpha = .45f,
    surface = Color(0xFF2A1C13), surfaceTex = "tex_wood", surfaceTexAlpha = .38f,
    paper2 = Color(0xFF3B2B1E),
    glow = Color(0xFF4C3A14),
    ink = Color(0xFFF3E5C8), muted = Color(0xFFC2A882), faint = Color(0xFF7D6B52),
    line = Color(0x55D8AE4E),
)

/** The workshop's colours. Paper, ink and the cards follow the [Palette]; tin, brass and the wall are the same day and night. */
object C {
    @Volatile var p: Palette = DAY

    val dark get() = p.dark
    val paper get() = p.paper
    val paper2 get() = p.paper2
    val surface get() = p.surface
    val glow get() = p.glow
    val ink get() = p.ink
    val muted get() = p.muted
    val faint get() = p.faint
    val line get() = p.line

    /** Cream, for text and icons on tin, teal and the wall. */
    val light = Color(0xFFFFF8E8)
    /** Dark ink, for text on brass. */
    val onBrass = Color(0xFF24160B)
    val tin = Color(0xFFD24A32)
    val tinDeep = Color(0xFF962C1F)
    val tinHi = Color(0xFFF07A5C)
    val brass = Color(0xFFF0C040)
    val brassDeep = Color(0xFFC08A20)
    val brassHi = Color(0xFFFFF2A8)
    val teal = Color(0xFF3FA098)
    val tealDeep get() = if (p.dark) Color(0xFF6CC7BD) else Color(0xFF26665E)
    val wall = Color(0xFF1D3532)
    val wallText = Color(0xFF9AB8B2)
    val wood = Color(0xFF2B1D14)
    val woodHi = Color(0xFF4A3324)
    val rust = Color(0xFFD07A3C)
    val shade = Color(0xB0120D0B)
    val navBg = Color(0xFF162724)
    val navInk = Color(0xFF8FAAA4)
}

/** Every text in the app: Nunito (SIL Open Font License), soft and round like the toys, with Cyrillic. */
val Sans = FontFamily(
    Font(R.font.nunito_500, FontWeight.Normal), Font(R.font.nunito_500, FontWeight.Medium), Font(R.font.nunito_600, FontWeight.SemiBold),
    Font(R.font.nunito_700, FontWeight.Bold), Font(R.font.nunito_800, FontWeight.ExtraBold), Font(R.font.nunito_900, FontWeight.Black),
)

private fun st(size: Int, line: Int, w: FontWeight, c: Color, track: Double = 0.0) =
    TextStyle(fontFamily = Sans, fontSize = size.sp, lineHeight = line.sp, fontWeight = w, color = c, letterSpacing = track.sp)

/** The text styles of one palette. */
class Styles(p: Palette) {
    val Huge = st(60, 62, FontWeight.Black, p.ink).copy(fontFeatureSettings = "tnum")
    val Display = st(24, 29, FontWeight.ExtraBold, p.ink)
    val Title = st(20, 25, FontWeight.ExtraBold, p.ink)
    val Heading = st(17, 22, FontWeight.Bold, p.ink)
    val Body = st(15, 21, FontWeight.Medium, p.ink)
    val Sub = st(14, 19, FontWeight.SemiBold, p.muted)
    val Small = st(13, 18, FontWeight.Medium, p.muted)
    val Button = st(15, 20, FontWeight.ExtraBold, p.ink)
    val Clock = st(26, 30, FontWeight.ExtraBold, C.brass).copy(fontFeatureSettings = "tnum")
    val Label = st(11, 14, FontWeight.ExtraBold, p.muted, track = .8)
}

object T {
    private val day = Styles(DAY)
    private val evening = Styles(EVENING)
    private val s get() = if (C.p.dark) evening else day

    val Huge get() = s.Huge
    val Display get() = s.Display
    val Title get() = s.Title
    val Heading get() = s.Heading
    val Body get() = s.Body
    val Sub get() = s.Sub
    val Small get() = s.Small
    val Button get() = s.Button
    val Clock get() = s.Clock
    val Label get() = s.Label
}

/** Theme choice: follow the phone, always day, always evening. */
object Looks {
    const val SYSTEM = 0
    const val DAY = 1
    const val EVENING = 2
}

@Composable
fun WindupTheme(look: Int = Looks.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (look) { Looks.DAY -> false; Looks.EVENING -> true; else -> isSystemInDarkTheme() }
    C.p = if (dark) EVENING else DAY
    val scheme = if (dark) darkColorScheme(
        primary = C.tin, onPrimary = C.light, background = C.paper, surface = C.surface, onSurface = C.ink,
        onBackground = C.ink, outline = C.line, error = C.tin,
    ) else lightColorScheme(
        primary = C.tin, onPrimary = C.light, background = C.paper, surface = C.surface, onSurface = C.ink,
        onBackground = C.ink, outline = C.line, error = C.tin,
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = MaterialTheme.typography.copy(bodyLarge = T.Body, bodyMedium = T.Body, bodySmall = T.Small, labelLarge = T.Button, titleLarge = T.Title),
    ) {
        // Everything reads the palette directly, so a change of light redraws the whole tree.
        key(dark) { content() }
    }
}

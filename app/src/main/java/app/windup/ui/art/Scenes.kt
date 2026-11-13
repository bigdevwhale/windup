package app.windup.ui.art

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import app.windup.domain.Toy
import kotlin.math.PI
import kotlin.math.sin

/** A pin as the drum draws it: minutes into the waking day, which toy's note, whether it was overwound. */
data class DrumPin(val min: Int, val toy: Toy, val over: Boolean)

/** The backdrops: the shelf by the lamp, the workbench, the music box, the fair stall, the capsule machine. */
object Scenes {
    const val SW = 216
    const val SH = 216
    val PLANK = intArrayOf(66, 138, 210)
    val SLOT = intArrayOf(36, 108, 180)
    const val ROW = 72

    /** The shelf grows a board for every three fair toys. */
    fun shelfH(rows: Int) = SH + ROW * (rows - 3).coerceAtLeast(0)
    fun plank(row: Int) = 66 + ROW * row

    /** The rows a drum needs for every toy's note, top to bottom. */
    private fun noteRow(t: Toy, top: Float, span: Float) = top + t.ordinal * span / (Toy.entries.size - 1f)

    const val EW = 108
    const val EH = 72
    const val ETOP = 60

    const val BW = 216
    const val BH = 152
    const val TABLE = 134
    const val COMB = 62
    private const val CY0 = 13f
    private const val CY1 = 63f
    private const val DRUM_END = BW - 16f

    const val STRIP_W = 216
    const val STRIP_H = 48

    private val WARM = Color(0xFFFFD58A)
    private val DUSK = Color(0xFF0B1A18)
    private val TEAL = Color(0xFF16302C)
    private val INKC = Color(0xFF1E130C)

    /** Wallpaper under a lamp at (lx, ly): the paper, dimmed, with a warm pool of light that fades out at [reach]. */
    fun wall(g: G, x: Float, y: Float, w: Float, h: Float, lx: Float, ly: Float, reach: Float, glow: Float = 1f) {
        g.tiled(Img["scene_wall"], x, y, w, h, 64)
        // The paper is old and the room is dim: a deep teal wash over it, the lamp's pool warming it back up.
        g.rect(x, y, w, h, TEAL.copy(alpha = .72f))
        g.rect(x, y, w, h, DUSK.copy(alpha = .28f))
        g.rect(x, y, w, h, Brush.radialGradient(
            0f to WARM.copy(alpha = .55f * glow), .35f to WARM.copy(alpha = .22f * glow), 1f to WARM.copy(alpha = 0f),
            center = g.o(lx, ly), radius = g.u(reach),
        ))
        g.rect(x, y, w, h, Brush.radialGradient(
            0f to Color.Transparent, .7f to Color.Transparent, 1f to DUSK.copy(alpha = .45f),
            center = g.o(lx, ly), radius = g.u(reach * 1.25f),
        ))
    }

    /** A wooden shelf board, the toys' feet on its top at y, and its shadow on the wall below. */
    fun plank(g: G, y: Number, w: Number) {
        val yy = y.toFloat()
        val img = Img["scene_plank"]
        val ww = w.toFloat() + 6
        val h = ww * img.height / img.width
        // The board is the top two fifths of the picture; the brackets hang below it.
        g.rect(0, yy + h * .3f, w, 8, Brush.verticalGradient(listOf(Color.Black.copy(alpha = .4f), Color.Transparent), g.u(yy + h * .3f), g.u(yy + h * .3f + 8)))
        g.image(img, -3, yy - h * .07f, ww, h)
    }

    fun shelf(g: G, rows: Int) {
        val h = shelfH(rows).toFloat()
        wall(g, 0f, 0f, SW.toFloat(), h, 108f, 14f, 200f)
        for (r in 0 until rows.coerceAtLeast(3)) plank(g, plank(r), SW)
        val lamp = Img["scene_lamp"]
        val lw = 26f
        val lh = lw * lamp.height / lamp.width
        g.image(lamp, 108 - lw / 2, -lh * .42f, lw, lh)
    }

    /** The plate under a toy, in its metal, its bar showing the winding left and a dot per alloy step. */
    fun plate(g: G, cx: Int, by: Int, wind: Float, alloy: Int = 0, today: Boolean = false) {
        val m = Pal.metals[alloy.coerceIn(0, Pal.metals.size - 1)]
        val x = cx - 12f
        val y = by + 1.6f
        g.round(x, y, 24, 6, 2, Brush.verticalGradient(listOf(col(m[0]), col(m[1]), col(m[2])), g.u(y), g.u(y + 6)))
        g.round(x + 2.2f, y + 1.8f, 19.6f, 2.4f, 1.2f, INKC.copy(alpha = .8f))
        if (wind > 0) g.round(x + 2.2f, y + 1.8f, 19.6f * wind.coerceIn(0f, 1f), 2.4f, 1.2f, Brush.verticalGradient(listOf(Color(0xFFFFF0A0), Color(0xFFE8A82A)), g.u(y + 1.8f), g.u(y + 4.2f)))
        for (i in 0 until alloy) g.circle(cx - 2f * alloy + 1 + i * 4, y + 7.6f, .9f, col(m[0]))
        // Wound today: a little teal medal with a tick at the plate's right end.
        if (today) {
            val mx = cx + 13.5f; val my = y + 3f
            g.circle(mx, my + .3f, 3.6f, Color.Black.copy(alpha = .35f))
            g.circle(mx, my, 3.4f, Brush.radialGradient(listOf(Color(0xFF8AD8CC), Color(0xFF3FA098), Color(0xFF26665E)), g.o(mx - 1, my - 1), g.u(4.5f)))
            val p = androidx.compose.ui.graphics.Path().apply {
                moveTo(g.u(mx - 1.6f), g.u(my + .1f)); lineTo(g.u(mx - .4f), g.u(my + 1.3f)); lineTo(g.u(mx + 1.8f), g.u(my - 1.2f))
            }
            g.path(p, Color.White, .75f)
        }
    }

    private val FLAGS = listOf(0xd24a32, 0xf0c040, 0x3fa098, 0xea86a6, 0x4d84c8)

    /** Bunting across the top shelf: every toy was wound today. */
    fun garland(g: G, ms: Long) {
        fun yAt(x: Float) = 9 + sin(PI * (x - 2) / (SW - 4)).toFloat() * 14
        val p = androidx.compose.ui.graphics.Path()
        for (i in 0..54) { val x = 2 + i * (SW - 4) / 54f; if (i == 0) p.moveTo(g.u(x), g.u(yAt(x))) else p.lineTo(g.u(x), g.u(yAt(x))) }
        g.path(p, Color(0xFF3A2414), .5f)
        var x = 8f
        var k = 0
        while (x < SW - 6) {
            val y = yAt(x)
            val sway = sin(ms / 420.0 + k).toFloat() * 1.2f
            val c = col(FLAGS[k % FLAGS.size])
            g.path(g.pathOf(x - 3.6f to y, x + 3.6f to y, x + sway to y + 8), c)
            g.path(g.pathOf(x - 3.6f to y, x to y, x + sway * .5f to y + 4), Color.White.copy(alpha = .18f))
            x += 12; k++
        }
    }

    /** The question mark over the empty place a blueprint toy will take. */
    fun query(g: G, cx: Int, by: Int) = g.text("?", cx, by - 18, 18, Color(0xFF9AB8B2).copy(alpha = .9f))

    /** A thick wooden top, its surface at y. */
    private fun counter(g: G, y: Float, w: Float) {
        val img = Img["scene_counter"]
        val h = (w + 6) * img.height / img.width
        g.image(img, -3, y - h * .1f, w + 6, h)
    }

    fun bench(g: G) {
        wall(g, 0f, 0f, EW.toFloat(), EH.toFloat(), 54f, -6f, 110f)
        g.rect(0, ETOP + 4, EW, EH - ETOP, Color(0xFF4A2C16))
        counter(g, ETOP.toFloat(), EW.toFloat())
    }

    /** The progress of a set, as a brass gauge set in the bench. */
    fun gauge(g: G, p: Float) {
        val x = 10f; val y = ETOP + 6.5f; val w = EW - 20f
        g.round(x - .6f, y - .6f, w + 1.2f, 4.2f, 2.1f, Color(0xFF2A170C).copy(alpha = .85f))
        g.round(x, y, w, 3, 1.5f, Color(0xFF3A2414))
        if (p > 0) g.round(x, y, w * p.coerceIn(0f, 1f), 3, 1.5f, Brush.verticalGradient(listOf(Color(0xFFFFF0A0), Color(0xFFF0C040), Color(0xFFC08A20)), g.u(y), g.u(y + 3)))
    }

    /** The music box on the table: its inlaid wooden case, open, with dark velvet behind the drum and the comb. */
    fun box(g: G) {
        wall(g, 0f, 0f, BW.toFloat(), BH.toFloat(), 108f, 40f, 180f)
        // The box's shadow on the wall, then the case and its velvet.
        g.round(6, 6, BW - 8, 74, 6, Color.Black.copy(alpha = .35f))
        g.rect(10, 9, BW - 20, 64, Brush.verticalGradient(listOf(Color(0xFF2A0E12), Color(0xFF4A1A20), Color(0xFF2A0E12)), g.u(9), g.u(73)))
        g.nine(Img["scene_box_frame"], 3, 2, BW - 6, 78, intArrayOf(94, 90, 95, 89), floatArrayOf(9f, 9f, 9f, 9f))
        plank(g, TABLE, BW)
        g.rect(0, TABLE + 9, BW, BH - TABLE - 9, Brush.verticalGradient(listOf(Color(0xFF4A2C16), Color(0xFF2A170C)), g.u(TABLE + 9), g.u(BH)))
    }

    /** A note rising from a toy as its pin sounds: [t] 0..1 of its flight, in the toy's colour. */
    fun note(g: G, x: Float, y: Float, t: Float, c: Int, seed: Int) {
        val a = (if (t < .15f) t / .15f else 1 - (t - .15f) / .85f).coerceIn(0f, 1f)
        val xx = x + sin(t * 6.0 + seed).toFloat() * 3f
        val yy = y - t * 34f
        val col = col(c, a)
        val s = .8f + .4f * (seed % 3) / 2f
        g.at(xx, yy, deg = -14f, sx = s, sy = s) {
            g.oval(xx, yy, 2.4f, 1.8f, col)
        }
        g.line(xx + 2f * s, yy - .3f, xx + 2f * s, yy - 9 * s, col, .7f)
        if (seed % 2 == 0) g.path(g.pathOf(xx + 2f * s to yy - 9 * s, xx + 5f * s to yy - 6.5f * s, xx + 2f * s to yy - 7.2f * s), col)
        g.circle(xx, yy, 4.5f * s, Color(0xFFFFF0B0).copy(alpha = .12f * a))
    }

    private fun drumBrush(g: G, y0: Float, y1: Float) = Brush.verticalGradient(
        0f to Color(0xFF7A5410), .18f to Color(0xFFFFF2A8), .35f to Color(0xFFF0C040), .75f to Color(0xFFC08A20), 1f to Color(0xFF5A3C0C),
        startY = g.u(y0), endY = g.u(y1),
    )

    private fun pin(g: G, x: Float, y: Float, t: Toy, over: Boolean) {
        g.circle(x, y + .5f, 2.3f, Color.Black.copy(alpha = .35f))
        g.circle(x, y, 2f, if (over) Color(0xFF968F80) else col(Toys[t].color))
        g.circle(x - .6f, y - .6f, .7f, Color.White.copy(alpha = .75f))
    }

    /** Hours between the long ticks and the labels under the drum: whatever divides the day evenly. */
    fun hourStep(len: Int): Int {
        val hours = (len / 60).coerceAtLeast(1)
        return listOf(2, 3, 4).firstOrNull { hours % it == 0 } ?: 2
    }

    /** Where an hour of the day falls across the strip, 0..1 of its width. */
    fun stripX(min: Int, len: Int) = (6 + min.coerceIn(0, len) * (STRIP_W - 12f) / len) / STRIP_W

    /** The day as a flat strip of drum: time across, the toy's note down, and a red line at now. */
    fun strip(g: G, pins: List<DrumPin>, len: Int, now: Int?) {
        val w = STRIP_W.toFloat()
        val h = STRIP_H.toFloat()
        g.round(0, 0, w, h, 6, drumBrush(g, 0f, h))
        val hours = len / 60
        for (k in 0..hours) {
            val x = 6 + k * (w - 12) / hours.coerceAtLeast(1)
            val tall = if (k % hourStep(len) == 0) 4f else 2f
            g.line(x, 1, x, 1 + tall, Color(0xFF7A5410), .5f); g.line(x, h - 1 - tall, x, h - 1, Color(0xFF7A5410), .5f)
        }
        if (now != null) {
            val nx = 6 + now.coerceIn(0, len) * (w - 12) / len
            g.round(nx, 0, w - nx, h, 6, INKC.copy(alpha = .32f))
            g.line(nx, 0, nx, h, Color(0xFFD24A32), .9f)
            g.circle(nx, 1.5f, 1.8f, Color(0xFFD24A32))
        }
        for (p in pins) pin(g, 6 + p.min.coerceIn(0, len) * (w - 12) / len, noteRow(p.toy, 6f, h - 12), p.toy, p.over)
    }

    /**
     * The music box, playing: the drum turns under the comb at [play] minutes into the day; each tooth shakes
     * for a moment after its note ([plucked] holds the time of each toy's last note).
     */
    fun drum(g: G, pins: List<DrumPin>, len: Int, play: Double, plucked: LongArray, nowMs: Long, playing: Boolean = true) {
        val pxm = .6
        val x0 = COMB - 6f; val x1 = DRUM_END
        g.round(x0, CY0, x1 - x0, CY1 - CY0, 3, drumBrush(g, CY0, CY1))
        for (k in 0..len / 60) {
            val x = (COMB + (k * 60 - play) * pxm).toFloat()
            if (x < x0 || x > x1) continue
            g.line(x, CY0 + 1, x, CY0 + 3.5f, Color(0xFF7A5410), .5f); g.line(x, CY1 - 3.5f, x, CY1 - 1, Color(0xFF7A5410), .5f)
        }
        for (p in pins) {
            val x = (COMB + (p.min - play) * pxm).toFloat()
            if (x < x0 + 2 || x > x1 - 2) continue
            pin(g, x, noteRow(p.toy, CY0 + 4, CY1 - CY0 - 8), p.toy, p.over)
        }
        // The cylinder's ends, then the comb's base and its teeth.
        g.rect(x0, CY0, 3, CY1 - CY0, Brush.horizontalGradient(listOf(Color(0x99000000), Color.Transparent), g.u(x0), g.u(x0 + 3)))
        g.rect(x1 - 3, CY0, 3, CY1 - CY0, Brush.horizontalGradient(listOf(Color.Transparent, Color(0x99000000)), g.u(x1 - 3), g.u(x1)))
        g.round(14, CY0, 10, CY1 - CY0, 2, Brush.horizontalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFB8B2A4), Color(0xFF6F6A62)), g.u(14), g.u(24)))
        for (t in Toy.entries) {
            val y = noteRow(t, CY0 + 4, CY1 - CY0 - 8)
            val pl = nowMs - plucked[t.ordinal] < 260
            val v = if (pl) sin(nowMs / 18.0).toFloat() * .8f else 0f
            g.round(22, y - 1f + v * .3f, COMB - 22f + v, 2, 1, Brush.verticalGradient(
                listOf(if (pl) Color.White else Color(0xFFF0EEE8), Color(0xFF8A857C)), g.u(y - 1), g.u(y + 1),
            ))
        }
        g.line(COMB, CY0 - 2, COMB, CY1 + 2, Color(0xFFD24A32).copy(alpha = .9f), .7f)
        if (playing && play > 0) g.round(14, CY1 + 3.4f, (DRUM_END - 14) * (play / len).coerceIn(0.0, 1.0).toFloat(), 1.4f, .7f, Color(0xFFFFD24A))
    }

    // The weekend fair's stall.
    const val FW = 216
    const val FH = 88
    const val COUNTER = 70
    val FAIR_SLOT = intArrayOf(62, 154)

    /** The stall: a striped awning over a wooden counter; on weekdays the awning is rolled up and the light is low. */
    fun stall(g: G, open: Boolean, ms: Long) {
        wall(g, 0f, 0f, FW.toFloat(), FH.toFloat(), 108f, 26f, if (open) 170f else 90f, if (open) 1f else .5f)
        for (x in listOf(10f, FW - 14f)) g.round(x, 6, 4, FH - 10, 1.2f, Brush.horizontalGradient(listOf(Color(0xFFDC9C64), Color(0xFFA2643A), Color(0xFF6E3E20)), g.u(x), g.u(x + 4)))
        if (open) {
            val a = Img["scene_awning"]
            val ah = 26f
            g.image(a, 2, 1, FW - 4, ah)
            for ((i, x) in (20 until FW - 16 step 16).withIndex()) {
                val on = .75f + .25f * sin(ms / 300.0 + i * 1.3).toFloat()
                g.circle(x, 30, 3.5f, Color(0xFFFFE9A0).copy(alpha = .18f * on))
                g.circle(x, 30, 1.4f, Color(0xFFFFF4C8).copy(alpha = on))
            }
        } else {
            val a = Img["scene_awning_rolled"]
            g.image(a, 2, 1, FW - 4, 9)
        }
        counter(g, COUNTER.toFloat(), FW.toFloat())
    }

    /** A paper price tag hanging off the counter. */
    fun tag(g: G, cx: Int, sold: Boolean) {
        val y = COUNTER + 3f
        g.line(cx, y, cx, y + 3.5f, INKC, .5f)
        g.round(cx - 6.5f, y + 3.5f, 13, 8, 1.6f, if (sold) Color(0xFF3FA098) else Color(0xFFF4ECD8))
        val ink = if (sold) Color(0xFF153C38) else Color(0xFF8A7E66)
        g.line(cx - 3.5f, y + 6.5f, cx + 3.5f, y + 6.5f, ink, .7f); g.line(cx - 3.5f, y + 8.8f, cx + 1.5f, y + 8.8f, ink, .7f)
    }

    // The capsule machine.
    const val MW = 48
    const val MH = 64

    /** The machine at a moment: it rattles while the crank turns, then a capsule rolls out into the tray. */
    fun machine(g: G, shake: Double, drop: Int?, dropY: Int, ms: Long) {
        val m = Img["scene_machine"]
        val h = MH - 2f
        val w = h * m.width / m.height
        val rot = (sin(ms / 45.0) * 2.2 * shake).toFloat()
        g.oval(MW / 2f, MH - 1.5f, w * .42f, 1.6f, Color.Black.copy(alpha = .22f))
        g.at(MW / 2f, MH - 1f, deg = rot) { g.image(m, (MW - w) / 2, MH - 1f - h, w, h) }
        if (drop != null) {
            val cap = Img["scene_capsule"]
            val p = ((dropY - 40) / 14f).coerceIn(0f, 1f)
            val s = 5f + 5f * p
            g.image(cap, MW / 2f - s / 2 + drop % 3 - 1, MH * .62f + p * MH * .2f, s, s, filter = CAPS[drop % CAPS.size])
        }
    }

    /** The capsule art is red; the others are the same capsule turned to other colours. */
    private val CAPS = listOf(null, hue(.5f, .9f, .2f), hue(.25f, .65f, .7f), hue(.95f, .6f, .75f), hue(.3f, .5f, 1f), hue(.4f, .85f, .35f))
    private fun hue(r: Float, gg: Float, b: Float) = androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
        .3f * r * 1.6f, .59f * r * 1.6f, .11f * r * 1.6f, 0f, 0f,
        .3f * gg * 1.6f, .59f * gg * 1.6f, .11f * gg * 1.6f, 0f, 0f,
        .3f * b * 1.6f, .59f * b * 1.6f, .11f * b * 1.6f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )))
}

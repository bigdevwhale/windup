package app.windup

import app.windup.domain.Toy
import app.windup.ui.art.Buf
import app.windup.ui.art.DrumPin
import app.windup.ui.art.Scenes
import app.windup.ui.art.St
import app.windup.ui.art.Toys
import app.windup.ui.art.bay
import app.windup.ui.art.rgb
import org.junit.Test
import java.io.File

/** Draws the toys and scenes to PNGs for a look (run with `-Prender`); not a test. */
class RenderArtScenes {
    private val out = File(System.getProperty("render.out") ?: "build/art").apply { mkdirs() }

    /** Ten frames of every toy's cycle, plain and painted, then a rusty row. */
    @Test fun film() {
        val n = 10
        val rows = Toy.entries.size * 2 + 1
        val b = Buf(48 * n, 48 * rows)
        b.r(0, 0, b.w, b.h, rgb(0x1d3532))
        Toy.entries.forEachIndexed { r, t ->
            for (painted in listOf(false, true)) {
                val st = St()
                val row = r * 2 + if (painted) 1 else 0
                for (f in 0 until n) {
                    Toys.drawAt(b, t, Toys.parts(t, painted, 0f), 24 + f * 48, 46 + row * 48, st, 1.0)
                    repeat(10) { Toys.step(Toys[t], st, 1.0 / Toys[t].speed / n / 10, 1.0) }
                }
            }
        }
        Toy.entries.forEachIndexed { i, t -> if (i < n) Toys.drawAt(b, t, Toys.parts(t, false, (i + 1) / 9f), 24 + i * 48, 46 + (rows - 1) * 48, St(), 0.0) }
        Png.write(File(out, "film.png"), b, 3)
    }

    /** Each metal for the frog and the monkey, a trick in mid-air, a jammed bear, and the parade garland. */
    @Test fun metals() {
        val b = Buf(48 * 8, 48 * 3)
        b.r(0, 0, b.w, b.h, rgb(0x1d3532))
        for ((row, t) in listOf(Toy.FROG, Toy.MONKEY).withIndex()) for (a in 0..4) {
            val st = St().apply { ph = 2.3 }
            Toys.drawAt(b, t, Toys.parts(t, false, 0f, a == 4), 24 + a * 48, 46 + row * 48, st, 1.0, alloy = a)
            app.windup.ui.art.Scenes.plate(b, 24 + a * 48, 46 + row * 48, .7f, a)
        }
        for ((i, p) in listOf(5.2, 5.4, 5.6).withIndex()) Toys.drawAt(b, Toy.PENGUIN, Toys.parts(Toy.PENGUIN, false, 0f), 24 + (5 + i) * 48, 46, St().apply { ph = p }, 1.0, alloy = 2)
        Toys.drawAt(b, Toy.BEAR, Toys.parts(Toy.BEAR, false, 1f), 24 + 5 * 48, 46 + 48, St(), 0.0, jam = true)
        Toys.drawAt(b, Toy.SOLDIER, Toys.parts(Toy.SOLDIER, false, 1f), 24 + 6 * 48, 46 + 48, St(), 0.0, jam = true)
        val g = Buf(Scenes.SW, 48); g.r(0, 0, g.w, g.h, rgb(0x1d3532)); Scenes.garland(g, 0)
        for (y in 0 until 44) for (x in 0 until minOf(b.w, g.w)) b.set(x, 2 * 48 + y, g.get(x, y))
        Png.write(File(out, "metals.png"), b, 3)
    }

    /** The stall open and shut, and a shelf grown to five boards with every fair toy on it. */
    @Test fun fair() {
        for (open in listOf(true, false)) {
            val b = Buf(Scenes.FW, Scenes.FH).also { it.copyFrom(Scenes.stall(open)) }
            for ((i, t) in listOf(Toy.DIVER, Toy.MINER).withIndex()) {
                if (open) Toys.drawAt(b, t, Toys.parts(t, false, 0f), Scenes.FAIR_SLOT[i], Scenes.COUNTER, St().apply { ph = .3 }, 1.0)
                else Toys.drawAt(b, t, Toys.parts(t, false, 0f), Scenes.FAIR_SLOT[i], Scenes.COUNTER, St(), 0.0, sil = rgb(0x2a4744), shadow = false)
                Scenes.tag(b, Scenes.FAIR_SLOT[i], i == 1 && open)
            }
            Png.write(File(out, "stall-${if (open) "open" else "shut"}.png"), b, 3)
        }
        val slots = Toy.CORE + Toy.FAIR
        val rows = (slots.size + 2) / 3
        val b = Buf(Scenes.SW, Scenes.shelfH(rows)).also { it.copyFrom(Scenes.shelf(rows)) }
        Scenes.garland(b, 0)
        slots.forEachIndexed { i, t ->
            val cx = Scenes.SLOT[i % 3]; val by = Scenes.plank(i / 3)
            Toys.drawAt(b, t, Toys.parts(t, false, 0f), cx, by, St(), 0.0)
            Scenes.plate(b, cx, by, .5f, i % 5)
        }
        Png.write(File(out, "shelf-full.png"), b, 2)
    }

    @Test fun scenes() {
        val shelf = Buf(Scenes.SW, Scenes.SH).also { it.copyFrom(Scenes.shelf) }
        for (t in Toy.entries) {
            val cx = Scenes.SLOT[t.ordinal % 3]; val by = Scenes.PLANK[t.ordinal / 3]
            Toys.drawAt(shelf, t, Toys.parts(t, t == Toy.FROG, if (t == Toy.PENGUIN) .5f else 0f), cx, by, St(), 0.0)
            Scenes.plate(shelf, cx, by, (t.ordinal % 4) / 3f)
        }
        Png.write(File(out, "shelf.png"), shelf, 3)
        val pins = listOf(20, 65, 128, 210, 300, 420, 530, 640, 700, 810, 900).mapIndexed { i, m -> DrumPin(m, Toy.entries[i % 9], i == 8) }
        val strip = Buf(Scenes.STRIP_W, Scenes.STRIP_H)
        Scenes.strip(strip, pins, 960, 600)
        Png.write(File(out, "strip.png"), strip, 3)
        val box = Buf(Scenes.BW, Scenes.BH).also { it.copyFrom(Scenes.box) }
        Scenes.drum(box, pins, 960, 300.0, LongArray(9) { if (it == 3) 1000 else 0 }, 1100)
        listOf(Toy.BALLET, Toy.FROG, Toy.MONKEY, Toy.BIRD).forEachIndexed { k, t -> Toys.drawAt(box, t, Toys.parts(t, false, 0f), 27 + k * 54, Scenes.TABLE, St(), 0.0) }
        Png.write(File(out, "box.png"), box, 3)
        val bench = Buf(Scenes.EW, Scenes.EH).also { it.copyFrom(Scenes.bench) }
        Toys.drawAt(bench, Toy.MONKEY, Toys.parts(Toy.MONKEY, false, 0f), Scenes.EW / 2, Scenes.ETOP, St(), 0.0)
        Scenes.gauge(bench, .6f)
        Png.write(File(out, "bench.png"), bench, 4)
        val m = Buf(Scenes.MW, Scenes.MH)
        Scenes.machine(m, rgb(0xefe0bf), 0.0, 0.0, 2, 54, 0)
        Png.write(File(out, "machine.png"), m, 4)
    }
}

/** The launcher icon's picture: the frog under the lamp, 60×60 art pixels with a transparent ground. */
class RenderArtIcon {
    private val out = File(System.getProperty("render.out") ?: "build/art").apply { mkdirs() }

    @Test fun icon() {
        val b = Buf(60, 60)
        for (y in 0 until 60) for (x in 0 until 60) {
            val d = kotlin.math.hypot(x - 29.5, y - 30.5)
            val lv = ((1 - d / 27).coerceAtLeast(0.0) * 3 + bay(x, y) * .7).toInt()
            if (lv >= 1) b.p(x, y, intArrayOf(0, 0x33463a, 0x4a5238, 0x665c3a)[lv.coerceAtMost(3)])
        }
        val big = Buf(48, 48)
        Toys.drawAt(big, Toy.FROG, Toys.parts(Toy.FROG, false, 0f), 24, 46, St().apply { kph = .2 }, 0.0, shadow = false)
        // The frog at full size, centred, its key showing.
        b.draw(big, 6, 2)
        Png.write(File(out, "icon.png"), b, 1)
        Png.write(File(out, "icon-big.png"), b, 8)
        File(out, "icon.png").copyTo(File(out, "../../../docs/icon-src.png").apply { parentFile?.mkdirs() }, overwrite = true)
    }
}

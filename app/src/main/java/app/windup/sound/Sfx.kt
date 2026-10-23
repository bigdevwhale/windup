package app.windup.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * The workshop's sounds, synthesised once into small WAV files: the ratchet of a key, nine music-box notes
 * (a pentatonic run, one per toy) and the ping of an overwound spring.
 */
class Sfx(private val context: Context) {
    private val pool = SoundPool.Builder().setMaxStreams(10)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        .build()
    private var click = 0
    private var ping = 0
    private val notes = IntArray(NOTES.size)
    @Volatile var on = true

    fun load() {
        val dir = File(context.cacheDir, "sfx").apply { mkdirs() }
        fun file(name: String, pcm: ShortArray) = File(dir, "$name.wav").also { if (!it.exists()) Wav.write(it, pcm) }.absolutePath
        click = pool.load(file("click2", Synth.ratchet()), 1)
        ping = pool.load(file("ping2", Synth.ping()), 1)
        NOTES.forEachIndexed { i, f -> notes[i] = pool.load(file("note2-$i", Synth.box(f)), 1) }
    }

    fun click() { if (on && click != 0) pool.play(click, .9f, .9f, 1, 0, 1f) }
    fun ping() { if (on && ping != 0) pool.play(ping, .6f, .6f, 1, 0, 1f) }

    /** One note of the box; [detune] slightly sours an overwound pin. */
    fun note(i: Int, vol: Float = .8f, detune: Boolean = false) {
        if (on) notes.getOrNull(i)?.takeIf { it != 0 }?.let { pool.play(it, vol, vol, 1, 0, if (detune) 1.012f else 1f) }
    }

    companion object {
        /** C major pentatonic from C5: the frog is the lowest, the acrobat the highest. */
        val NOTES = doubleArrayOf(523.25, 587.33, 659.25, 783.99, 880.0, 1046.5, 1174.66, 1318.51, 1567.98)
    }
}

object Synth {
    const val RATE = 22050

    fun ratchet(): ShortArray {
        val n = (RATE * .03).toInt()
        val r = Random(7)
        var lp = 0.0
        return ShortArray(n) { i ->
            val env = (1 - i / n.toDouble()).pow(3)
            val noise = r.nextDouble() * 2 - 1
            lp += (noise - lp) * .55
            val hp = noise - lp
            val tick = sin(2 * PI * 2900 * i / RATE) * exp(-i / (RATE * .004))
            ((hp * .6 + tick * .5) * env * 26000).toInt().coerceIn(-32767, 32767).toShort()
        }
    }

    /** A plucked steel tooth: the fundamental, a bright octave and a faint inharmonic third partial. */
    fun box(f: Double): ShortArray {
        val n = (RATE * 1.6).toInt()
        return ShortArray(n) { i ->
            val t = i / RATE.toDouble()
            val a = (t / .003).coerceAtMost(1.0)
            val v = sin(2 * PI * f * t) * exp(-t * 2.6) + .28 * sin(2 * PI * f * 2 * t) * exp(-t * 5.0) + .08 * sin(2 * PI * f * 3.01 * t) * exp(-t * 8.0)
            (v * a * 15000).toInt().coerceIn(-32767, 32767).toShort()
        }
    }

    fun ping(): ShortArray {
        val n = (RATE * .9).toInt()
        return ShortArray(n) { i ->
            val t = i / RATE.toDouble()
            val v = sin(2 * PI * 1830 * t) * .6 + sin(2 * PI * 2745 * t) * .3 + sin(2 * PI * 3610 * t) * .2
            (v * exp(-t * 6) * 9000).toInt().toShort()
        }
    }
}

object Wav {
    fun write(f: File, pcm: ShortArray) {
        val data = ByteArrayOutputStream()
        for (s in pcm) { data.write(s.toInt() and 0xFF); data.write((s.toInt() shr 8) and 0xFF) }
        val bytes = data.toByteArray()
        DataOutputStream(f.outputStream().buffered()).use { o ->
            fun le32(v: Int) { o.write(v and 0xFF); o.write((v shr 8) and 0xFF); o.write((v shr 16) and 0xFF); o.write((v shr 24) and 0xFF) }
            fun le16(v: Int) { o.write(v and 0xFF); o.write((v shr 8) and 0xFF) }
            o.writeBytes("RIFF"); le32(36 + bytes.size); o.writeBytes("WAVE")
            o.writeBytes("fmt "); le32(16); le16(1); le16(1); le32(Synth.RATE); le32(Synth.RATE * 2); le16(2); le16(16)
            o.writeBytes("data"); le32(bytes.size); o.write(bytes)
        }
    }
}

package app.windup.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.windup.domain.Toy
import app.windup.ui.art.Buf
import app.windup.ui.art.St
import app.windup.ui.art.Toys
import app.windup.ui.theme.C
import app.windup.ui.theme.T

/**
 * The pixel frame of the prototype: a 2dp ink edge on every side, with a light inner rim on top and a
 * darker one below, like a printed price tag.
 */
fun Modifier.px(bg: Color = C.light, bottom: Color = Color(0x38602814), top: Color = Color(0x80FFFFFF)) = this
    .padding(2.dp)
    .drawBehind {
        val e = 2.dp.toPx()
        drawRect(C.ink, Offset(-e, 0f), Size(size.width + 2 * e, size.height))
        drawRect(C.ink, Offset(0f, -e), Size(size.width, size.height + 2 * e))
        drawRect(bg)
        drawRect(top, Offset.Zero, Size(size.width, e))
        drawRect(bottom, Offset(0f, size.height - e), Size(size.width, e))
    }

enum class Kind { TIN, BRASS, GHOST, TEAL }

@Composable
fun Btn(text: String, modifier: Modifier = Modifier, kind: Kind = Kind.TIN, enabled: Boolean = true, big: Boolean = false, onClick: () -> Unit) {
    val (bg, lo, hi, fg) = when (kind) {
        Kind.TIN -> listOf(C.tin, C.tinDeep, C.tinHi, C.light)
        Kind.BRASS -> listOf(C.brass, C.brassDeep, C.brassHi, C.ink)
        Kind.GHOST -> listOf(C.light, C.paper2, Color.White, C.ink)
        Kind.TEAL -> listOf(C.teal, C.tealDeep, Color(0xFF8AD8CC), C.light)
    }
    Box(
        modifier
            .px(if (enabled) bg else C.paper2, bottom = if (enabled) lo else C.paper2, top = if (enabled) hi else C.paper2)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = 12.dp, end = 12.dp, top = if (big) 14.dp else 8.dp, bottom = if (big) 16.dp else 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = (if (big) T.Display else T.Button).copy(color = if (enabled) fg else C.faint), textAlign = TextAlign.Center) }
}

@Composable
fun Panel(modifier: Modifier = Modifier, bg: Color = C.light, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp).px(bg).padding(horizontal = 12.dp, vertical = 10.dp), content = content)
}

@Composable
fun Chip(text: String, on: Boolean, modifier: Modifier = Modifier, sub: String? = null, onClick: () -> Unit) {
    Column(modifier.px(if (on) Color(0xFFF6DC84) else C.light).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 7.dp)) {
        Text(text, style = T.Body)
        if (sub != null) Text(sub, style = T.Small)
    }
}

@Composable
fun Bar(p: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 12.dp) {
    Box(modifier.height(height + 4.dp).px(C.paper2, C.paper2, C.paper2)) {
        Box(Modifier.fillMaxWidth(p.coerceIn(0f, 1f)).height(height).background(color))
    }
}

/** The dark strip at the top of every screen, the colour of the shelf's wall. */
@Composable
fun TopBar(title: String, sub: String? = null, modifier: Modifier = Modifier, end: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().background(C.wall).statusBarsPadding().padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Title.copy(color = C.light))
            if (sub != null) Text(sub, style = T.Small.copy(color = C.wallText))
        }
        end()
    }
}

@Composable
fun Gap(h: Int = 10) = Spacer(Modifier.height(h.dp))

@Composable
fun Flow(modifier: Modifier = Modifier, gap: Dp = 8.dp, content: @Composable () -> Unit) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) { content() }
}

/** A full-screen sheet over the shelf, with a title and a way back. */
@Composable
fun Sheet(title: String, onClose: () -> Unit, closeLabel: String, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(C.shade).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(10.dp).px(C.paper)) {
            Row(Modifier.fillMaxWidth().background(C.wall).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = T.Title.copy(color = C.light), modifier = Modifier.weight(1f))
                Text(closeLabel, style = T.Button.copy(color = C.wallText), modifier = Modifier.clickable(onClick = onClose).padding(6.dp))
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp), content = content)
        }
    }
}

/** A still toy, trimmed to its pixels, at [k] dp per art pixel. */
@Composable
fun ToyIcon(t: Toy, painted: Boolean, rust: Float, k: Float, modifier: Modifier = Modifier, sil: Int = 0, alloy: Int = 0) {
    val bmp = remember(t, painted, rust, sil, alloy) {
        val b = Buf(48, 48)
        Toys.drawAt(b, t, Toys.parts(t, painted, rust, alloy >= 4), 24, 46, St(), 0.0, sil = sil, shadow = false, alloy = alloy)
        var x0 = 48; var y0 = 48; var x1 = 0; var y1 = 0
        for (y in 0 until 48) for (x in 0 until 48) if (b.d[y * 48 + x] ushr 24 != 0) { x0 = minOf(x0, x); x1 = maxOf(x1, x); y0 = minOf(y0, y); y1 = maxOf(y1, y) }
        val w = x1 - x0 + 1; val h = y1 - y0 + 1
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { it.setPixels(b.d, y0 * 48 + x0, 48, 0, 0, w, h) }.asImageBitmap()
    }
    Canvas(modifier.size((bmp.width * k).dp, (bmp.height * k).dp)) {
        drawImage(bmp, IntOffset.Zero, IntSize(bmp.width, bmp.height), IntOffset.Zero, IntSize(size.width.toInt(), size.height.toInt()), filterQuality = FilterQuality.None)
    }
}

@Composable
fun Dots(n: Int, of: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        for (i in 0 until of) Box(Modifier.weight(1f).height((12 + i * 6).dp).px(when { i < n -> C.brassDeep; i == n -> C.brass; else -> C.paper2 }, C.ink.copy(alpha = 0f), C.ink.copy(alpha = 0f)))
    }
}

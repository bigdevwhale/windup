package app.windup.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import java.time.LocalDate
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.windup.domain.Toy
import app.windup.ui.art.G
import app.windup.ui.art.Img
import app.windup.ui.art.St
import app.windup.ui.art.Toys
import app.windup.ui.theme.C
import app.windup.ui.theme.T

/** Fills the scope with [tex] repeated every [tile] dp, at [alpha]. */
private fun DrawScope.texture(tex: String, alpha: Float, tile: Float = 256f) {
    if (alpha <= 0f) return
    val g = G(this, density)
    // The texture is drawn faintly by laying it over and letting the colour beneath show through.
    drawContext.canvas.saveLayer(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), androidx.compose.ui.graphics.Paint().apply { this.alpha = alpha })
    g.tiled(Img[tex], 0, 0, size.width / density, size.height / density, tile)
    drawContext.canvas.restore()
}

/** A brass rivet, the kind that holds a tin toy's plates together. */
private fun DrawScope.rivet(c: Offset, r: Float) {
    drawCircle(Color.Black.copy(alpha = .35f), r * 1.15f, c + Offset(0f, r * .35f))
    drawCircle(Brush.radialGradient(listOf(C.brassHi, C.brass, C.brassDeep), c - Offset(r * .4f, r * .4f), r * 1.6f), r, c)
}

/** The page under a screen: paper by day, felt by evening, a little darker towards the edges. */
fun Modifier.screen(): Modifier = this.drawBehind {
    drawRect(C.paper)
    texture(C.p.paperTex, C.p.paperTexAlpha, 320f)
    drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = if (C.dark) .35f else .10f)), Offset(size.width / 2, size.height * .35f), size.maxDimension * .8f))
}

/**
 * A card: a plate of card stock (or, in the evening, walnut) with a hairline brass edge, light catching its top,
 * and brass [rivets] at the corners for the big panels.
 */
fun Modifier.card(bg: Color = C.surface, radius: Dp = 18.dp, edge: Color = C.line, lift: Dp = 2.dp, rivets: Boolean = false): Modifier {
    val shape = RoundedCornerShape(radius)
    val tex = bg == C.surface
    return this
        .shadow(lift, shape, ambientColor = Color.Black.copy(alpha = .4f), spotColor = Color.Black.copy(alpha = .4f))
        .clip(shape)
        .drawBehind {
            drawRect(bg)
            if (tex) texture(C.p.surfaceTex, C.p.surfaceTexAlpha)
            drawRect(Brush.verticalGradient(
                0f to Color.White.copy(alpha = if (C.dark) .06f else .22f), .35f to Color.Transparent,
                1f to Color.Black.copy(alpha = if (C.dark) .18f else .05f),
            ))
            if (rivets) {
                val i = 10.dp.toPx(); val r = 2.6.dp.toPx()
                for (o in listOf(Offset(i, i), Offset(size.width - i, i), Offset(i, size.height - i), Offset(size.width - i, size.height - i))) rivet(o, r)
            }
        }
        .border(1.dp, edge, shape)
}

/** Polished metal, lit from above: brass, tin red, teal enamel. */
fun brassBrush() = Brush.verticalGradient(listOf(C.brassHi, C.brass, C.brassDeep))
fun tinBrush() = Brush.verticalGradient(listOf(C.tinHi, C.tin, C.tinDeep))
fun tealBrush() = Brush.verticalGradient(listOf(Color(0xFF6CC7BD), C.teal, Color(0xFF26665E)))

enum class Kind { TIN, BRASS, GHOST, TEAL }

@Composable
fun Btn(text: String, modifier: Modifier = Modifier, kind: Kind = Kind.TIN, enabled: Boolean = true, big: Boolean = false, onClick: () -> Unit) {
    val press = remember { MutableInteractionSource() }
    val down by press.collectIsPressedAsState()
    val scale by animateFloatAsState(if (down) .97f else 1f, label = "press")
    val shape = RoundedCornerShape(if (big) 20.dp else 14.dp)
    val fg = when {
        !enabled -> C.faint
        kind == Kind.BRASS -> C.onBrass
        kind == Kind.GHOST -> C.ink
        else -> C.light
    }
    val fill: Brush? = when {
        !enabled -> null
        kind == Kind.TIN -> tinBrush()
        kind == Kind.BRASS -> brassBrush()
        kind == Kind.TEAL -> tealBrush()
        else -> null
    }
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (fill != null) 3.dp else 0.dp, shape, ambientColor = Color.Black.copy(alpha = .5f), spotColor = Color.Black.copy(alpha = .5f))
            .clip(shape)
            .then(if (fill != null) Modifier.background(fill) else Modifier.background(if (enabled) C.surface.copy(alpha = .55f) else C.paper2))
            .border(1.dp, if (fill != null) Color.White.copy(alpha = .22f) else if (enabled) C.brassDeep.copy(alpha = .55f) else Color.Transparent, shape)
            .clickable(interactionSource = press, indication = ripple(), enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = if (big) 18.dp else 11.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = (if (big) T.Display else T.Button).copy(color = fg), textAlign = TextAlign.Center) }
}

@Composable
fun Panel(modifier: Modifier = Modifier, bg: Color = C.surface, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).card(bg, rivets = true).padding(horizontal = 18.dp, vertical = 16.dp), content = content)
}

@Composable
fun Chip(text: String, on: Boolean, modifier: Modifier = Modifier, sub: String? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .then(if (on) Modifier.shadow(2.dp, shape).clip(shape).background(brassBrush()).border(1.dp, Color.White.copy(alpha = .25f), shape)
            else Modifier.card(radius = 14.dp, lift = 0.dp))
            .clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 8.dp),
    ) {
        Text(text, style = T.Body.copy(fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold, color = if (on) C.onBrass else C.ink))
        if (sub != null) Text(sub, style = T.Small.copy(color = if (on) C.onBrass.copy(alpha = .7f) else C.muted))
    }
}

@Composable
fun Bar(p: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 10.dp) {
    val shape = RoundedCornerShape(height / 2)
    Box(modifier.height(height).clip(shape).background(C.paper2).border(.5.dp, Color.Black.copy(alpha = .12f), shape)) {
        Box(
            Modifier.fillMaxWidth(p.coerceIn(0f, 1f)).height(height).clip(shape)
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .35f).compositeOver(color), color, color.copy(alpha = .85f).compositeOver(Color.Black)))),
        )
    }
}

private fun Color.compositeOver(bg: Color): Color {
    val a = alpha
    return Color(red * a + bg.red * (1 - a), green * a + bg.green * (1 - a), blue * a + bg.blue * (1 - a), 1f)
}

/** The strip at the top of every screen: the shelf's wallpaper in shadow, with a brass rule under it. */
@Composable
fun TopBar(title: String, sub: String? = null, modifier: Modifier = Modifier, end: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier.fillMaxWidth()
            .drawBehind {
                drawRect(C.wall)
                texture("scene_wall", .1f, 120f)
                drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .25f), Color.Transparent)))
                drawRect(Brush.horizontalGradient(listOf(Color.Transparent, C.brassDeep, C.brass, C.brassDeep, Color.Transparent)), Offset(0f, size.height - 1.5.dp.toPx()), androidx.compose.ui.geometry.Size(size.width, 1.5.dp.toPx()))
            }
            .statusBarsPadding().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Display.copy(color = C.light))
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
fun Sheet(title: String, onClose: () -> Unit, closeLabel: String, help: GuideState? = null, helpLabel: String = "?", content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(C.shade).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(top = 12.dp)
                .shadow(12.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)).screen(),
        ) {
            TopBar(title, modifier = Modifier.padding(0.dp)) {
                if (help != null) HelpButton(help, helpLabel)
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, closeLabel, tint = C.wallText) }
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 10.dp), content = content)
        }
    }
}

/** A still toy, its key at rest, [k] dp to the scene unit. */
@Composable
fun ToyIcon(t: Toy, painted: Boolean, rust: Float, k: Float, modifier: Modifier = Modifier, sil: Int = 0, alloy: Int = 0) {
    val p = painted && sil == 0
    val (ww, hh) = Toys.size(t, p)
    val reach = Toys.keyReach(t, p)
    Canvas(modifier.size(((ww + reach) * k).dp, (hh * k).dp)) {
        val g = G(this, k.dp.toPx())
        g.shift(reach + ww / 2, hh) {
            Toys.drawAt(g, t, Toys.parts(t, painted, rust, alloy >= 4), 0, 0, St(), 0.0, sil = sil, shadow = false, alloy = alloy)
        }
    }
}

@Composable
fun Dots(n: Int, of: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        for (i in 0 until of) Box(
            Modifier.weight(1f).height((10 + i * 5).dp).clip(RoundedCornerShape(4.dp))
                .then(if (i <= n) Modifier.background(if (i == n) brassBrush() else Brush.verticalGradient(listOf(C.brassDeep, C.brassDeep))) else Modifier.background(C.paper2)),
        )
    }
}

/** A painted gear or spring, for prices and purses. */
@Composable
fun PartIcon(spring: Boolean, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Image(Img[if (spring) "ui_spring" else "ui_gear"], null, modifier.size(size), filterQuality = FilterQuality.Medium)
}

/** How many gears or springs there are, as a small pill: the part's picture and the count. */
@Composable
fun Coin(spring: Boolean, n: Int, modifier: Modifier = Modifier, dark: Boolean = true) {
    Row(
        modifier.clip(RoundedCornerShape(50))
            .background(if (dark) Color.Black.copy(alpha = .25f) else C.paper2.copy(alpha = .7f))
            .border(1.dp, if (dark) C.brass.copy(alpha = .25f) else C.line, RoundedCornerShape(50))
            .padding(start = 6.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        PartIcon(spring, 20.dp)
        Text("$n", style = T.Button.copy(color = if (dark) C.brass else C.ink, fontFeatureSettings = "tnum"))
    }
}

/** A row of choices in a groove, the picked one a brass tab. */
@Composable
fun Segments(items: List<String>, sel: Int, modifier: Modifier = Modifier, badges: List<Int> = emptyList(), onSel: (Int) -> Unit) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(C.paper2)
            .border(1.dp, Color.Black.copy(alpha = if (C.dark) .3f else .08f), RoundedCornerShape(50)).padding(4.dp),
    ) {
        items.forEachIndexed { i, label ->
            val on = i == sel
            val a by animateFloatAsState(if (on) 1f else 0f, label = "seg")
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(50)).drawBehind { if (a > 0) drawRect(brassBrush(), alpha = a) }
                    .clickable { onSel(i) }.padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = T.Button.copy(fontSize = 13.sp, color = if (on) C.onBrass else C.muted), maxLines = 1)
                    val b = badges.getOrNull(i) ?: 0
                    if (b > 0) Box(Modifier.padding(start = 4.dp).size(16.dp).clip(CircleShape).background(C.tin), contentAlignment = Alignment.Center) {
                        Text("$b", style = T.Label.copy(color = C.light, fontSize = 10.sp, lineHeight = 10.sp, letterSpacing = 0.sp))
                    }
                }
            }
        }
    }
}

/** One figure with its caption, for a row of them. */
@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier, accent: Color = C.ink) {
    Column(modifier.card(radius = 16.dp).padding(horizontal = 12.dp, vertical = 12.dp)) {
        Text(value, style = T.Title.copy(color = accent, fontFeatureSettings = "tnum"), maxLines = 1)
        Text(label, style = T.Small, maxLines = 2)
    }
}

/** A heading over a group, set small in capitals with a brass rule running out after it. */
@Composable
fun Caption(text: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = T.Label.copy(color = if (C.dark) C.brass else C.brassDeep))
        Box(Modifier.padding(start = 10.dp).weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(C.line, Color.Transparent))))
    }
}

/**
 * One figure in a grid of them, all the same size: an icon in a brass medallion, the figure, what it counts,
 * and a line of context under it.
 */
@Composable
fun Metric(icon: @Composable () -> Unit, value: String, label: String, modifier: Modifier = Modifier, sub: String? = null, subColor: Color = C.muted) {
    Column(modifier.height(118.dp).card(radius = 18.dp).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(brassBrush()).border(1.dp, Color.White.copy(alpha = .3f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Spacer(Modifier.weight(1f))
        Text(value, style = T.Title.copy(fontSize = 22.sp, fontFeatureSettings = "tnum"), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = T.Small.copy(color = C.ink, fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(sub ?: " ", style = T.Small.copy(fontSize = 12.sp, color = subColor), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A material icon sized and inked for a [Metric] medallion. */
@Composable
fun MetricIcon(v: ImageVector) = Icon(v, null, tint = C.onBrass, modifier = Modifier.size(18.dp))

/**
 * The last days as a strip of calendar cells, today at the right end: the weekday, the date, and a little
 * gauge of how many pins the day got. The picked day is a brass tab.
 */
@Composable
fun DayStrip(days: List<LocalDate>, pins: List<Int>, sel: Int, today: LocalDate, ru: Boolean, modifier: Modifier = Modifier, onSel: (Int) -> Unit) {
    val scroll = rememberScrollState()
    LaunchedEffect(scroll.maxValue) { scroll.scrollTo(scroll.maxValue) }
    val loc = if (ru) Locale.forLanguageTag("ru") else Locale.ENGLISH
    Row(modifier.fillMaxWidth().horizontalScroll(scroll).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.forEachIndexed { i, d ->
            val on = i == sel
            val n = pins.getOrElse(i) { 0 }
            val shape = RoundedCornerShape(16.dp)
            Column(
                Modifier.width(50.dp).height(78.dp)
                    .then(if (on) Modifier.shadow(3.dp, shape).clip(shape).background(brassBrush()).border(1.dp, Color.White.copy(alpha = .3f), shape) else Modifier.card(radius = 16.dp, lift = 0.dp))
                    .clickable { onSel(i) }.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val ink = if (on) C.onBrass else C.ink
                Text(
                    if (d == today) (if (ru) "СЕГ" else "TODAY") else d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, loc).uppercase(),
                    style = T.Label.copy(color = if (on) C.onBrass.copy(alpha = .75f) else if (d == today) C.tin else C.muted, fontSize = 10.sp),
                )
                Text("${d.dayOfMonth}", style = T.Title.copy(color = ink, fontFeatureSettings = "tnum"))
                Spacer(Modifier.weight(1f))
                // Four little bars: one per two pins, so a busy day fills the gauge.
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                    for (k in 0 until 4) {
                        val lit = n > k * 2
                        Box(
                            Modifier.width(5.dp).height((4 + k * 2).dp).clip(RoundedCornerShape(1.5.dp))
                                .background(if (lit) (if (on) C.onBrass else C.brassDeep) else (if (on) C.onBrass.copy(alpha = .18f) else C.paper2)),
                        )
                    }
                }
            }
        }
    }
}

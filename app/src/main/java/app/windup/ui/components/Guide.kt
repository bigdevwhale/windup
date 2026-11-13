package app.windup.ui.components

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.windup.container
import app.windup.ui.Guide
import app.windup.ui.Home
import app.windup.ui.Words
import app.windup.ui.theme.C
import app.windup.ui.theme.T

/** A screen's guide: shown the first time the screen opens, put away for good with "Got it", called back with "?". */
@Stable
class GuideState(val key: String, seen: Boolean, private val save: (String) -> Unit) {
    var visible by mutableStateOf(!seen)
        private set

    fun show() { visible = true }
    fun toggle() { visible = !visible }
    fun dismiss() { visible = false; save(key) }
}

@Composable
fun rememberGuide(h: Home, key: String): GuideState {
    val c = LocalContext.current.container
    val seen = key in h.settings.seen
    return remember(key, seen) { GuideState(key, seen) { k -> c.write { settings.update { it.copy(seen = it.seen + k) } } } }
}

/** One point of a guide: an icon and a sentence. */
class Tip(val icon: ImageVector, val text: String)

/** The "?" in a screen's top bar: brings its guide back, or puts it away. */
@Composable
fun HelpButton(g: GuideState, label: String) {
    IconButton(onClick = { g.toggle() }) {
        Icon(Icons.AutoMirrored.Rounded.HelpOutline, label, tint = if (g.visible) C.brass else C.wallText)
    }
}

/**
 * The guide card: a brass-lit plate at the top of the screen with a few points, each with its icon, and a
 * button to put it away.
 */
@Composable
fun GuideCard(g: GuideState, guide: Guide, w: Words, modifier: Modifier = Modifier) {
    AnimatedVisibility(g.visible, modifier, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).card(C.glow, rivets = true, edge = C.brassDeep.copy(alpha = .5f)).padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoStories, null, tint = if (C.dark) C.brass else C.brassDeep, modifier = Modifier.size(20.dp))
                Text(guide.title, style = T.Heading, modifier = Modifier.padding(start = 8.dp))
            }
            for (t in guide.tips) Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(brassBrush()).border(1.dp, Color.White.copy(alpha = .3f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(t.icon, null, tint = C.onBrass, modifier = Modifier.size(16.dp)) }
                Text(t.text, style = T.Body, modifier = Modifier.padding(start = 12.dp, top = 3.dp).weight(1f))
            }
            Btn(w.t("Понятно", "Got it"), Modifier.padding(top = 14.dp).align(Alignment.End), kind = Kind.BRASS) { g.dismiss() }
        }
    }
}

package org.sandhimitra.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sandhimitra.app.R

/** Global header: small logo at the top-left corner, the Sanskrit wordmark centred, Menu button on the right. */
@Composable
fun GlobalHeader(onMenu: () -> Unit) {
    val t = LocalTokens.current
    Column(Modifier.fillMaxWidth().background(t.card)) {
        Box(Modifier.fillMaxWidth().statusBarsPadding().height(60.dp).padding(horizontal = 12.dp)) {
            Image(painterResource(R.drawable.header_icon), contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.align(Alignment.CenterStart).size(40.dp).clip(RoundedCornerShape(9.dp)))
            Wordmark(Modifier.align(Alignment.Center).height(38.dp))
            IconButton(onClick = onMenu, modifier = Modifier.align(Alignment.CenterEnd).semantics { contentDescription = "Menu" }) {
                Icon(Icons.Default.Menu, contentDescription = null, tint = t.primary, modifier = Modifier.size(30.dp))
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(t.outline))
    }
}

/** The supplied wordmark artwork with its white background removed; it takes the theme's contrast colour (original navy on light, light tone on dark). */
@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    val t = LocalTokens.current
    Image(painterResource(R.drawable.sanskrit_wordmark_transparent), contentDescription = "सन्धिमित्रम्", contentScale = ContentScale.Fit,
        colorFilter = if (t.dark) androidx.compose.ui.graphics.ColorFilter.tint(t.text) else null, modifier = modifier.aspectRatio(2137f / 709f))
}

@Composable
fun AttributionFooter(modifier: Modifier = Modifier) {
    val t = LocalTokens.current
    Column(modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Sx("Compiled and Created by", size = 12.sp, color = t.muted, align = TextAlign.Center)
        Sx("©Dr. Diddigi Vamsi Krishna", size = 14.sp, weight = FontWeight.SemiBold, color = t.primary, align = TextAlign.Center)
    }
}

/** Standard page: header, optional back + title row, scrollable content, attribution footer (pinned near the bottom on short pages). */
@Composable
fun Page(onMenu: () -> Unit, onBack: (() -> Unit)?, title: String? = null, bottomBar: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val t = LocalTokens.current
    Column(Modifier.fillMaxSize().background(t.bg)) {
        GlobalHeader(onMenu)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val minH = maxHeight
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(Modifier.fillMaxWidth().heightIn(min = minH).navigationBarsPadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.fillMaxWidth()) {
                        if (onBack != null || title != null) {
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (onBack != null) IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = "Back" }) { Icon(Icons.Default.ArrowBack, null, tint = t.primary) }
                                if (title != null) Sx(title, size = 20.sp, weight = FontWeight.SemiBold, color = t.primary, modifier = Modifier.weight(1f).padding(end = 8.dp))
                            }
                        } else Spacer(Modifier.height(12.dp))
                        content()
                    }
                    AttributionFooter()
                }
            }
        }
        bottomBar?.invoke()
    }
}

@Composable
fun CardBox(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val t = LocalTokens.current
    val shape = RoundedCornerShape(14.dp)
    val m = modifier.clip(shape).background(t.card).border(BorderStroke(1.dp, t.outline), shape).let { if (onClick != null) it.clickable(onClick = onClick) else it }
    Column(m.padding(14.dp), content = content)
}

/** Segmented capsule selector. */
@Composable
fun Capsule(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, textSize: Int = 14) {
    val t = LocalTokens.current
    val shape = RoundedCornerShape(50)
    Row(modifier.fillMaxWidth().clip(shape).border(BorderStroke(1.dp, t.primary), shape)) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            Box(Modifier.weight(1f).background(if (sel) t.primary else Color.Transparent).clickable { onSelect(i) }.padding(vertical = 10.dp, horizontal = 4.dp), contentAlignment = Alignment.Center) {
                Sx(label, size = textSize.sp, weight = if (sel) FontWeight.SemiBold else FontWeight.Normal, color = if (sel) t.onPrimary else t.primary, align = TextAlign.Center, lineHeight = (textSize * 1.3f).sp)
            }
        }
    }
}

@Composable
fun StatusChip(status: org.sandhimitra.app.engine.Status) {
    val t = LocalTokens.current
    val (key, col) = when (status) {
        org.sandhimitra.app.engine.Status.VERIFIED -> "verified" to t.ok
        org.sandhimitra.app.engine.Status.PROVISIONAL -> "provisional" to t.accent
        org.sandhimitra.app.engine.Status.NO_RESULT -> "no_result" to t.muted
    }
    Box(Modifier.clip(RoundedCornerShape(50)).border(BorderStroke(1.dp, col), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp)) {
        Sx(tr(key), size = 12.sp, weight = FontWeight.SemiBold, color = col)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Sx(text, size = 18.sp, weight = FontWeight.SemiBold, color = LocalTokens.current.accent, modifier = modifier.padding(top = 16.dp, bottom = 6.dp))
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, filled: Boolean = true) {
    val t = LocalTokens.current
    val shape = RoundedCornerShape(50)
    val m = modifier.clip(shape).let { if (filled) it.background(t.primary) else it.border(BorderStroke(1.dp, t.primary), shape) }.clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 11.dp)
    Box(m, contentAlignment = Alignment.Center) { Sx(text, size = 15.sp, weight = FontWeight.SemiBold, color = if (filled) t.onPrimary else t.primary, align = TextAlign.Center) }
}

fun Modifier.androidxBorder(t: Tokens): Modifier = this.border(BorderStroke(1.dp, t.outline), RoundedCornerShape(16.dp))
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)

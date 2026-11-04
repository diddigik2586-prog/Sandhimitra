package org.sandhimitra.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.sandhimitra.app.R
import org.sandhimitra.app.SandhimitraApp

/** Five-second splash: supplied icon, supplied wordmark PNG, determinate progress bar and the approved attribution. No other text. */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val t = LocalTokens.current
    var started by rememberSaveable { mutableStateOf(false) }
    val p by animateFloatAsState(if (started) 1f else 0f, tween(5000, easing = LinearEasing), label = "splash")
    LaunchedEffect(Unit) { started = true; delay(5000); onDone() }
    Column(Modifier.fillMaxSize().background(t.bg).systemBarsPadding().padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Image(painterResource(R.drawable.header_icon), null, Modifier.size(130.dp).clip(RoundedCornerShape(26.dp)), contentScale = ContentScale.Fit)
        Spacer(Modifier.height(28.dp))
        Wordmark(Modifier.fillMaxWidth(0.45f))
        Spacer(Modifier.height(36.dp))
        LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth(0.7f).height(6.dp).clip(RoundedCornerShape(3.dp)), color = t.primary, trackColor = t.outline)
        Spacer(Modifier.weight(1f))
        AttributionFooter()
    }
}

@Composable
fun HomeScreen(app: SandhimitraApp, onMenu: () -> Unit, go: (String) -> Unit) {
    val t = LocalTokens.current
    Page(onMenu, null) {
        // Tier 1: Introduction (horizontal, full width)
        HomeBar(tr("introduction")) { go(Routes.INTRO) }
        Spacer(Modifier.height(12.dp))
        // Tier 2: exactly four equal square tiles in a 2 x 2 grid
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = 12.dp
            val side = (maxWidth - gap) / 2
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    HomeTile(tr("vowel"), side, app, "vowel") { go(Routes.cat("vowel")) }
                    HomeTile(tr("consonant"), side, app, "consonant") { go(Routes.cat("consonant")) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    HomeTile(tr("visarga"), side, app, "visarga") { go(Routes.cat("visarga")) }
                    HomeTile(tr("prakriti"), side, app, "prakriti") { go(Routes.cat("prakriti")) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        // Tier 3: Sandhātrī engine (horizontal, full width)
        HomeBar(tr("sandhatri")) { go(Routes.SANDHATRI) }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HomeBar(label: String, onClick: () -> Unit) {
    val t = LocalTokens.current
    val shape = RoundedCornerShape(14.dp)
    Box(Modifier.testTag("bar_$label").fillMaxWidth().heightIn(min = 78.dp).clip(shape).background(t.primary).clickable(onClick = onClick).padding(14.dp), contentAlignment = Alignment.Center) {
        Sx(label, size = 22.sp, weight = FontWeight.SemiBold, color = t.onPrimary, align = TextAlign.Center)
    }
}

@Composable
private fun HomeTile(label: String, side: androidx.compose.ui.unit.Dp, app: SandhimitraApp, group: String, onClick: () -> Unit) {
    val t = LocalTokens.current
    val topics = remember(group) { app.content.topics(group) }
    val prog = app.progress.version.let { app.progress.doneCount(topics.map { it.id }) }
    val shape = RoundedCornerShape(16.dp)
    Column(Modifier.testTag("tile_$group").size(side).clip(shape).background(t.card).androidxBorder(t).clickableNoRipple(onClick).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Sx(label, size = 20.sp, weight = FontWeight.SemiBold, color = t.primary, align = TextAlign.Center, lineHeight = 26.sp)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.width(36.dp).height(3.dp).background(t.accent, RoundedCornerShape(2.dp)))
        Spacer(Modifier.height(8.dp))
        Sx("$prog / ${topics.size}", size = 13.sp, color = t.muted, align = TextAlign.Center)
    }
}

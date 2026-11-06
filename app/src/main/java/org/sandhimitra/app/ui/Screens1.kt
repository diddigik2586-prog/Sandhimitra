package org.sandhimitra.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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

/** Home: header, Introduction bar, 2 x 2 category tiles, Sandhātrī bar, Sandhicakram card and the attribution — sized to the viewport, never scrolled. */
@Composable
fun HomeScreen(app: SandhimitraApp, onMenu: () -> Unit, go: (String) -> Unit) {
    val t = LocalTokens.current
    Column(Modifier.fillMaxSize().background(t.bg)) {
        GlobalHeader(onMenu)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp)) {
            val gap = 10.dp; val footer = 78.dp
            val avail = maxHeight - footer - 8.dp
            var h1 = 72.dp; var h2 = 92.dp
            var side = minOf((maxWidth - gap) / 2, (avail - h1 * 2 - h2 - gap * 4) / 2)
            if (side < 118.dp) { h1 = 56.dp; h2 = 76.dp; side = minOf((maxWidth - gap) / 2, (avail - h1 * 2 - h2 - gap * 4) / 2) }
            if (side < 72.dp) side = 72.dp
            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(8.dp))
                HomeBar(tr("introduction"), h1) { go(Routes.INTRO) }
                Spacer(Modifier.height(gap))
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(gap)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        HomeTile(tr("vowel"), side, app, "vowel") { go(Routes.cat("vowel")) }
                        HomeTile(tr("consonant"), side, app, "consonant") { go(Routes.cat("consonant")) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        HomeTile(tr("visarga"), side, app, "visarga") { go(Routes.cat("visarga")) }
                        HomeTile(tr("prakriti"), side, app, "prakriti") { go(Routes.cat("prakriti")) }
                    }
                }
                Spacer(Modifier.height(gap))
                HomeBar(tr("sandhatri"), h1) { go(Routes.SANDHATRI) }
                Spacer(Modifier.height(gap))
                GamesCard(h2) { go(Routes.GAMES) }
                Spacer(Modifier.weight(1f))
                AttributionFooter()
            }
        }
    }
}

/** Sandhicakram card: its own highlight in each theme (light: indigo-violet gradient with warm-gold lettering and border;
 *  dark: gold gradient with deep-indigo lettering), joystick pinned at the far left, text centred on the card independently of the icon,
 *  and a restrained moving light sweep (off when the system has animations disabled). */
@Composable
fun GamesCard(height: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val t = LocalTokens.current
    val fill = if (t.dark) listOf(Color(0xFFF3C77E), Color(0xFFD08A4E)) else listOf(Color(0xFF4A3F8F), Color(0xFF2F3E6B))
    val ink = if (t.dark) Color(0xFF1F2742) else Color(0xFFFFD88A)
    val edge = if (t.dark) Color(0xFF1F2742).copy(alpha = 0.35f) else Color(0xFFFFD88A).copy(alpha = 0.9f)
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val reduceMotion = remember { try { android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f } catch (e: Exception) { false } }
    val shape = RoundedCornerShape(14.dp)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(initialValue = -1f, targetValue = 2f, animationSpec = infiniteRepeatable(tween<Float>(2600, easing = LinearEasing), RepeatMode.Restart), label = "sweep")
    Box(Modifier.testTag("bar_games").fillMaxWidth().height(height).clip(shape).background(Brush.horizontalGradient(fill))
        .border(BorderStroke(1.5.dp, edge), shape).clickable(onClick = onClick)
        .drawWithContent {
            drawContent()
            if (!reduceMotion) {
                val w = size.width; val sx = x * w
                drawRect(brush = Brush.linearGradient(colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent), start = Offset(sx, 0f), end = Offset(sx + w * 0.45f, size.height)))
            }
        }) {
        Icon(painterResource(R.drawable.ic_gamepad), contentDescription = null, tint = ink, modifier = Modifier.testTag("games_icon").align(Alignment.CenterStart).padding(start = 18.dp).size(34.dp))
        Column(Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Sx(tr("games"), size = 22.sp, weight = FontWeight.SemiBold, color = ink, align = TextAlign.Center, lineHeight = 28.sp)
            Sx(tr("games_sub"), size = 13.sp, color = ink.copy(alpha = 0.9f), align = TextAlign.Center, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun HomeBar(label: String, height: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val t = LocalTokens.current
    val shape = RoundedCornerShape(14.dp)
    Box(Modifier.testTag("bar_$label").fillMaxWidth().height(height).clip(shape).background(t.primary).clickable(onClick = onClick).padding(14.dp), contentAlignment = Alignment.Center) {
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

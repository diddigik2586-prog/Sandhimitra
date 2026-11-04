package org.sandhimitra.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.sandhimitra.app.SandhimitraApp

object Routes {
    const val SPLASH = "splash"; const val HOME = "home"; const val INTRO = "intro"; const val CAT = "cat/{group}"; const val TOPIC = "topic/{id}"
    const val SANDHATRI = "sandhatri"; const val YOJANA = "yojana"; const val VICCHEDA = "viccheda"; const val REGISTRY = "registry"
    const val PSET = "pset/{id}"; const val ABOUT = "about"; const val DVK = "dvk"; const val SUPPORT = "support"; const val PRIVACY = "privacy"
    fun cat(g: String) = "cat/$g"; fun topic(id: String) = "topic/$id"; fun pset(id: String) = "pset/$id"
}

@Composable
fun MainApp(app: SandhimitraApp) {
    val nav = rememberNavController()
    val prefs = app.prefs
    val ctx = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var exitAsk by remember { mutableStateOf(false) }
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val t = LocalTokens.current

    fun go(r: String) { menuOpen = false; nav.navigate(r) { launchSingleTop = true } }
    fun goHome() { menuOpen = false; if (!nav.popBackStack(Routes.HOME, false)) nav.navigate(Routes.HOME) { launchSingleTop = true } }
    val open = { menuOpen = true }
    val back: () -> Unit = { nav.popBackStack() }

    BackHandler(enabled = menuOpen) { menuOpen = false }
    BackHandler(enabled = !menuOpen && route == Routes.HOME) { exitAsk = true }

    Box(Modifier.fillMaxSize().background(t.bg)) {
        NavHost(nav, startDestination = Routes.SPLASH, modifier = Modifier.fillMaxSize()) {
            composable(Routes.SPLASH) { SplashScreen { nav.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } } } }
            composable(Routes.HOME) { HomeScreen(app, open) { go(it) } }
            composable(Routes.INTRO) { IntroScreen(app, open, back) }
            composable(Routes.CAT) { e -> CategoryScreen(app, e.arguments?.getString("group") ?: "vowel", open, back) { go(it) } }
            composable(Routes.TOPIC) { e -> TopicScreen(app, e.arguments?.getString("id") ?: "t00", open, back, practiceOnly = false) }
            composable(Routes.PSET) { e -> TopicScreen(app, e.arguments?.getString("id") ?: "t40", open, back, practiceOnly = true) }
            composable(Routes.SANDHATRI) { SandhatriHome(app, open, back) { go(it) } }
            composable(Routes.YOJANA) { YojanaScreen(app, open, back) }
            composable(Routes.VICCHEDA) { VicchedaScreen(app, open, back) }
            composable(Routes.REGISTRY) { RegistryScreen(open, back) }
            composable(Routes.ABOUT) { AboutScreen(app, open, back) }
            composable(Routes.DVK) { DvkScreen(open, back) }
            composable(Routes.SUPPORT) { SupportScreen(open, back) }
            composable(Routes.PRIVACY) { PrivacyScreen(open, back) }
        }

        AnimatedVisibility(menuOpen, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { menuOpen = false })
        }
        AnimatedVisibility(menuOpen, modifier = Modifier.align(Alignment.CenterEnd), enter = slideInHorizontally { it }, exit = slideOutHorizontally { it }) {
            MenuPanel(app, onHome = { goHome() }, onIntro = { go(Routes.INTRO) }, onSandhatri = { go(Routes.SANDHATRI) }, onAbout = { go(Routes.ABOUT) }, onDvk = { go(Routes.DVK) },
                onSupport = { go(Routes.SUPPORT) }, onPrivacy = { go(Routes.PRIVACY) }, onFeedback = { menuOpen = false; sendFeedback(ctx) }, onClose = { menuOpen = false })
        }
    }

    if (exitAsk) {
        AlertDialog(onDismissRequest = { exitAsk = false },
            title = { Sx(tr("exit_title"), size = 18.sp, weight = FontWeight.SemiBold) },
            text = { Sx(tr("exit_body")) },
            confirmButton = { TextButton(onClick = { exitAsk = false; (ctx as? android.app.Activity)?.finish() }) { Sx(tr("exit"), color = t.primary, weight = FontWeight.SemiBold) } },
            dismissButton = { TextButton(onClick = { exitAsk = false }) { Sx(tr("stay"), color = t.primary) } },
            containerColor = t.card)
    }
}

/** Feedback is an email action only: ACTION_SENDTO opens the default email app with a prefilled recipient and subject; the user presses Send. */
fun sendFeedback(ctx: android.content.Context) {
    val i = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:dvkrishna92@gmail.com")).putExtra(Intent.EXTRA_SUBJECT, "Sandhimitra App Feedback")
    try { ctx.startActivity(i) } catch (e: ActivityNotFoundException) {
        val lang = (ctx.applicationContext as SandhimitraApp).prefs.lang
        Toast.makeText(ctx, S.t("no_mail", lang), Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun MenuPanel(app: SandhimitraApp, onHome: () -> Unit, onIntro: () -> Unit, onSandhatri: () -> Unit, onAbout: () -> Unit, onDvk: () -> Unit,
                      onSupport: () -> Unit, onPrivacy: () -> Unit, onFeedback: () -> Unit, onClose: () -> Unit) {
    val t = LocalTokens.current; val prefs = app.prefs
    val langs = listOf("sa", "kn", "en")
    BoxWithConstraints(Modifier.fillMaxHeight()) {
        val w = minOf(340.dp, maxWidth * 0.9f)
        Column(Modifier.width(w).fillMaxHeight().background(t.card).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Sx(tr("menu"), size = 20.sp, weight = FontWeight.SemiBold, color = t.primary, modifier = Modifier.weight(1f))
                Sx("✕", size = 22.sp, color = t.primary, modifier = Modifier.clickable { onClose() }.padding(8.dp))
            }
            // A. Preferences
            Sx(tr("prefs"), size = 13.sp, color = t.muted, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
            Sx(tr("language"), size = 13.sp, color = t.muted)
            Capsule(listOf("संस्कृतम्", "ಕನ್ನಡ", "English"), langs.indexOf(prefs.lang).coerceAtLeast(0), { prefs.chooseLang(langs[it]) }, Modifier.padding(vertical = 6.dp))
            Sx(tr("theme"), size = 13.sp, color = t.muted)
            Capsule(listOf(tr("light"), tr("dark")), if (prefs.dark) 1 else 0, { prefs.chooseDark(it == 1) }, Modifier.padding(vertical = 6.dp))
            Sx(tr("fontsize"), size = 13.sp, color = t.muted)
            Capsule(listOf(tr("fs0"), tr("fs1"), tr("fs2"), tr("fs3")), prefs.fontStep, { prefs.chooseFontStep(it) }, Modifier.padding(vertical = 6.dp), textSize = 12)
            Divider()
            // B. Main navigation
            MenuItem(tr("home"), onHome); MenuItem(tr("introduction"), onIntro); MenuItem(tr("sandhatri"), onSandhatri)
            Divider()
            // additional separator immediately above About App
            Box(Modifier.padding(bottom = 6.dp).fillMaxWidth().height(2.dp).background(t.accent))
            // C. Information and support
            MenuItem(tr("about_app"), onAbout); MenuItem(tr("about_dvk"), onDvk); MenuItem(tr("support"), onSupport); MenuItem(tr("feedback"), onFeedback)
            Divider()
            // D. Privacy
            MenuItem(tr("privacy"), onPrivacy)
        }
    }
}

@Composable private fun Divider() { Box(Modifier.padding(vertical = 10.dp).fillMaxWidth().height(1.dp).background(LocalTokens.current.outline)) }

@Composable private fun MenuItem(label: String, onClick: () -> Unit) {
    Sx(label, size = 17.sp, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp))
}

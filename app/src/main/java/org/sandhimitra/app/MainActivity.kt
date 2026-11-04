package org.sandhimitra.app

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val IndigoPrimary = Color(0xFF394B78)
private val CopperPrimary = Color(0xFFD18B63)
private val CopperAccent = Color(0xFFC17B55)
private val LightBg = Color(0xFFF0F2F8)
private val LightCard = Color(0xFFFFFFFF)
private val DarkBg = Color(0xFF171E30)
private val DarkCard = Color(0xFF262F49)
private val Ink = Color(0xFF28334D)
private val LightText = Color(0xFFF0F2F8)
private val DarkAccent = Color(0xFFE6AE88)

enum class AppLanguage { ENGLISH, SANSKRIT, KANNADA }
enum class ThemeMode { LIGHT, DARK }
enum class LessonMode { LESSON, PRACTICE }
enum class Section(val id: Int) { INTRO(1), VOWEL(2), CONSONANT(3), VISARGA(4), PRAKRITIBHAVA(5), ENGINE(6) }

private data class Labels(val title: String, val subtitle: String)
private fun labels(section: Section, lang: AppLanguage): Labels = when (section) {
    Section.INTRO -> when(lang) { AppLanguage.ENGLISH -> Labels("Introduction", "Foundations of Sanskrit Sandhi"); AppLanguage.SANSKRIT -> Labels("प्रस्तावना", "सन्धेः सामान्यपरिचयः"); AppLanguage.KANNADA -> Labels("ಪರಿಚಯ", "ಸಂಸ್ಕೃತ ಸಂಧಿಯ ಮೂಲತತ್ತ್ವಗಳು") }
    Section.VOWEL -> when(lang) { AppLanguage.ENGLISH -> Labels("Vowel Sandhi", "स्वरसन्धिः"); AppLanguage.SANSKRIT -> Labels("स्वरसन्धिः", "स्वरवर्णानां सन्धिः"); AppLanguage.KANNADA -> Labels("ಸ್ವರಸಂಧಿ", "ಸ್ವರಗಳ ಸಂಧಿ") }
    Section.CONSONANT -> when(lang) { AppLanguage.ENGLISH -> Labels("Consonant Sandhi", "व्यञ्जनसन्धयः"); AppLanguage.SANSKRIT -> Labels("व्यञ्जनसन्धयः", "व्यञ्जनवर्णानां सन्धिः"); AppLanguage.KANNADA -> Labels("ವ್ಯಂಜನಸಂಧಿ", "ವ್ಯಂಜನಗಳ ಸಂಧಿ") }
    Section.VISARGA -> when(lang) { AppLanguage.ENGLISH -> Labels("Visarga Sandhi", "विसर्गसन्धयः"); AppLanguage.SANSKRIT -> Labels("विसर्गसन्धयः", "विसर्गस्य सन्धिः"); AppLanguage.KANNADA -> Labels("ವಿಸರ್ಗಸಂಧಿ", "ವಿಸರ್ಗದ ಸಂಧಿ") }
    Section.PRAKRITIBHAVA -> when(lang) { AppLanguage.ENGLISH -> Labels("Prakṛtibhāva Sandhi", "प्रकृतिभावः"); AppLanguage.SANSKRIT -> Labels("प्रकृतिभावः", "प्रकृतिस्थितिः"); AppLanguage.KANNADA -> Labels("ಪ್ರಕೃತಿಭಾವ ಸಂಧಿ", "ಮೂಲರೂಪದ ಸ್ಥಿತಿ") }
    Section.ENGINE -> when(lang) { AppLanguage.ENGLISH -> Labels("Sandhini Engine", "Sandhi Yojana · Sandhi Viccheda"); AppLanguage.SANSKRIT -> Labels("सन्धिनिर्णायकयन्त्रम्", "सन्धियोजनम् · सन्धिविच्छेदः"); AppLanguage.KANNADA -> Labels("ಸಂಧಿಮಿತ್ರ ಎಂಜಿನ್", "ಸಂಧಿಯೋಜನೆ · ಸಂಧಿವಿಚ್ಛೇದ") }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(23,30,48)
        window.navigationBarColor = android.graphics.Color.rgb(23,30,48)
        setContent { SandhimitraApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SandhimitraApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("sandhimitra_preferences", android.content.Context.MODE_PRIVATE) }
    var language by remember { mutableStateOf(runCatching { AppLanguage.valueOf(prefs.getString("language", "ENGLISH") ?: "ENGLISH") }.getOrDefault(AppLanguage.ENGLISH)) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark_theme", false)) }
    var fontSize by remember { mutableIntStateOf(prefs.getInt("font_size", 1).coerceIn(0, 2)) }
    var current by remember { mutableStateOf<Section?>(null) }
    var aboutPage by remember { mutableStateOf<String?>(null) }
    var mode by remember { mutableStateOf(LessonMode.LESSON) }
    var menu by remember { mutableStateOf(false) }
    var splash by remember { mutableStateOf(true) }
    LaunchedEffect(language) { prefs.edit().putString("language", language.name).apply() }
    LaunchedEffect(dark) { prefs.edit().putBoolean("dark_theme", dark).apply() }
    LaunchedEffect(fontSize) { prefs.edit().putInt("font_size", fontSize).apply() }
    LaunchedEffect(Unit) { delay(5_000); splash = false }
    val bg = if (dark) DarkBg else LightBg
    val card = if (dark) DarkCard else LightCard
    val text = if (dark) LightText else Ink
    val baseDensity = LocalDensity.current
    val selectedScale = listOf(0.90f, 1.00f, 1.15f)[fontSize]
    CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, baseDensity.fontScale * selectedScale)) {
    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary=CopperPrimary, secondary=DarkAccent, background=DarkBg, surface=DarkCard, onSurface=LightText) else lightColorScheme(primary=IndigoPrimary, secondary=CopperAccent, background=LightBg, surface=LightCard, onSurface=Ink)) {
        if (splash) { SplashScreen(dark) } else Scaffold(containerColor=bg, topBar={
            TopAppBar(
                title={ Column { Text("सन्धिमित्रम्", fontWeight=FontWeight.Bold, color=text); Text("Sandhimitra", fontSize=12.sp, color=text.copy(alpha=.75f)) } },
                navigationIcon={ if (current != null || aboutPage != null) IconButton(onClick={current=null;aboutPage=null}) { Icon(Icons.Default.ArrowBack, null, tint=text) } },
                actions={ Box { IconButton(onClick={menu=true}) { Icon(Icons.Default.Menu, null, tint=text) }; DropdownMenu(expanded=menu, onDismissRequest={menu=false}) {
                    DropdownMenuItem(text={Text(when(language){AppLanguage.ENGLISH->"Language";AppLanguage.SANSKRIT->"भाषा";AppLanguage.KANNADA->"ಭಾಷೆ"})}, onClick={})
                    AppLanguage.entries.forEach { l -> DropdownMenuItem(text={Text(when(l){AppLanguage.ENGLISH->"English";AppLanguage.SANSKRIT->"संस्कृतम्";AppLanguage.KANNADA->"ಕನ್ನಡ"})}, onClick={language=l;menu=false}) }
                    HorizontalDivider()
                    DropdownMenuItem(text={Text(when(language){AppLanguage.ENGLISH->"Theme";AppLanguage.SANSKRIT->"वर्णविन्यासः";AppLanguage.KANNADA->"ವಿನ್ಯಾಸ"})}, onClick={})
                    Row(Modifier.padding(horizontal=12.dp, vertical=4.dp), horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        listOf(when(language){AppLanguage.ENGLISH->"Light";AppLanguage.SANSKRIT->"प्रकाशः";AppLanguage.KANNADA->"ಬೆಳಕು"}, when(language){AppLanguage.ENGLISH->"Dark";AppLanguage.SANSKRIT->"अन्धकारः";AppLanguage.KANNADA->"ಕತ್ತಲೆ"}).forEachIndexed { i, label ->
                            FilterChip(selected=(dark == (i==1)), onClick={dark=(i==1);menu=false}, label={Text(label)})
                        }
                    }
                    DropdownMenuItem(text={Text(when(language){AppLanguage.ENGLISH->"Font size";AppLanguage.SANSKRIT->"अक्षरपरिमाणम्";AppLanguage.KANNADA->"ಅಕ್ಷರದ ಗಾತ್ರ"})}, onClick={})
                    Row(Modifier.padding(horizontal=12.dp, vertical=4.dp), horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        listOf("A−", "A", "A+").forEachIndexed { i, label ->
                            FilterChip(selected=fontSize==i, onClick={fontSize=i;menu=false}, label={Text(label)})
                        }
                    }
                    HorizontalDivider()
                    DropdownMenuItem(text={Text("About App")}, onClick={aboutPage="app";current=null;menu=false})
                    DropdownMenuItem(text={Text("About Dr. DVK")}, onClick={aboutPage="dvk";current=null;menu=false})
                    DropdownMenuItem(text={Text("Support Sandhimitra")}, onClick={aboutPage="support";current=null;menu=false})
                    DropdownMenuItem(text={Text("Feedback")}, onClick={aboutPage="feedback";current=null;menu=false})
                } } }
            , colors=TopAppBarDefaults.topAppBarColors(containerColor=bg))
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal=16.dp)) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (aboutPage != null) AboutScreen(aboutPage!!, text, card, context)
                    else if (current == null) HomeScreen(language, card, text, onOpen={current=it;mode=LessonMode.LESSON})
                    else if (current == Section.ENGINE) EngineScreen(language, card, text)
                    else SectionScreen(current!!, language, mode, onMode={mode=it}, card, text)
                }
                Footer(text)
            }
        }
    }
    }
}


@Composable
private fun AboutScreen(page:String, text:Color, card:Color, context:android.content.Context) {
    val title = when(page) {
        "app" -> "About Sandhimitra"
        "dvk" -> "About Dr. DVK"
        "support" -> "Support Sandhimitra"
        else -> "Feedback"
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(title, color=text, fontSize=23.sp, fontWeight=FontWeight.Bold, modifier=Modifier.padding(top=16.dp))
        when(page) {
            "app" -> Card(colors=CardDefaults.cardColors(containerColor=card)) {
                Text(
                    "Sandhimitra is a Sanskrit grammar learning and sandhi-analysis application for learners, teachers and researchers. It is designed to present the grammatical foundations of sandhi through structured lessons, Aṣṭādhyāyī references, derivations, examples, exceptions and guided practice.\\n\\nThe Sandhini Engine is intended to support both Sandhi Yojana (joining) and Sandhi Viccheda (analysis of joined forms). Its grammatical authority is a traceable, rule-based database. Where multiple analyses or optional forms are documented, they must be presented separately; unsupported inputs must not produce guessed results. Core learning and analysis facilities are designed for offline use.",
                    color=text, modifier=Modifier.padding(16.dp), lineHeight=23.sp
                )
            }
            "dvk" -> Card(colors=CardDefaults.cardColors(containerColor=card)) {
                Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("Dr. Diddigi Vamsi Krishna", color=text, fontWeight=FontWeight.Bold, fontSize=19.sp)
                    Text("Sanskrit Scholar | Manuscriptologist | Research Assistant, Oriental Research Institute, University of Mysore.", color=text, lineHeight=23.sp)
                    Text("The portrait will be added from the exact photograph supplied for this application. No substitute image is used.", color=text.copy(alpha=.75f))
                }
            }
            "support" -> Card(colors=CardDefaults.cardColors(containerColor=card)) {
                Text("Sandhimitra is being developed as an independent scholarly learning resource. Payment or donation options will be displayed only after the creator supplies and confirms the exact payment details. No payment details are currently configured.", color=text, modifier=Modifier.padding(16.dp), lineHeight=23.sp)
            }
            else -> {
                var subject by remember { mutableStateOf("") }
                var message by remember { mutableStateOf("") }
                Card(colors=CardDefaults.cardColors(containerColor=card)) {
                    Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value=subject,onValueChange={subject=it},label={Text("Subject")},modifier=Modifier.fillMaxWidth())
                        OutlinedTextField(value=message,onValueChange={message=it},label={Text("Message")},modifier=Modifier.fillMaxWidth(),minLines=4)
                        Button(onClick={
                            val intent=Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:dvkrishna92@gmail.com")).apply {
                                putExtra(Intent.EXTRA_SUBJECT, subject)
                                putExtra(Intent.EXTRA_TEXT, message)
                            }
                            runCatching { context.startActivity(intent) }
                        },modifier=Modifier.fillMaxWidth()) { Text("Send feedback by email") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SplashScreen(dark:Boolean) {
    val bg = if (dark) DarkBg else LightBg
    val fg = if (dark) LightText else Ink
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val started = System.currentTimeMillis()
        while (progress < 1f) {
            progress = ((System.currentTimeMillis() - started) / 5000f).coerceIn(0f, 1f)
            delay(16)
        }
    }
    Column(Modifier.fillMaxSize().background(bg).padding(24.dp), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.Center) {
        Image(painterResource(R.drawable.sandhimitra_mark), contentDescription="Sandhimitra manuscript and quill emblem", modifier=Modifier.fillMaxWidth().height(220.dp))
        Spacer(Modifier.height(22.dp))
        Text("सन्धिमित्रम्", fontSize=32.sp, fontWeight=FontWeight.Bold, color=fg)
        Text("Sandhimitra", fontSize=18.sp, color=fg.copy(alpha=.8f))
        Spacer(Modifier.height(32.dp))
        LinearProgressIndicator(progress={progress}, modifier=Modifier.fillMaxWidth(.72f), color=CopperPrimary, trackColor=fg.copy(alpha=.18f))
        Spacer(Modifier.height(24.dp))
        Text("Compiled and Created by", fontSize=11.sp, color=fg.copy(alpha=.7f))
        Text("©Dr. Diddigi Vamsi Krishna", fontSize=12.sp, color=fg, fontWeight=FontWeight.SemiBold)
    }
}

@Composable
private fun HomeScreen(lang: AppLanguage, card: Color, text: Color, onOpen:(Section)->Unit) {
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp), contentPadding=PaddingValues(top=12.dp,bottom=12.dp)) {
        item {
            Card(colors=CardDefaults.cardColors(containerColor=card), shape=RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.sandhimitra_mark), contentDescription="Sandhimitra manuscript and quill emblem", modifier=Modifier.height(104.dp).fillMaxWidth())
                    Text("सन्धिमित्रम्", fontSize=25.sp, fontWeight=FontWeight.Bold, color=text)
                }
            }
        }
        items(6) { i ->
            val section=Section.entries[i]; val l=labels(section,lang)
            Card(modifier=Modifier.fillMaxWidth().clickable { onOpen(section) }, colors=CardDefaults.cardColors(containerColor=card), shape=RoundedCornerShape(18.dp), elevation=CardDefaults.cardElevation(2.dp)) {
                Row(Modifier.padding(18.dp), verticalAlignment=Alignment.CenterVertically) {
                    Surface(color=if(section==Section.ENGINE) IndigoPrimary else CopperAccent.copy(alpha=.22f), shape=RoundedCornerShape(12.dp)) { Text("${i+1}", Modifier.padding(horizontal=14.dp, vertical=10.dp), color=if(section==Section.ENGINE) Color.White else text, fontWeight=FontWeight.Bold) }
                    Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(l.title, color=text, fontWeight=FontWeight.SemiBold, fontSize=17.sp); Text(l.subtitle, color=text.copy(alpha=.7f), fontSize=12.sp) }
                }
            }
        }
    }
}

@Composable
private fun SectionScreen(section: Section, lang: AppLanguage, mode: LessonMode, onMode:(LessonMode)->Unit, card:Color, text:Color) {
    val title=labels(section,lang)
    Column(Modifier.fillMaxSize()) {
        Text(title.title, color=text, fontSize=23.sp, fontWeight=FontWeight.Bold, modifier=Modifier.padding(top=16.dp,bottom=4.dp))
        Text(title.subtitle, color=text.copy(alpha=.7f), fontSize=13.sp)
        Spacer(Modifier.height(14.dp))
        if (section == Section.VOWEL || section == Section.CONSONANT || section == Section.VISARGA) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(selected=mode==LessonMode.LESSON,onClick={onMode(LessonMode.LESSON)},shape=SegmentedButtonDefaults.itemShape(0),label={Text(when(lang){AppLanguage.ENGLISH->"Lesson";AppLanguage.SANSKRIT->"पाठः";AppLanguage.KANNADA->"ಪಾಠ"})})
                SegmentedButton(selected=mode==LessonMode.PRACTICE,onClick={onMode(LessonMode.PRACTICE)},shape=SegmentedButtonDefaults.itemShape(1),label={Text(when(lang){AppLanguage.ENGLISH->"Practice";AppLanguage.SANSKRIT->"अभ्यासः";AppLanguage.KANNADA->"ಅಭ್ಯಾಸ"})})
            }
            Spacer(Modifier.height(12.dp))
            if (mode==LessonMode.LESSON) LessonContent(section, lang, card, text) else PracticeContent(section, lang, card, text)
        } else {
            LessonContent(section, lang, card, text)
        }
    }
}

@Composable
private fun LessonContent(section:Section, lang:AppLanguage, card:Color, text:Color) {
    val heading=when(lang){AppLanguage.ENGLISH->"Lesson overview";AppLanguage.SANSKRIT->"पाठसारः";AppLanguage.KANNADA->"ಪಾಠದ ಪರಿಚಯ"}
    val body=when(section){
        Section.INTRO -> when(lang){AppLanguage.ENGLISH->"Sandhi is the regulated interaction of sounds in close succession. In the Pāṇinian system, its operation is connected with Saṃhitā and governed by specific grammatical conditions. Sandhi operations may involve substitution (ādeśa), augmentation (āgama), or deletion (lopa).";AppLanguage.SANSKRIT->"वर्णानां सन्निकर्षे संहितायां च शास्त्रनियमेन यः वर्णविकारः प्रवर्तते सन्धिः। तस्य मुख्यानि कार्याणि आदेशः, आगमः, लोपश्च।";AppLanguage.KANNADA->"ವರ್ಣಗಳು ನಿಕಟವಾಗಿ ಮತ್ತು ವಿರಾಮವಿಲ್ಲದೆ ಉಚ್ಚರಿಸಲ್ಪಡುವ ಸಂಹಿತೆಯ ಸಂದರ್ಭದಲ್ಲಿ, ವ್ಯಾಕರಣನಿಯಮದಂತೆ ಉಂಟಾಗುವ ವರ್ಣವಿಕಾರವೇ ಸಂಧಿ. ಅದರ ಮುಖ್ಯ ಕಾರ್ಯಗಳು ಆದೇಶ, ಆಗಮ ಮತ್ತು ಲೋಪ."}
        Section.VOWEL -> when(lang){AppLanguage.ENGLISH->"Vowel Sandhi concerns the interaction of vowel sounds under prescribed conditions. The lesson sequence covers savarṇadīrgha, guṇa, vṛddhi, yaṇ, ayādi and the relevant pūrvarūpa and pararūpa operations. Each rule must be studied with its environment and exceptions.";AppLanguage.SANSKRIT->"स्वरवर्णयोः संहितायां शास्त्रविहितः विकारः स्वरसन्धिः। सवर्णदीर्घः, गुणः, वृद्धिः, यण्, अयादयः प्रमुखाः विषयाः।";AppLanguage.KANNADA->"ಸ್ವರಗಳ ಸಂಹಿತೆಯಲ್ಲಿ ನಿಯಮಾನುಸಾರ ಉಂಟಾಗುವ ಬದಲಾವಣೆಯೇ ಸ್ವರಸಂಧಿ. ಸವರ್ಣದೀರ್ಘ, ಗುಣ, ವೃದ್ಧಿ, ಯಣ್, ಅಯಾದಿ ಮೊದಲಾದ ವಿಧಿಗಳನ್ನು ಅವುಗಳ ನಿಮಿತ್ತ ಮತ್ತು ಅಪವಾದಗಳೊಂದಿಗೆ ಅಧ್ಯಯನ ಮಾಡಲಾಗುತ್ತದೆ."}
        Section.CONSONANT -> when(lang){AppLanguage.ENGLISH->"Consonant Sandhi examines changes involving consonants at grammatical boundaries. Topics include ścutva, ṣṭutva, jaśtva, cartva, nasal substitution, anusvāra and parasavarṇa, together with their governing conditions and rule interactions.";AppLanguage.SANSKRIT->"व्यञ्जनानां परस्परसन्निधौ तथा स्वरव्यञ्जनसन्निधौ प्रवर्तमानाः शास्त्रीयविधयः व्यञ्जनसन्धिप्रकरणे निरूप्यन्ते।";AppLanguage.KANNADA->"ವ್ಯಂಜನಗಳ ಪರಸ್ಪರ ಸಾನ್ನಿಧ್ಯದಲ್ಲಿ ಅಥವಾ ಸ್ವರ-ವ್ಯಂಜನಗಳ ಸಾನ್ನಿಧ್ಯದಲ್ಲಿ ನಡೆಯುವ ನಿಯಮಬದ್ಧ ಬದಲಾವಣೆಗಳನ್ನು ವ್ಯಂಜನಸಂಧಿಯಲ್ಲಿ ಅಧ್ಯಯನ ಮಾಡಲಾಗುತ್ತದೆ."}
        Section.VISARGA -> when(lang){AppLanguage.ENGLISH->"Visarga Sandhi studies the changes affecting visarga in the presence of following sounds. The analysis distinguishes the relevant substitutions, deletion and special phonetic realizations, including jihvāmūlīya and upadhmānīya where applicable.";AppLanguage.SANSKRIT->"विसर्गस्य परवर्णनिमित्तकाः आदेशलोपादयः विधयः विसर्गसन्धौ विविच्यन्ते।";AppLanguage.KANNADA->"ಮುಂದಿನ ವರ್ಣದ ನಿಮಿತ್ತದಿಂದ ವಿಸರ್ಗಕ್ಕೆ ಆಗುವ ಆದೇಶ, ಲೋಪ ಮತ್ತು ವಿಶೇಷ ಉಚ್ಚಾರಣಾ ರೂಪಗಳನ್ನು ವಿಸರ್ಗಸಂಧಿಯಲ್ಲಿ ಪರಿಶೀಲಿಸಲಾಗುತ್ತದೆ."}
        Section.PRAKRITIBHAVA -> when(lang){AppLanguage.ENGLISH->"Prakṛtibhāva concerns cases in which a form remains in its original state despite a context in which a change might otherwise be considered. Its application must be established from the relevant grammatical rule and context.";AppLanguage.SANSKRIT->"सन्धिनिमित्ते सति अपि यत्र प्रकृतिरूपस्य अवस्थानं विधीयते स प्रकृतिभावः।";AppLanguage.KANNADA->"ಸಂಧಿಗೆ ಅನುಕೂಲವಾದ ಸಂದರ್ಭವಿದ್ದರೂ ನಿರ್ದಿಷ್ಟ ನಿಯಮದಂತೆ ಮೂಲರೂಪವೇ ಉಳಿಯುವ ಸ್ಥಿತಿಯನ್ನು ಪ್ರಕೃತಿಭಾವವೆಂದು ಕರೆಯಲಾಗುತ್ತದೆ."}
        else -> ""
    }
    LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item { Card(colors=CardDefaults.cardColors(containerColor=card)) { Column(Modifier.padding(16.dp)) { Text(heading,color=text,fontWeight=FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text(body,color=text,lineHeight=23.sp) } } }
        item { Card(colors=CardDefaults.cardColors(containerColor=card)) { Column(Modifier.padding(16.dp)) { Text(when(lang){AppLanguage.ENGLISH->"Foundational sūtra";AppLanguage.SANSKRIT->"मूलसूत्रम्";AppLanguage.KANNADA->"ಮೂಲಸೂತ್ರ"},color=text,fontWeight=FontWeight.Bold); Text("परः सन्निकर्षः संहिता।  (Aṣṭādhyāyī 1.4.109)",color=text, modifier=Modifier.padding(top=8.dp)); Text(when(lang){AppLanguage.ENGLISH->"Saṃhitā is the closest proximity of sounds.";AppLanguage.SANSKRIT->"वर्णानाम् अतिशयितः सन्निकर्षः संहिता।";AppLanguage.KANNADA->"ವರ್ಣಗಳ ಅತ್ಯಂತ ನಿಕಟ ಸಾನ್ನಿಧ್ಯವೇ ಸಂಹಿತೆ."},color=text.copy(alpha=.75f),modifier=Modifier.padding(top=6.dp)) } } }
    }
}

@Composable
private fun PracticeContent(section:Section, lang:AppLanguage, card:Color, text:Color) {
    val prompt=when(lang){AppLanguage.ENGLISH->"Practice material is being aligned with the verified source exercises. No unverified answer will be displayed.";AppLanguage.SANSKRIT->"प्रमाणिताभ्याससामग्री सम्पाद्यते। अप्रमाणितम् उत्तरं न प्रदर्श्यते।";AppLanguage.KANNADA->"ಪರಿಶೀಲಿತ ಮೂಲ ಅಭ್ಯಾಸಗಳೊಂದಿಗೆ ಪ್ರಶ್ನೆಗಳನ್ನು ಹೊಂದಿಸಲಾಗುತ್ತಿದೆ. ಪರಿಶೀಲಿಸದ ಉತ್ತರವನ್ನು ತೋರಿಸಲಾಗುವುದಿಲ್ಲ."}
    Card(colors=CardDefaults.cardColors(containerColor=card)) { Column(Modifier.padding(16.dp)) { Text(prompt,color=text,lineHeight=23.sp) } }
}

@Composable
private fun EngineScreen(lang:AppLanguage, card:Color, text:Color) {
    var direction by remember { mutableStateOf(0) }
    var inputA by remember { mutableStateOf("") }; var inputB by remember { mutableStateOf("") }; var result by remember { mutableStateOf<String?>(null) }; var dialog by remember { mutableStateOf(false) }
    var insufficientInput by remember { mutableStateOf(false) }
    val title=labels(Section.ENGINE,lang)
    Column(Modifier.fillMaxSize().padding(top=14.dp)) {
        Text(title.title,color=text,fontSize=23.sp,fontWeight=FontWeight.Bold)
        Text(when(lang){AppLanguage.ENGLISH->"Rule-based analysis · no fabricated forms";AppLanguage.SANSKRIT->"सूत्राधारितं विश्लेषणम् · कल्पितरूपाणि न";AppLanguage.KANNADA->"ಸೂತ್ರಾಧಾರಿತ ವಿಶ್ಲೇಷಣೆ · ಕಲ್ಪಿತ ರೂಪಗಳಿಲ್ಲ"},color=text.copy(alpha=.7f),fontSize=12.sp)
        Spacer(Modifier.height(14.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = direction == 0,
                onClick = { direction = 0; result = null },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text(when(lang){AppLanguage.ENGLISH->"Yojana";AppLanguage.SANSKRIT->"योजनम्";AppLanguage.KANNADA->"ಯೋಜನೆ"})
            }
            SegmentedButton(
                selected = direction == 1,
                onClick = { direction = 1; result = null },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text(when(lang){AppLanguage.ENGLISH->"Viccheda";AppLanguage.SANSKRIT->"विच्छेदः";AppLanguage.KANNADA->"ವಿಚ್ಛೇದ"})
            }
        }
        Spacer(Modifier.height(12.dp))
        if(direction==0){ OutlinedTextField(value=inputA,onValueChange={inputA=it;result=null},label={Text(when(lang){AppLanguage.ENGLISH->"First word";AppLanguage.SANSKRIT->"प्रथमं पदम्";AppLanguage.KANNADA->"ಮೊದಲ ಪದ"})},modifier=Modifier.fillMaxWidth()); OutlinedTextField(value=inputB,onValueChange={inputB=it;result=null},label={Text(when(lang){AppLanguage.ENGLISH->"Second word";AppLanguage.SANSKRIT->"द्वितीयं पदम्";AppLanguage.KANNADA->"ಎರಡನೇ ಪದ"})},modifier=Modifier.fillMaxWidth()) }
        else OutlinedTextField(value=inputA,onValueChange={inputA=it;result=null},label={Text(when(lang){AppLanguage.ENGLISH->"Combined form";AppLanguage.SANSKRIT->"संहितारूपम्";AppLanguage.KANNADA->"ಸಂಧಿರೂಪ"})},modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Button(onClick={
            val key=if(direction==0) "${inputA.trim()}+${inputB.trim()}" else inputA.trim()
            insufficientInput = if(direction==0) inputA.isBlank() || inputB.isBlank() else inputA.isBlank()
            result=if(insufficientInput) null else VerifiedRules.lookup(key,direction)
            if(result==null) dialog=true
        },modifier=Modifier.fillMaxWidth()) { Text(when(lang){AppLanguage.ENGLISH->"Analyse";AppLanguage.SANSKRIT->"विश्लेषयतु";AppLanguage.KANNADA->"ವಿಶ್ಲೇಷಿಸಿ"}) }
        result?.let { Card(Modifier.fillMaxWidth().padding(top=12.dp),colors=CardDefaults.cardColors(containerColor=card)) { Text(it,color=text,modifier=Modifier.padding(16.dp),lineHeight=23.sp) } }
        if(dialog) AlertDialog(
            onDismissRequest={dialog=false},
            title={Text(when(lang){
                AppLanguage.ENGLISH->if(insufficientInput) "Insufficient Input" else "No Verified Analysis Available";
                AppLanguage.SANSKRIT->if(insufficientInput) "अपर्याप्तः निवेशः" else "प्रमाणितं विश्लेषणं नोपलब्धम्";
                AppLanguage.KANNADA->if(insufficientInput) "ಅಪೂರ್ಣ ನಮೂದು" else "ಪರಿಶೀಲಿತ ವಿಶ್ಲೇಷಣೆ ಲಭ್ಯವಿಲ್ಲ"
            })},
            text={Text(when(lang){
                AppLanguage.ENGLISH->if(insufficientInput) "Enter all required word components and try again." else "The current verified rule database does not contain an analysis for this input. No form has been generated. This does not by itself establish grammatical impossibility.";
                AppLanguage.SANSKRIT->if(insufficientInput) "आवश्यकानि सर्वाणि पदानि निवेश्य पुनः प्रयत्नं कुर्वन्तु।" else "अस्य निवेशस्य विश्लेषणं वर्तमानप्रमाणितनियमसञ्चये नास्ति। किमपि रूपं न निष्पादितम्। एतेन व्याकरणासम्भवः स्वतः न सिद्ध्यति।";
                AppLanguage.KANNADA->if(insufficientInput) "ಅಗತ್ಯವಿರುವ ಎಲ್ಲ ಪದಭಾಗಗಳನ್ನು ನಮೂದಿಸಿ ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ." else "ಈ ನಮೂದಿಗೆ ಪ್ರಸ್ತುತ ಪರಿಶೀಲಿತ ನಿಯಮಸಂಚಯದಲ್ಲಿ ವಿಶ್ಲೇಷಣೆ ಇಲ್ಲ. ಯಾವುದೇ ರೂಪವನ್ನು ರಚಿಸಲಾಗಿಲ್ಲ. ಇದರಿಂದ ವ್ಯಾಕರಣಾತ್ಮಕ ಅಸಾಧ್ಯತೆ ಸಾಬೀತಾಗುವುದಿಲ್ಲ."
            })},
            confirmButton={TextButton(onClick={dialog=false}){Text(when(lang){AppLanguage.ENGLISH->"OK";AppLanguage.SANSKRIT->"अस्तु";AppLanguage.KANNADA->"ಸರಿ"})}}
        )
    }
}

private object VerifiedRules {
    // No transformations are enabled until each rule record is reviewed against
    // the supplied source materials and entered into the verified rule registry.
    fun lookup(key:String,direction:Int):String? = null
}

@Composable
private fun Footer(text:Color) {
    Column(Modifier.fillMaxWidth().padding(vertical=8.dp), horizontalAlignment=Alignment.CenterHorizontally) {
        Text("Compiled and Created by",fontSize=10.sp,color=text.copy(alpha=.65f))
        Text("©Dr. Diddigi Vamsi Krishna",fontSize=11.sp,color=text,fontWeight=FontWeight.SemiBold)
    }
}

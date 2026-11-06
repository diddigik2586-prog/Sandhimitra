package org.sandhimitra.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.sandhimitra.app.R
import org.sandhimitra.app.SandhimitraApp
import org.sandhimitra.app.engine.Rules

private fun L(sa: String, kn: String, en: String, lang: String) = when (lang) { "kn" -> kn; "en" -> en; else -> sa }

@Composable
fun AboutScreen(app: SandhimitraApp, onMenu: () -> Unit, onBack: () -> Unit) {
    val t = LocalTokens.current
    Page(onMenu, onBack, "About the App") { CentrePage {
        Sx("Explore Sanskrit Sandhi in a simple and engaging way.", size = 18.sp, weight = FontWeight.SemiBold, color = t.primary, modifier = Modifier.padding(bottom = 10.dp))
        Sx("Learn the different types of Sandhi through clear explanations, examples, lessons and practice activities. Understand how words combine and how Sandhi forms can be separated.")
        SectionTitle("What You Can Explore")
        listOf("Vowel Sandhi", "Consonant Sandhi", "Visarga Sandhi", "Prakṛtibhāva Sandhi", "Lessons and practice activities", "Sandhātrī — explore Sandhi formation and word separation",
            "Learn in Sanskrit, Kannada or English", "Continue learning with your progress saved on your device").forEach { Sx(it, modifier = Modifier.padding(bottom = 4.dp)) }
        Sx("The app is designed to make learning and exploring Sanskrit Sandhi accessible and convenient.", modifier = Modifier.padding(top = 12.dp))
        SectionTitle("Font Credits")
        listOf("Sanskrit: Tiro Devanagari Hindi", "Kannada: Noto Sans Kannada", "English: Andika", "All fonts are used under the SIL Open Font License 1.1.").forEach { Sx(it, modifier = Modifier.padding(bottom = 2.dp)) }
    } }
}

@Composable
fun DvkScreen(onMenu: () -> Unit, onBack: () -> Unit) {
    val lang = LocalLang.current; val t = LocalTokens.current
    Page(onMenu, onBack, tr("about_dvk")) { CentrePage {
        Image(painterResource(R.drawable.dvk_photograph), contentDescription = "Dr. Diddigi Vamsi Krishna", contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)))
        Sx(L("डॉ. दिड्डिगि वंशी कृष्णः", "ಡಾ. ದಿಡ್ಡಿಗಿ ವಂಶಿ ಕೃಷ್ಣ", "Dr. Diddigi Vamsi Krishna", lang), size = 22.sp, weight = FontWeight.Bold, color = t.primary, align = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
        Sx(L("संस्कृतविद्वान् | हस्तप्रतिशास्त्रज्ञः | संशोधकः", "ಸಂಸ್ಕೃತ ವಿದ್ವಾಂಸ | ಹಸ್ತಪ್ರತಿಶಾಸ್ತ್ರಜ್ಞ | ಸಂಶೋಧಕ", "Sanskrit Scholar | Manuscriptologist | Researcher", lang), size = 15.sp, weight = FontWeight.SemiBold, align = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        listOf(
            L("डॉ. दिड्डिगि वंशी कृष्णः संस्कृतग्रन्थपाठविद्यायां, हस्तप्रतिसंरक्षणे, समालोचनात्मकसम्पादने, सूचीकरणे, भारतीयलिपिविद्यायां च विशेषज्ञः संस्कृतविद्वान् हस्तप्रतिशास्त्रज्ञश्च। मैसूरुविश्वविद्यालयात् संस्कृते पीएच्.डी. उपाधिं प्राप्तवान्; निरुक्तपरम्परा तस्य संशोधनविषयः।",
              "ಡಾ. ದಿಡ್ಡಿಗಿ ವಂಶಿ ಕೃಷ್ಣ ಅವರು ಸಂಸ್ಕೃತ ಗ್ರಂಥಪಾಠ ಅಧ್ಯಯನ, ಹಸ್ತಪ್ರತಿ ಸಂರಕ್ಷಣೆ, ವಿಮರ್ಶಾತ್ಮಕ ಸಂಪಾದನೆ, ಸೂಚೀಕರಣ ಮತ್ತು ಭಾರತೀಯ ಲಿಪಿ ಅಧ್ಯಯನಗಳಲ್ಲಿ ವಿಶೇಷಜ್ಞರಾದ ಸಂಸ್ಕೃತ ವಿದ್ವಾಂಸರು ಮತ್ತು ಹಸ್ತಪ್ರತಿಶಾಸ್ತ್ರಜ್ಞರು. ಮೈಸೂರು ವಿಶ್ವವಿದ್ಯಾನಿಲಯದಿಂದ ಸಂಸ್ಕೃತದಲ್ಲಿ ಪಿಎಚ್.ಡಿ. ಪಡೆದಿದ್ದು, ನಿರುಕ್ತ ಪರಂಪರೆ ಅವರ ಸಂಶೋಧನೆಯ ವಿಷಯ.",
              "Dr. Diddigi Vamsi Krishna is a Sanskrit scholar and manuscriptologist specialising in Sanskrit textual studies, manuscript preservation, critical editing, cataloguing and Indic script studies. He holds a Ph.D. in Sanskrit from the University of Mysore, with research focused on the Nirukta tradition.", lang),
            L("सः मैसूरुविश्वविद्यालयस्य प्राच्यसंशोधनालये (ORI) संशोधनसहायकः (श्रेणी-II) रूपेण कार्यं करोति; हस्तप्रतिसंशोधनं, विद्वत्सम्पादनं, अभिलेखप्रलेखनं, प्रकाशनं च तस्य कार्यक्षेत्रम्। ORI डिजिटल्-पुस्तक-परियोजनायाः समन्वयकः सन् अङ्कीकरणे, अङ्कीयसंरक्षणे, विद्वत्सङ्ग्रहाणां प्रलेखने च संलग्नः।",
              "ಅವರು ಮೈಸೂರು ವಿಶ್ವವಿದ್ಯಾನಿಲಯದ ಪ್ರಾಚ್ಯ ಸಂಶೋಧನಾ ಸಂಸ್ಥೆ (ORI) ಯಲ್ಲಿ ಸಂಶೋಧನಾ ಸಹಾಯಕ (ಶ್ರೇಣಿ-II) ಆಗಿ ಸೇವೆ ಸಲ್ಲಿಸುತ್ತಿದ್ದು, ಹಸ್ತಪ್ರತಿ ಸಂಶೋಧನೆ, ವಿದ್ವತ್ ಸಂಪಾದನೆ, ಆರ್ಕೈವ್ ದಾಖಲೀಕರಣ ಮತ್ತು ಪ್ರಕಟಣೆಯಲ್ಲಿ ಕೊಡುಗೆ ನೀಡುತ್ತಿದ್ದಾರೆ. ORI ಡಿಜಿಟಲ್ ಪುಸ್ತಕ ಯೋಜನೆಯ ಸಂಯೋಜಕರಾಗಿ ಡಿಜಿಟಲೀಕರಣ, ಡಿಜಿಟಲ್ ಸಂರಕ್ಷಣೆ ಮತ್ತು ವಿದ್ವತ್ ಸಂಗ್ರಹಗಳ ದಾಖಲೀಕರಣದಲ್ಲಿ ತೊಡಗಿದ್ದಾರೆ.",
              "He serves as a Research Assistant Grade II at the Oriental Research Institute (ORI), University of Mysore, Mysuru, contributing to manuscript research, scholarly editing, archival documentation and publication. As Coordinator of the ORI Digital Books Project, he is involved in digitisation, digital preservation and the documentation of scholarly collections.", lang),
            L("तस्य विद्वत्प्रवृत्तिषु संस्कृतग्रन्थपाठविद्या, बहुभाषिकहस्तप्रतयः, लिप्यन्तरणं, पारम्परिकज्ञानव्यवस्थाः, अङ्कीयधरोहरसंरक्षणं च अन्तर्भवन्ति; सम्पादनकार्येषु, शैक्षणिकप्रकाशनेषु, हस्तप्रतिशास्त्रप्रशिक्षणे च तेन योगदानं कृतम्।",
              "ಅವರ ವಿದ್ವತ್ ಆಸಕ್ತಿಗಳಲ್ಲಿ ಸಂಸ್ಕೃತ ಗ್ರಂಥಪಾಠ ವಿದ್ವತ್ತು, ಬಹುಭಾಷಾ ಹಸ್ತಪ್ರತಿಗಳು, ಲಿಪ್ಯಂತರ, ಸಾಂಪ್ರದಾಯಿಕ ಜ್ಞಾನ ವ್ಯವಸ್ಥೆಗಳು ಮತ್ತು ಡಿಜಿಟಲ್ ಪರಂಪರೆ ಸಂರಕ್ಷಣೆ ಸೇರಿವೆ. ಸಂಪಾದನಾ ಚಟುವಟಿಕೆಗಳು, ಶೈಕ್ಷಣಿಕ ಪ್ರಕಟಣೆಗಳು ಮತ್ತು ಹಸ್ತಪ್ರತಿಶಾಸ್ತ್ರ ತರಬೇತಿಯಲ್ಲೂ ಕೊಡುಗೆ ನೀಡಿದ್ದಾರೆ.",
              "His scholarly interests include Sanskrit textual scholarship, multilingual manuscripts, transliteration, traditional knowledge systems and digital heritage preservation. He has also contributed to editorial activities, academic publications and manuscriptology training.", lang),
            L("डॉ. कृष्णः मैसूरुनगरस्य शिंशुमार-शोध-प्रतिष्ठानस्य संस्थापकनिदेशकः; बेङ्गलूरुनगरस्य आदिशिला-प्रकाशनस्य गौरवमुख्यसम्पादकरूपेण, जाह्नवी-संस्कृत-ई-पत्रिकायाः समीक्षकरूपेण च सेवां कृतवान्।",
              "ಡಾ. ಕೃಷ್ಣ ಅವರು ಮೈಸೂರಿನ ಶಿಂಶುಮಾರ ಶೋಧ ಪ್ರತಿಷ್ಠಾನದ ಸಂಸ್ಥಾಪಕ ನಿರ್ದೇಶಕರು; ಬೆಂಗಳೂರಿನ ಆದಿಶಿಲಾ ಪ್ರಕಾಶನದ ಗೌರವ ಮುಖ್ಯ ಸಂಪಾದಕರಾಗಿ ಮತ್ತು ಜಾಹ್ನವೀ ಸಂಸ್ಕೃತ ಇ-ಜರ್ನಲ್‌ನ ವಿಮರ್ಶಕರಾಗಿಯೂ ಸೇವೆ ಸಲ್ಲಿಸಿದ್ದಾರೆ.",
              "Dr. Krishna is the Founding Director of Shimshumara Shodha Pratishthanam, Mysuru, and has served as Honorary Chief Editor at Adishila Publication, Bengaluru. He has also served as a reviewer for the Jahnavi Sanskrit e-Journal.", lang),
            L("कर्नाटक-संस्कृत-विश्वविद्यालयात् तस्य संस्कृतकृतेः ‘निरुक्तचिन्तनम्’ इत्यस्य कृते ग्रन्थपुरस्कारः (२०२०) प्राप्तः; २०१४ तमे वर्षे ‘संस्कृत-सुप्तनिधि-आचार्य’ इति सम्मानः अपि।",
              "ಕರ್ನಾಟಕ ಸಂಸ್ಕೃತ ವಿಶ್ವವಿದ್ಯಾನಿಲಯದಿಂದ ಅವರ ಸಂಸ್ಕೃತ ಕೃತಿ ‘ನಿರುಕ್ತಚಿಂತನಂ’ ಗೆ ಗ್ರಂಥ ಪುರಸ್ಕಾರ (2020) ಲಭಿಸಿದೆ; 2014ರಲ್ಲಿ ‘ಸಂಸ್ಕೃತ-ಸುಪ್ತನಿಧಿ-ಆಚಾರ್ಯ’ ಮನ್ನಣೆಯೂ ದೊರೆತಿದೆ.",
              "He received the Grantha Puraskara (2020) from Karnataka Samskrit University for his Sanskrit work Niruktacintanam and the Saṃskṛta-Suptanidhi-Ācārya recognition in 2014.", lang)
        ).forEach { Sx(it, modifier = Modifier.padding(bottom = 10.dp)) }
        if (lang != "en") Sx(L("टिप्पणी: संस्कृत-कन्नडरूपे दत्तं जीवनवृत्तं प्रदत्तस्य आङ्ग्लपाठस्य अनुवादः।", "ಸೂಚನೆ: ಕನ್ನಡ/ಸಂಸ್ಕೃತ ರೂಪದ ಪರಿಚಯವು ಒದಗಿಸಿದ ಇಂಗ್ಲಿಷ್ ಪಠ್ಯದ ಅನುವಾದ.", "", lang), size = 12.sp, color = t.muted, italic = true)
    } }
}

@Composable
fun SupportScreen(onMenu: () -> Unit, onBack: () -> Unit) {
    val t = LocalTokens.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val upi = "8970500450@kotakbank"
    Page(onMenu, onBack, tr("support")) {
        CentrePage {
            Sx(tr("support_intro"), modifier = Modifier.padding(bottom = 12.dp))
            CardBox(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Sx(tr("scan_upi"), size = 16.sp, weight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    Image(painterResource(R.drawable.support_qr), contentDescription = "UPI QR", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth(0.72f).aspectRatio(1f).clip(RoundedCornerShape(10.dp)))
                    Spacer(Modifier.height(10.dp))
                    Sx("DIDDIGI VAMSI KRISHNA", weight = FontWeight.Bold)
                    Sx(tr("upi_id"), size = 13.sp, color = t.muted)
                    Sx(upi, size = 18.sp, weight = FontWeight.SemiBold, color = t.primary)
                    PrimaryButton(tr("copy_upi"), {
                        (ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("UPI", upi))
                        android.widget.Toast.makeText(ctx, S.t("copied", (ctx.applicationContext as org.sandhimitra.app.SandhimitraApp).prefs.lang), android.widget.Toast.LENGTH_SHORT).show()
                    }, Modifier.padding(top = 10.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            var bankOpen by rememberSaveable { mutableStateOf(false) }
            CardBox(Modifier.fillMaxWidth(), onClick = { bankOpen = !bankOpen }) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Sx(tr("bank_details") + if (bankOpen) "  ▴" else "  ▾", size = 16.sp, weight = FontWeight.SemiBold, color = t.primary)
                    if (bankOpen) listOf("acc_holder" to "DIDDIGI VAMSI KRISHNA", "bank" to "Kotak Mahindra Bank", "branch" to "MYSORE", "acc_no" to "1348857820", "ifsc" to "KKBK0000425").forEach { (k, v) ->
                        Sx(tr(k), size = 13.sp, color = t.muted, modifier = Modifier.padding(top = 8.dp)); Sx(v, size = 17.sp, weight = FontWeight.SemiBold)
                    }
                }
            }
            Sx(tr("support_note"), size = 13.sp, color = t.muted, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
fun PrivacyScreen(onMenu: () -> Unit, onBack: () -> Unit) {
    val lang = LocalLang.current
    Page(onMenu, onBack, tr("privacy")) { CentrePage {
        data class P(val h: String, val b: String)
        listOf(
            P(L("स्थानीयं प्रक्रियणम्", "ಸ್ಥಳೀಯ ಸಂಸ್ಕರಣೆ", "Local processing", lang), L("पाठाः, अभ्यासाः, सन्धात्री, शब्दकोशान्वेषणं च भवतः यन्त्रे एव प्रवर्तन्ते। भवता लिखिताः संस्कृतपदाः अन्यत्र न प्रेष्यन्ते।", "ಪಾಠ, ಅಭ್ಯಾಸ, ಸಂಧಾತ್ರೀ ಮತ್ತು ಶಬ್ದಕೋಶ ಹುಡುಕಾಟ ನಿಮ್ಮ ಸಾಧನದಲ್ಲೇ ನಡೆಯುತ್ತವೆ. ನೀವು ಬರೆದ ಸಂಸ್ಕೃತ ಪದಗಳನ್ನು ಬೇರೆಲ್ಲೂ ಕಳುಹಿಸಲಾಗುವುದಿಲ್ಲ.", "Lessons, practice, Sandhātrī and lexicon lookup run entirely on your device. The Sanskrit text you type is never sent anywhere.", lang)),
            P(L("संग्रहः", "ಸಂಗ್ರಹ", "What is stored", lang), L("भाषा, वर्णसंयोजनं, अक्षरमानं, पाठपूर्णता, अभ्यासफलानि, पुस्तकचिह्नानि, सन्धात्री-इतिहासः च केवलं अनुप्रयोगस्य खाजगी-स्मृतौ रक्ष्यन्ते। अनुप्रयोगं निष्कास्य तानि विनश्यन्ति।", "ಭಾಷೆ, ಥೀಮ್, ಅಕ್ಷರ ಗಾತ್ರ, ಪಾಠ ಪೂರ್ಣತೆ, ಅಭ್ಯಾಸ ಫಲಿತಾಂಶ, ಬುಕ್‌ಮಾರ್ಕ್ ಮತ್ತು ಸಂಧಾತ್ರೀ ಇತಿಹಾಸ ಆ್ಯಪ್‌ನ ಖಾಸಗಿ ಸಂಗ್ರಹದಲ್ಲಿ ಮಾತ್ರ ಉಳಿಯುತ್ತವೆ. ಆ್ಯಪ್ ಅಳಿಸಿದರೆ ಇವು ಅಳಿಯುತ್ತವೆ.", "Your language, theme, font size, lesson completion, practice results, bookmarks and Sandhātrī history are stored only in the app's private storage on your device and are removed when the app is uninstalled.", lang)),
            P(L("अन्तर्जालं विना", "ಇಂಟರ್ನೆಟ್ ಇಲ್ಲದೆ", "Offline operation", lang), L("अयम् अनुप्रयोगः अन्तर्जाल-अनुमतिं न याचते; अतः स्वयं किमपि जालद्वारा न प्रेषयति। खाता, विज्ञापनं, विश्लेषणं, ट्रैकिङ्ग्, शुल्कसेवा वा नास्ति; व्यक्तिगतदत्तांशः न विक्रीयते।", "ಈ ಆ್ಯಪ್ ಇಂಟರ್ನೆಟ್ ಅನುಮತಿಯನ್ನು ಕೇಳುವುದಿಲ್ಲ; ಆದ್ದರಿಂದ ತಾನಾಗಿ ಏನನ್ನೂ ನೆಟ್‌ವರ್ಕ್ ಮೂಲಕ ಕಳುಹಿಸುವುದಿಲ್ಲ. ಖಾತೆ, ಜಾಹೀರಾತು, ವಿಶ್ಲೇಷಣೆ, ಟ್ರ್ಯಾಕಿಂಗ್ ಅಥವಾ ಪಾವತಿ ಸೇವೆ ಇಲ್ಲ; ವೈಯಕ್ತಿಕ ದತ್ತಾಂಶ ಮಾರಾಟವಾಗುವುದಿಲ್ಲ.", "The app does not request the internet permission, so it cannot send anything over the network by itself. There is no account, advertising, analytics, tracking or paid service, and no personal data is sold.", lang)),
            P(L("अभिप्रायः", "ಅಭಿಪ್ರಾಯ", "Feedback", lang), L("‘अभिप्रायः’ इति चिनोति चेत् भवतः ईमेल्-अनुप्रयोगः पूर्वपूरितेन प्राप्तकर्त्रा विषयेण च उद्घाट्यते। प्रेषणं भवता एव स्पष्टतया क्रियते; अनुप्रयोगः किमपि स्वयं न प्रेषयति।", "‘ಅಭಿಪ್ರಾಯ’ ಆಯ್ಕೆ ಮಾಡಿದರೆ ನಿಮ್ಮ ಇಮೇಲ್ ಆ್ಯಪ್ ಮೊದಲೇ ತುಂಬಿದ ಸ್ವೀಕರ್ತ ಮತ್ತು ವಿಷಯದೊಂದಿಗೆ ತೆರೆಯುತ್ತದೆ. ಕಳುಹಿಸುವುದು ನೀವೇ ಸ್ಪಷ್ಟವಾಗಿ ಮಾಡಬೇಕು; ಆ್ಯಪ್ ಏನನ್ನೂ ತಾನಾಗಿ ಕಳುಹಿಸುವುದಿಲ್ಲ.", "Choosing Feedback opens your own email app with the recipient and subject prefilled. You press Send yourself; the app sends nothing automatically. Your email provider then handles that message under its own policy.", lang)),
            P(L("साझाकरणं साहाय्यं च", "ಹಂಚಿಕೆ ಮತ್ತು ಬೆಂಬಲ", "Sharing and support", lang), L("प्रतिलिपिः, शेयर्, PDF च भवता प्रवर्तितेषु एव कार्येषु भवन्ति। साहाय्यपृष्ठे यथाप्रदत्तं QR-चित्रं दर्शितम्; अनुप्रयोगः भुगतानं न प्रक्रियते।", "ನಕಲು, ಹಂಚಿಕೆ ಮತ್ತು PDF ನೀವು ಪ್ರಾರಂಭಿಸಿದಾಗ ಮಾತ್ರ ನಡೆಯುತ್ತವೆ. ಬೆಂಬಲ ಪುಟದಲ್ಲಿ QR ಚಿತ್ರವನ್ನು ಒದಗಿಸಿದಂತೆಯೇ ತೋರಿಸಲಾಗಿದೆ; ಆ್ಯಪ್ ಪಾವತಿ ಸಂಸ್ಕರಿಸುವುದಿಲ್ಲ.", "Copy, share and PDF export happen only when you start them. The Support page shows the supplied QR image; the app does not process payments.", lang))
        ).forEach { p ->
            SectionTitle(p.h); Sx(p.b)
        }
    } }
}

/** Centres all text and images of an information page. */
@Composable
fun CentrePage(content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalAlign provides TextAlign.Center) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}

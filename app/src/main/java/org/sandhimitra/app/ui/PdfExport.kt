package org.sandhimitra.app.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import org.sandhimitra.app.R
import java.io.File

/** Local PDF export (user initiated). The exact wordmark PNG heads the page; text runs use the script-appropriate bundled font. */
object PdfExport {
    fun write(ctx: Context, title: String, lines: List<String>): File {
        fun tf(id: Int): Typeface = ResourcesCompat.getFont(ctx, id) ?: Typeface.DEFAULT
        val faces = listOf(tf(R.font.andika_regular), tf(R.font.kohinoor_deva_regular), tf(R.font.kannada_mn))
        val pw = 595; val ph = 842; val margin = 40f
        val doc = PdfDocument()
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pw, ph, 1).create()); var canvas: Canvas = page.canvas
        var pageNo = 1
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f; color = 0xFF28334D.toInt() }
        var y: Float
        fun header(c: Canvas): Float {
            val bmp = BitmapFactory.decodeResource(ctx.resources, R.drawable.sanskrit_wordmark_transparent)
            val h = 44f; val w = h * bmp.width / bmp.height
            c.drawBitmap(bmp, null, android.graphics.RectF(margin, 30f, margin + w, 30f + h), Paint(Paint.FILTER_BITMAP_FLAG))
            return 30f + h + 26f
        }
        y = header(canvas)
        fun drawRuns(c: Canvas, text: String, x0: Float, yy: Float) {
            var x = x0; var i = 0
            while (i < text.length) {
                val k = Fonts.kindOf(text[i]).let { if (it == 0 && text[i] == ' ') -1 else it }
                var j = i + 1
                while (j < text.length && (Fonts.kindOf(text[j]).let { if (it == 0 && text[j] == ' ') -1 else it }) == k) j++
                paint.typeface = faces[if (k < 0) 0 else k]
                val seg = text.substring(i, j); c.drawText(seg, x, yy, paint); x += paint.measureText(seg); i = j
            }
        }
        fun wrap(text: String): List<String> {
            val out = mutableListOf<String>(); var cur = StringBuilder()
            fun width(s: String): Float { var x = 0f; var i = 0
                while (i < s.length) { val k = Fonts.kindOf(s[i]); var j = i + 1; while (j < s.length && Fonts.kindOf(s[j]) == k) j++; paint.typeface = faces[k]; x += paint.measureText(s.substring(i, j)); i = j }; return x }
            for (w in text.split(" ")) { val cand = if (cur.isEmpty()) w else "$cur $w"; if (width(cand) > pw - 2 * margin && cur.isNotEmpty()) { out += cur.toString(); cur = StringBuilder(w) } else cur = StringBuilder(cand) }
            if (cur.isNotEmpty()) out += cur.toString(); return out
        }
        for (line in lines) {
            for (seg in wrap(line)) {
                if (y > ph - 60f) { doc.finishPage(page); pageNo++; page = doc.startPage(PdfDocument.PageInfo.Builder(pw, ph, pageNo).create()); canvas = page.canvas; y = header(canvas) }
                drawRuns(canvas, seg, margin, y); y += 20f
            }
            y += 6f
        }
        doc.finishPage(page)
        val dir = File(ctx.cacheDir, "pdf").apply { mkdirs() }
        val f = File(dir, "sandhatri-${System.currentTimeMillis()}.pdf")
        f.outputStream().use { doc.writeTo(it) }; doc.close(); return f
    }
}

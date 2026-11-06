package com.example.ui.components

import android.content.Context
import android.content.Intent
import com.example.model.Song

object ShareHelper {
    fun shareSong(context: Context, song: Song) {
        val snippet = song.lyrics.lines().take(4).joinToString("\n")
        val shareText = """
🎵 استمع إلى رائعة الفنانة رحمة رياض:
« ${song.arabicTitle} » (${song.title})

✨ كلمات من الأغنية:
$snippet

📺 شاهد واستمع إليها الآن بجودة فائقة بدون إنترنت عبر تطبيق:
RahmaRiad 🌟

━━━━━━━━━━━━━━━━━━━━
حقوق وتطوير التطبيق: ✪Mister Ali
━━━━━━━━━━━━━━━━━━━━
        """.trimIndent()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "أغنية ${song.arabicTitle} - رحمة رياض")
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, "مشاركة أغنية ${song.arabicTitle} عبر:")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}

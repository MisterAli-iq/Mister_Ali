package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeEmbedView(
    videoId: String,
    isPlaying: Boolean = true,
    isShort: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val webView = remember(context) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess = true
                allowContentAccess = true
                loadWithOverviewMode = true
                useWideViewPort = true
                cacheMode = WebSettings.LOAD_DEFAULT
            }
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    return false
                }
            }
        }
    }

    DisposableEffect(videoId) {
        val embedHtml = buildYouTubeHtml(videoId, isShort = isShort)
        webView.loadDataWithBaseURL(
            "https://www.youtube.com",
            embedHtml,
            "text/html",
            "UTF-8",
            null
        )

        onDispose {
            try {
                webView.loadUrl("about:blank")
                webView.onPause()
            } catch (_: Exception) {}
        }
    }

    DisposableEffect(isPlaying) {
        if (!isPlaying) {
            try {
                webView.evaluateJavascript("player.pauseVideo();", null)
            } catch (_: Exception) {}
        } else {
            try {
                webView.evaluateJavascript("player.playVideo();", null)
            } catch (_: Exception) {}
        }
        onDispose { }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black)
    ) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun buildYouTubeHtml(videoId: String, isShort: Boolean): String {
    val aspectRatioStyle = if (isShort) {
        "object-fit: cover; width: 100%; height: 100%;"
    } else {
        "width: 100%; height: 100%;"
    }

    return """
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { box-sizing: border-box; }
                html, body {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    background-color: #000000;
                    overflow: hidden;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                #player {
                    $aspectRatioStyle
                    border: none;
                }
                .ytp-watermark, .ytp-youtube-button { display: none !important; }
            </style>
        </head>
        <body>
            <iframe 
                id="player"
                src="https://www.youtube-nocookie.com/embed/$videoId?enablejsapi=1&autoplay=1&playsinline=1&rel=0&modestbranding=1&controls=1&showinfo=0&fs=1"
                frameborder="0"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                allowfullscreen>
            </iframe>
        </body>
        </html>
    """.trimIndent()
}

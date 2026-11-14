package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import coil.compose.AsyncImage
import com.example.R
import com.example.data.remote.MediaStreamResolver
import com.example.ui.theme.YouTubeGold
import com.example.ui.theme.YouTubeRed
import kotlinx.coroutines.delay
import org.json.JSONArray
import java.util.concurrent.atomic.AtomicBoolean

private const val PLAYER_TAG = "NativeMediaPlayerView"
private const val SNIFFER_TIMEOUT_MS = 6_000L

internal fun safelyDestroyWebView(
    webView: WebView?,
    destroyedFlag: AtomicBoolean? = null
) {
    if (webView == null) return
    if (destroyedFlag != null && !destroyedFlag.compareAndSet(false, true)) return
    runCatching {
        webView.stopLoading()
        webView.onPause()
        webView.webChromeClient = null
        webView.webViewClient = WebViewClient()
        webView.loadUrl("about:blank")
        webView.removeAllViews()
        (webView.parent as? ViewGroup)?.removeView(webView)
        Handler(Looper.getMainLooper()).post {
            runCatching { webView.destroy() }
        }
    }.onFailure { err ->
        Log.w(PLAYER_TAG, "WebView cleanup ignored: ${err.message}")
    }
}

@kotlin.OptIn(ExperimentalLayoutApi::class)
@OptIn(UnstableApi::class)
@Composable
fun NativeMediaPlayerView(
    player: Player?,
    isResolvingStream: Boolean,
    isBuffering: Boolean,
    errorMessage: String?,
    recoveryStatusMessage: String? = null,
    fallbackEmbedUrl: String? = null,
    useWebFallback: Boolean = false,
    snifferTargetUrl: String? = null,
    thumbnailUrl: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    onRetry: () -> Unit = {},
    onLowerQuality: (() -> Unit)? = null,
    onClearCache: (() -> Unit)? = null,
    onSwitchAlternativePlayer: (() -> Unit)? = null,
    onStreamCaptured: ((String, Map<String, String>) -> Unit)? = null,
    onSnifferFailed: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (useWebFallback && !fallbackEmbedUrl.isNullOrBlank()) {
            SilentEmbeddedWebPlayer(
                embedUrl = fallbackEmbedUrl,
                thumbnailUrl = thumbnailUrl,
                onRetryNativeStream = onRetry,
                modifier = Modifier.fillMaxSize()
            )
        } else if (player != null && errorMessage == null) {
            ContentFrame(
                player = player,
                modifier = Modifier.fillMaxSize(),
                surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                contentScale = contentScale,
                shutter = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                }
            )
        }

        // Hidden headless WebView stream-capture backup (captures real .mp4 / .m3u8 for native ExoPlayer)
        if (!snifferTargetUrl.isNullOrBlank() && !useWebFallback && errorMessage == null && onStreamCaptured != null) {
            HiddenWebStreamSniffer(
                targetUrl = snifferTargetUrl,
                onStreamCaptured = onStreamCaptured,
                onCaptureFailed = { onSnifferFailed?.invoke() }
            )
        }

        if ((isResolvingStream || isBuffering || !recoveryStatusMessage.isNullOrBlank()) && errorMessage == null && !useWebFallback) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(Color(0xBB000000), RoundedCornerShape(14.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .testTag("player_loading_indicator")
            ) {
                CircularProgressIndicator(
                    color = YouTubeGold,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = recoveryStatusMessage
                        ?: if (isResolvingStream) "جارٍ تحضير رابط البث المباشر..." else "جارٍ تشغيل الفيديو...",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (errorMessage != null && !useWebFallback) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0E0E12))
            ) {
                if (!thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        alpha = 0.22f
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = YouTubeRed,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("player_error_message_text")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "يمكنك إعادة المحاولة أو خفض الجودة للمتابعة",
                        color = YouTubeGold,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("player_error_retry_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إعادة المحاولة", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (onLowerQuality != null) {
                            OutlinedButton(
                                onClick = onLowerQuality,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, YouTubeGold.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("player_error_lower_quality_button")
                            ) {
                                Icon(Icons.Default.HighQuality, contentDescription = null, tint = YouTubeGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("جودة أقل (360p)", color = YouTubeGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (onClearCache != null) {
                            OutlinedButton(
                                onClick = onClearCache,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, Color(0xFF555555)),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("player_error_clear_cache_button")
                            ) {
                                Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مسح الكاش", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (onSwitchAlternativePlayer != null && !fallbackEmbedUrl.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = onSwitchAlternativePlayer,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, Color(0xFF64B5F6)),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("player_error_alt_player_button")
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("المشغل البديل", color = Color(0xFF64B5F6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Hidden off-UI WebView stream sniffer used purely as a backup extractor to intercept real
 * .mp4 / .m3u8 / videoplayback network requests or DOM <video> sources and hand them back
 * to the native built-in ExoPlayer within a hard 6-second cap.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HiddenWebStreamSniffer(
    targetUrl: String,
    onStreamCaptured: (String, Map<String, String>) -> Unit,
    onCaptureFailed: () -> Unit
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val latestCaptured by rememberUpdatedState(onStreamCaptured)
    val latestFailed by rememberUpdatedState(onCaptureFailed)
    val delivered = remember(targetUrl) { AtomicBoolean(false) }
    val isDestroyed = remember(targetUrl) { AtomicBoolean(false) }

    val isYouTubeWatch = remember(targetUrl) {
        targetUrl.contains("youtube.com/watch", ignoreCase = true) ||
            targetUrl.contains("youtu.be/", ignoreCase = true)
    }

    val snifferWebView = remember(context, targetUrl) {
        runCatching {
            WebView(context).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
                }
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadsImagesAutomatically = false
                    blockNetworkImage = true
                    mediaPlaybackRequiresUserGesture = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        safeBrowsingEnabled = false
                    }
                    userAgentString = if (isYouTubeWatch) {
                        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"
                    } else {
                        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                    }
                }
                webChromeClient = WebChromeClient()
            }
        }.onFailure { err ->
            Log.w(PLAYER_TAG, "Hidden sniffer WebView creation failed: ${err.message}")
        }.getOrNull()
    }

    if (snifferWebView == null) {
        LaunchedEffect(targetUrl) {
            if (delivered.compareAndSet(false, true)) {
                latestFailed()
            }
        }
        return
    }

    LaunchedEffect(snifferWebView, targetUrl) {
        val defaultHeaders = mapOf(
            "User-Agent" to (snifferWebView.settings.userAgentString ?: ""),
            "Referer" to targetUrl
        )

        fun isFullPlayableCapture(candidateUrl: String): Boolean {
            if (!MediaStreamResolver.isDirectMediaUri(candidateUrl)) return false
            val lower = candidateUrl.lowercase()
            // Ignore tiny byte-range DASH chunks that lack full audio+video timeline
            if (lower.contains("googlevideo.com/videoplayback") &&
                (lower.contains("&range=") || lower.contains("?range="))
            ) {
                return false
            }
            return true
        }

        fun tryDeliverCaptured(candidate: String?, headers: Map<String, String>) {
            val clean = candidate?.trim()?.replace("&amp;", "&").orEmpty()
            if (clean.isBlank() || !isFullPlayableCapture(clean)) return
            if (delivered.compareAndSet(false, true)) {
                mainHandler.post {
                    runCatching { snifferWebView.stopLoading() }
                    latestCaptured(clean, headers)
                }
            }
        }

        snifferWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val scheme = request?.url?.scheme?.lowercase().orEmpty()
                return scheme != "http" && scheme != "https"
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val reqUrl = request?.url?.toString().orEmpty()
                if (reqUrl.isNotBlank() && isFullPlayableCapture(reqUrl)) {
                    val reqHeaders = HashMap(request?.requestHeaders ?: defaultHeaders)
                    if (!reqHeaders.containsKey("Referer")) {
                        reqHeaders["Referer"] = targetUrl
                    }
                    tryDeliverCaptured(reqUrl, reqHeaders)
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (delivered.get() || isDestroyed.get()) return
                val probeJs = """
                    (function() {
                        try {
                            var found = [];
                            var vids = document.querySelectorAll('video, source');
                            for (var i = 0; i < vids.length; i++) {
                                var v = vids[i];
                                var s = v.currentSrc || v.src || v.getAttribute('src');
                                if (s && s.indexOf('http') === 0) found.push(s);
                            }
                            if (window.performance && window.performance.getEntriesByType) {
                                var res = window.performance.getEntriesByType('resource');
                                for (var j = 0; j < res.length; j++) {
                                    var name = res[j].name || '';
                                    if (name.indexOf('.mp4') !== -1 || name.indexOf('.m3u8') !== -1 || name.indexOf('videoplayback') !== -1) {
                                        found.push(name);
                                    }
                                }
                            }
                            return JSON.stringify(found);
                        } catch(e) { return '[]'; }
                    })();
                """.trimIndent()
                view?.evaluateJavascript(probeJs) { rawJson ->
                    if (delivered.get() || isDestroyed.get() || rawJson.isNullOrBlank()) return@evaluateJavascript
                    runCatching {
                        val unwrapped = rawJson.trim().let {
                            if (it.startsWith("\"") && it.endsWith("\"")) {
                                org.json.JSONTokener(it).nextValue().toString()
                            } else it
                        }
                        val arr = JSONArray(unwrapped)
                        for (i in 0 until arr.length()) {
                            val candidate = arr.optString(i)
                            if (isFullPlayableCapture(candidate)) {
                                tryDeliverCaptured(candidate, defaultHeaders)
                                break
                            }
                        }
                    }
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true && delivered.compareAndSet(false, true)) {
                    latestFailed()
                }
            }

            override fun onRenderProcessGone(
                view: WebView?,
                detail: RenderProcessGoneDetail?
            ): Boolean {
                safelyDestroyWebView(view, isDestroyed)
                if (delivered.compareAndSet(false, true)) {
                    mainHandler.post { latestFailed() }
                }
                return true
            }
        }

        snifferWebView.onResume()
        snifferWebView.loadUrl(targetUrl)

        delay(SNIFFER_TIMEOUT_MS)
        if (delivered.compareAndSet(false, true)) {
            runCatching { snifferWebView.stopLoading() }
            latestFailed()
        }
    }

    key(snifferWebView) {
        AndroidView(
            factory = { snifferWebView },
            onRelease = { safelyDestroyWebView(it, isDestroyed) },
            modifier = Modifier
                .size(1.dp)
                .alpha(0f)
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SilentEmbeddedWebPlayer(
    embedUrl: String,
    thumbnailUrl: String? = null,
    onRetryNativeStream: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPageLoading by remember(embedUrl) { mutableStateOf(true) }
    var hasRenderCrash by remember(embedUrl) { mutableStateOf(false) }

    if (hasRenderCrash) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0E0E12)),
            contentAlignment = Alignment.Center
        ) {
            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    alpha = 0.28f
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.rahma_hero_banner),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    alpha = 0.25f
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircleOutline,
                    contentDescription = null,
                    tint = YouTubeGold,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "تعذر تشغيل الفيديو، اضغط لإعادة المحاولة",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRetryNativeStream,
                    colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("إعادة المحاولة", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    val isDestroyed = remember(embedUrl) { AtomicBoolean(false) }
    val webView = remember(context, embedUrl) {
        runCatching {
            WebView(context).apply {
                setBackgroundColor(AndroidColor.BLACK)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
                }
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        safeBrowsingEnabled = false
                    }
                    userAgentString =
                        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                }
                webChromeClient = WebChromeClient()
            }
        }.onFailure { err ->
            Log.w(PLAYER_TAG, "WebView creation failed: ${err.message}")
        }.getOrNull()
    }

    if (webView == null) {
        LaunchedEffect(Unit) {
            hasRenderCrash = true
            isPageLoading = false
        }
        return
    }

    LaunchedEffect(webView, embedUrl) {
        isPageLoading = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val target = request?.url?.toString()?.lowercase() ?: return true
                val baseHost = runCatching { android.net.Uri.parse(embedUrl).host?.lowercase().orEmpty() }.getOrDefault("")
                val isAllowedEmbed = target.contains("/embed/") ||
                    target.contains("youtube-nocookie.com/embed") ||
                    target.contains("instagram.com/reel") ||
                    target.contains("tiktok.com/embed") ||
                    target.contains("facebook.com/plugins/video.php") ||
                    (baseHost.isNotBlank() && target.contains(baseHost))
                return !isAllowedEmbed
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isPageLoading = false
                if (isDestroyed.get()) return
                view?.evaluateJavascript(
                    """
                    (function() {
                        try {
                            var style = document.createElement('style');
                            style.innerHTML = '.ytp-chrome-top, .ytp-youtube-button, .ytp-watermark, .ytp-pause-overlay { display: none !important; opacity: 0 !important; pointer-events: none !important; } body, html { background: #000 !important; margin: 0 !important; overflow: hidden !important; }';
                            document.head.appendChild(style);
                        } catch(e) {}
                    })();
                    """.trimIndent(),
                    null
                )
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    isPageLoading = false
                }
            }

            override fun onRenderProcessGone(
                view: WebView?,
                detail: RenderProcessGoneDetail?
            ): Boolean {
                val didCrash = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    detail?.didCrash() == true
                } else {
                    true
                }
                Log.w(PLAYER_TAG, "WebView render process gone (didCrash=$didCrash), switching to native fallback")
                safelyDestroyWebView(view, isDestroyed)
                isPageLoading = false
                hasRenderCrash = true
                return true
            }
        }
        webView.onResume()
        webView.loadUrl(embedUrl)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        key(webView) {
            AndroidView(
                factory = { webView },
                onRelease = { released ->
                    safelyDestroyWebView(released, isDestroyed)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        if (isPageLoading) {
            CircularProgressIndicator(
                color = YouTubeGold,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

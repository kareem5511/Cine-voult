package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.TrailerAutoPlayMode
import com.example.data.model.TrailerOption
import com.example.ui.theme.machinedCardBorder

fun isTrailerAutoPlayAllowed(context: Context, modeKey: String): Boolean {
    return when (TrailerAutoPlayMode.fromKey(modeKey)) {
        TrailerAutoPlayMode.OFF -> false
        TrailerAutoPlayMode.ALWAYS -> true
        TrailerAutoPlayMode.WIFI_ONLY -> isWifiNetworkConnected(context)
    }
}

fun isWifiNetworkConnected(context: Context): Boolean {
    return runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return@runCatching false
        val activeNetwork = cm.activeNetwork ?: return@runCatching false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return@runCatching false
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }.getOrDefault(false)
}

@OptIn(UnstableApi::class)
@Composable
fun InlineSpotlightTrailerPlayer(
    trailer: TrailerOption,
    isMuted: Boolean,
    isPaused: Boolean,
    modifier: Modifier = Modifier,
    onVideoReady: () -> Unit = {}
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    if (isPreview) return

    val directUrl = trailer.directUrl
    if (!directUrl.isNullOrBlank()) {
        val player = remember(directUrl) {
            runCatching {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(directUrl))
                    repeatMode = Player.REPEAT_MODE_ONE
                    playWhenReady = !isPaused
                    volume = if (isMuted) 0f else 1f
                    prepare()
                }
            }.getOrNull()
        }

        if (player != null) {
            LaunchedEffect(isMuted) {
                player.volume = if (isMuted) 0f else 1f
            }
            LaunchedEffect(isPaused) {
                player.playWhenReady = !isPaused
            }

            DisposableEffect(player) {
                val listener = object : Player.Listener {
                    override fun onRenderedFirstFrame() {
                        onVideoReady()
                    }
                }
                player.addListener(listener)
                onDispose {
                    player.removeListener(listener)
                    player.release()
                }
            }

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        this.player = player
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    if (view.player !== player) view.player = player
                },
                modifier = modifier
            )
        }
    } else if (!trailer.ytId.isNullOrBlank() && !isPaused) {
        val embedUrl = "https://www.youtube-nocookie.com/embed/${trailer.ytId}" +
            "?autoplay=1&mute=${if (isMuted) 1 else 0}&controls=0&rel=0&modestbranding=1&playsinline=1&loop=1&playlist=${trailer.ytId}"
        var webViewRef by remember { mutableStateOf<WebView?>(null) }

        DisposableEffect(Unit) {
            onDispose {
                webViewRef?.apply {
                    stopLoading()
                    loadUrl("about:blank")
                    destroy()
                }
            }
        }

        AndroidView(
            factory = { ctx ->
                createTrailerWebView(ctx, embedUrl) {
                    onVideoReady()
                }.also { webViewRef = it }
            },
            update = { wv ->
                val jsMute = if (isMuted) "mute" else "unMute"
                wv.evaluateJavascript("/* toggle $jsMute */", null)
            },
            modifier = modifier
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun CinemaTrailerLightboxDialog(
    title: String,
    trailer: TrailerOption,
    onBrowseStreams: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val isPreview = LocalInspectionMode.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            val modalShape = RoundedCornerShape(22.dp)
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = modalShape,
                tonalElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .machinedCardBorder(shape = modalShape, cornerRadiusDp = 22.dp)
                    .testTag("cinema_trailer_lightbox")
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 12.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${trailer.title} • ${trailer.typeLabel}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val externalLink = trailer.directUrl
                                ?: trailer.ytId?.let { "https://www.youtube.com/watch?v=$it" }
                            if (!externalLink.isNullOrBlank()) {
                                IconButton(
                                    onClick = { runCatching { uriHandler.openUri(externalLink) } }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.OpenInBrowser,
                                        contentDescription = "Open external trailer",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("lightbox_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close trailer",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // 16:9 Cinema Frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isPreview && !trailer.directUrl.isNullOrBlank()) {
                            val exoPlayer = remember(trailer.directUrl) {
                                runCatching {
                                    ExoPlayer.Builder(context).build().apply {
                                        setMediaItem(MediaItem.fromUri(trailer.directUrl))
                                        playWhenReady = true
                                        prepare()
                                    }
                                }.getOrNull()
                            }
                            if (exoPlayer != null) {
                                DisposableEffect(exoPlayer) {
                                    onDispose { exoPlayer.release() }
                                }
                                AndroidView(
                                    factory = { ctx ->
                                        PlayerView(ctx).apply {
                                            useController = true
                                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                            player = exoPlayer
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else if (!isPreview && !trailer.ytId.isNullOrBlank()) {
                            val embedUrl = "https://www.youtube-nocookie.com/embed/${trailer.ytId}" +
                                "?autoplay=1&rel=0&modestbranding=1&playsinline=1"
                            var wvRef by remember { mutableStateOf<WebView?>(null) }
                            DisposableEffect(Unit) {
                                onDispose {
                                    wvRef?.apply {
                                        stopLoading()
                                        loadUrl("about:blank")
                                        destroy()
                                    }
                                }
                            }
                            AndroidView(
                                factory = { ctx ->
                                    createTrailerWebView(ctx, embedUrl).also { wvRef = it }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Movie,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = trailer.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Pinned Browse Streams CTA below video
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        Button(
                            onClick = onBrowseStreams,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lightbox_browse_streams_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Browse Streams",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createTrailerWebView(
    context: Context,
    url: String,
    onLoaded: () -> Unit = {}
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(android.graphics.Color.BLACK)
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }
        webChromeClient = WebChromeClient()
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                onLoaded()
            }
        }
        loadUrl(url)
    }
}

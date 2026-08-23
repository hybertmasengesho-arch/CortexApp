package com.cortex.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

/**
 * A thin native shell around the real Cortex website. This is deliberately
 * simple — all the actual file-sharing, team, and notes functionality still
 * lives on the website (rh4work repo) and is loaded here in a WebView.
 *
 * The one thing this app adds that the website alone cannot: a Quick
 * Settings tile (see ShareTileService.kt) that jumps straight to the file
 * upload screen from the phone's control panel.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true   // needed: the site stores your login token here
        webView.webViewClient = WebViewClient()      // keeps navigation inside the app instead of opening Chrome

        val jumpToFiles = intent?.getBooleanExtra(EXTRA_OPEN_FILES, false) ?: false
        val url = if (jumpToFiles) "$BASE_URL/files.html" else "$BASE_URL/dashboard.html"
        webView.loadUrl(url)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    companion object {
        // TODO: update this if your Netlify domain ever changes.
        const val BASE_URL = "https://reasoningcortexhub.netlify.app"
        const val EXTRA_OPEN_FILES = "open_files"
    }
}

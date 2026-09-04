package com.cortex.app

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

/**
 * A thin native shell around the real Cortex website. This is deliberately
 * simple — all the actual file-sharing, team, and notes functionality still
 * lives on the website (rh4work repo) and is loaded here in a WebView.
 *
 * Two things a bare WebView does NOT support out of the box, which this
 * class adds because the site genuinely needs them:
 *
 * 1. FILE UPLOADS / THE FILE PICKER — every <input type="file"> on the
 *    site (Files, Notes, Reader, and a couple of others) silently does
 *    nothing without a WebChromeClient.onShowFileChooser implementation.
 *    See fileChooserLauncher below — this one native fix covers every
 *    page, since they all use the same underlying HTML file input.
 *
 * 2. DOWNLOADING / OPENING A FILE — the site fetches files as an
 *    authenticated Blob in JavaScript (it has to: the download needs a
 *    Bearer token a plain browser navigation can't send), then either
 *    window.open()s a blob: URL or force-downloads it. Both silently fail
 *    in a WebView: window.open() needs window-creation support this
 *    WebView doesn't have wired up, and blob: URLs aren't independently
 *    readable by native code even if it did. See public/js/nav.js's
 *    window.HubBlobDeliver on the website side — it detects
 *    window.AndroidDownload (added below) and routes the file over as
 *    base64 through the JS bridge instead, which saveBase64File() here
 *    then actually writes to the phone's Downloads folder.
 *
 * Also adds a Quick Settings tile (see ShareTileService.kt) that jumps
 * straight to the file upload screen from the phone's control panel —
 * something no website can add to itself.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Registered before the WebView is even shown a page — the launcher
        // has to exist before onShowFileChooser can ever fire.
        fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val callback = fileChooserCallback
            fileChooserCallback = null
            if (callback == null) return@registerForActivityResult

            val data = result.data
            if (result.resultCode != RESULT_OK || data == null) {
                callback.onReceiveValue(null) // user backed out of the picker — tell the page nothing was chosen
                return@registerForActivityResult
            }

            // Handles both a single file and (for inputs with `multiple`) several at once.
            val uris: Array<Uri> = when {
                data.clipData != null -> {
                    val clip = data.clipData!!
                    Array(clip.itemCount) { i -> clip.getItemAt(i).uri }
                }
                data.data != null -> arrayOf(data.data!!)
                else -> arrayOf()
            }
            callback.onReceiveValue(if (uris.isNotEmpty()) uris else null)
        }

        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true   // needed: the site stores your login token here
        webView.webViewClient = WebViewClient()      // keeps navigation inside the app instead of opening Chrome

        webView.webChromeClient = object : WebChromeClient() {
            // Fires whenever the page's JS clicks an <input type="file">.
            // Without this override, that click does nothing at all.
            override fun onShowFileChooser(
                view: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileChooserCallback?.onReceiveValue(null) // cancel any previous pending chooser first
                fileChooserCallback = filePathCallback

                val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                if (fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }

                return try {
                    fileChooserLauncher.launch(intent)
                    true
                } catch (e: Exception) {
                    fileChooserCallback = null
                    Toast.makeText(this@MainActivity, "Couldn't open the file picker.", Toast.LENGTH_SHORT).show()
                    false
                }
            }
        }

        // The bridge the website's window.HubBlobDeliver looks for — see the
        // big comment at the top of this class for why this exists at all.
        webView.addJavascriptInterface(AndroidDownloadBridge(this), "AndroidDownload")

        val jumpToFiles = intent?.getBooleanExtra(EXTRA_OPEN_FILES, false) ?: false
        val url = if (jumpToFiles) "$BASE_URL/files.html" else "$BASE_URL/dashboard.html"
        webView.loadUrl(url)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    companion object {
        // TODO: update this if your Netlify domain ever changes.
        const val BASE_URL = "https://caputa.netlify.app"
        const val EXTRA_OPEN_FILES = "open_files"
    }
}

/**
 * The native half of window.AndroidDownload.saveBase64File(...) — see
 * public/js/nav.js on the website for the JS half that calls this.
 *
 * Runs on a background thread (JS bridge calls do NOT run on the UI
 * thread by default, but Toast/startActivity need it) — posted back to
 * the main thread via runOnUiThread for anything UI-related.
 */
class AndroidDownloadBridge(private val activity: MainActivity) {

    @JavascriptInterface
    fun saveBase64File(base64Data: String, filename: String, mimeType: String, openAfterSave: Boolean) {
        try {
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(bytes, filename, mimeType)
            } else {
                saveViaLegacyFile(bytes, filename)
            }

            activity.runOnUiThread {
                if (uri == null) {
                    Toast.makeText(activity, "Couldn't save \"$filename.\"", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                Toast.makeText(activity, "Saved \"$filename\" to Downloads", Toast.LENGTH_SHORT).show()
                if (openAfterSave) openFile(uri, mimeType)
            }
        } catch (e: Exception) {
            activity.runOnUiThread {
                Toast.makeText(activity, "Couldn't save \"$filename.\"", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Android 10+ — writes into the shared Downloads collection via
    // MediaStore. No storage permission needed at all on this path; the
    // returned content:// Uri is safely shareable with other apps as-is.
    private fun saveViaMediaStore(bytes: ByteArray, filename: String, mimeType: String): Uri? {
        val resolver = activity.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val itemUri = resolver.insert(collection, values) ?: return null
        resolver.openOutputStream(itemUri)?.use { out -> out.write(bytes) } ?: return null
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(itemUri, values, null, null)
        return itemUri
    }

    // Android 9 and below — MediaStore.Downloads doesn't exist yet, so this
    // writes straight to the public Downloads directory the old way.
    // Requires WRITE_EXTERNAL_STORAGE on these versions (declared in the
    // manifest with maxSdkVersion="28" — not needed, and not requested, on
    // Android 10+).
    private fun saveViaLegacyFile(bytes: ByteArray, filename: String): Uri? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, filename)
            FileOutputStream(file).use { it.write(bytes) }
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }

    private fun openFile(uri: Uri, mimeType: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType.ifBlank { "*/*" })
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            // No app on the phone can open this file type — the file is
            // still safely saved to Downloads, just can't be auto-opened.
            Toast.makeText(activity, "Saved to Downloads — no app installed to open this file type.", Toast.LENGTH_LONG).show()
        }
    }
}

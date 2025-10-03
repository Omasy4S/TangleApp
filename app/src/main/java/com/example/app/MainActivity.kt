package com.example.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.webkit.JavascriptInterface
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    // Use ActivityResultContracts
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        filePathCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data))
        filePathCallback = null
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) launchFilePicker() else filePathCallback?.onReceiveValue(null)
        filePathCallback = null
    }

    private val widgetUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == KnittingCounterWidget.ACTION_WIDGET_UPDATE) {
                intent.getStringExtra("knittingProjects")?.let { updateWebViewData(it) }
            }
        }
    }

    private val filter = IntentFilter(KnittingCounterWidget.ACTION_WIDGET_UPDATE)

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webview)
        setupWebView()
        syncDataFromWidget()
        onBackPressedDispatcher.addCallback(this) {
            if (webView.canGoBack()) webView.goBack() else finish()
        }
    }

    override fun onResume() {
        super.onResume()
        ContextCompat.registerReceiver(this, widgetUpdateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        syncDataFromWidget()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(widgetUpdateReceiver)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            allowFileAccess = false
            allowContentAccess = true
            setSupportZoom(false)
            mediaPlaybackRequiresUserGesture = false
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                syncDataFromWidget()
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(webView: WebView?, callback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launchFilePicker()
                } else {
                    val permission = Manifest.permission.READ_EXTERNAL_STORAGE
                    if (ContextCompat.checkSelfPermission(this@MainActivity, permission) == PackageManager.PERMISSION_GRANTED) {
                        launchFilePicker()
                    } else {
                        permissionLauncher.launch(permission)
                    }
                }
                return true
            }

            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Подтверждение")
                    .setMessage(message)
                    .setPositiveButton("ОК") { _, _ -> result?.confirm() }
                    .setNegativeButton("Отмена") { _, _ -> result?.cancel() }
                    .setCancelable(false)
                    .show()
                return true
            }

            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Внимание")
                    .setMessage(message)
                    .setPositiveButton("OK") { _, _ -> result?.confirm() }
                    .setCancelable(false)
                    .show()
                return true
            }
        }
        webView.addJavascriptInterface(WebAppInterface(this), "Android")
        webView.loadUrl("file:///android_asset/kiniti.html")
    }

    private fun launchFilePicker() {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "application/pdf", "text/plain"))
                addCategory(Intent.CATEGORY_OPENABLE)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            filePickerLauncher.launch(intent)
        } catch (e: Exception) {
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
        }
    }

    private fun injectJsonIntoWebView(jsonString: String) {
        try {
            JSONArray(jsonString)
            val escapedJson = jsonString.replace("\\", "\\\\").replace("\"", "\\\"")
            webView.evaluateJavascript("window.updateProjectsFromAndroid(\"$escapedJson\");", null)
        } catch (e: Exception) {
            webView.evaluateJavascript("console.error('Invalid JSON from Android');", null)
        }
    }

    private fun syncDataFromWidget() {
        val prefs = getSharedPreferences("KnittingWidgetPrefs", Context.MODE_PRIVATE)
        val projectsJson = prefs.getString("knittingProjects", "[]") ?: "[]"
        injectJsonIntoWebView(projectsJson)
    }

    private fun updateWebViewData(projectsJson: String) {
        injectJsonIntoWebView(projectsJson)
    }

    inner class WebAppInterface(private val context: Context) {
        @JavascriptInterface
        fun saveProjects(projectsJson: String) {
            try {
                JSONArray(projectsJson)
                context.getSharedPreferences("KnittingWidgetPrefs", Context.MODE_PRIVATE)
                    .edit().putString("knittingProjects", projectsJson).apply()
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, KnittingCounterWidget::class.java))
                KnittingCounterWidget.updateAppWidget(context, appWidgetManager, *ids)
            } catch (e: Exception) { }
        }

        @JavascriptInterface
        fun openFile(fileDataUrl: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                val uri: Uri = when {
                    fileDataUrl.startsWith("data:application/pdf;base64,") -> {
                        val base64Data = fileDataUrl.substring("data:application/pdf;base64,".length)
                        val pdfBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                        val file = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")
                        FileOutputStream(file).use { it.write(pdfBytes) }
                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    }
                    fileDataUrl.startsWith("data:text/plain;base64,") -> {
                        val base64Data = fileDataUrl.substring("data:text/plain;base64,".length)
                        val textBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                        val file = File(context.cacheDir, "temp_note_${System.currentTimeMillis()}.txt")
                        FileOutputStream(file).use { it.write(textBytes) }
                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    }
                    else -> Uri.parse(fileDataUrl)
                }
                intent.setDataAndType(uri, if (fileDataUrl.contains("pdf")) "application/pdf" else "*/*")
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                (context as? Activity)?.runOnUiThread {
                    android.widget.Toast.makeText(context, "Нет приложения для файла или ошибка открытия", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

    }
}

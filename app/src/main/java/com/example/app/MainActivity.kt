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
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.webkit.JavascriptInterface

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val FILE_CHOOSER_REQUEST_CODE = 1

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    // BroadcastReceiver для обновления из виджета
    private val widgetUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.app.WIDGET_UPDATE") {
                val projectsJson = intent.getStringExtra("knittingProjects") ?: return
                webView.evaluateJavascript(
                    "localStorage.setItem('knittingProjects', '${projectsJson.replace("'", "\\'")}'); " +
                            "projects = JSON.parse(localStorage.getItem('knittingProjects') || '[]'); " +
                            "if (currentProjectIndex !== -1) renderCounters();",
                    null
                )
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportZoom(false)
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        // Загружаем данные из SharedPreferences при старте
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val prefs = getSharedPreferences("KnittingWidgetPrefs", Context.MODE_PRIVATE)
                val projectsJson = prefs.getString("knittingProjects", null)
                if (projectsJson != null) {
                    webView.evaluateJavascript(
                        "localStorage.setItem('knittingProjects', '${projectsJson.replace("'", "\\'")}'); " +
                                "projects = JSON.parse(localStorage.getItem('knittingProjects') || '[]');",
                        null
                    )
                }
            }
        }

        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.filePathCallback?.onReceiveValue(null)
                this@MainActivity.filePathCallback = filePathCallback

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    val permission = Manifest.permission.READ_EXTERNAL_STORAGE
                    if (ContextCompat.checkSelfPermission(this@MainActivity, permission) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissionLauncher.launch(permission)
                        return false
                    }
                }

                return try {
                    val intent = fileChooserParams?.createIntent()?.apply { type = "image/*" }
                    if (intent != null) {
                        startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)
                        true
                    } else {
                        filePathCallback?.onReceiveValue(null)
                        this@MainActivity.filePathCallback = null
                        false
                    }
                } catch (e: Exception) {
                    filePathCallback?.onReceiveValue(null)
                    this@MainActivity.filePathCallback = null
                    false
                }
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

        webView.loadUrl("file:///android_asset/kiniti.html")

        onBackPressedDispatcher.addCallback(this) {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                finish()
            }
        }

        // Регистрируем BroadcastReceiver (как вы просили)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(widgetUpdateReceiver, IntentFilter("com.example.app.WIDGET_UPDATE"), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(widgetUpdateReceiver, IntentFilter("com.example.app.WIDGET_UPDATE"))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            filePathCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
            filePathCallback = null
        }
    }

    override fun onDestroy() {
        unregisterReceiver(widgetUpdateReceiver)
        super.onDestroy()
    }

    // Интерфейс для синхронизации с виджетом
    class WebAppInterface(private val context: Context) {
        @JavascriptInterface
        fun saveProjects(projectsJson: String) {
            val prefs = context.getSharedPreferences("KnittingWidgetPrefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("knittingProjects", projectsJson)
                .apply()

            // Обновляем все виджеты
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, KnittingCounterWidget::class.java))
            KnittingCounterWidget.updateAppWidget(context, appWidgetManager, *ids)
        }
    }
}
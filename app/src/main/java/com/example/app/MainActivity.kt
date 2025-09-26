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
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.webkit.JavascriptInterface

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val FILE_CHOOSER_REQUEST_CODE = 1

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && filePathCallback != null) {
            openFileChooser()
        } else {
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
        }
    }

    private val widgetUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == KnittingCounterWidget.ACTION_WIDGET_UPDATE) {
                val projectsJson = intent.getStringExtra("knittingProjects") ?: return
                updateWebViewData(projectsJson)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        setupWebView()

        // Синхронизация данных при запуске
        syncDataFromWidget()
    }

    override fun onResume() {
        super.onResume()
        // Синхронизация данных при возвращении в приложение
        syncDataFromWidget()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(widgetUpdateReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver не был зарегистрирован
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            filePathCallback?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            )
            filePathCallback = null
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportZoom(false)
            useWideViewPort = true
            loadWithOverviewMode = true
            allowFileAccess = true
            allowContentAccess = true
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                syncDataFromWidget()
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
                return openFileChooser()
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

        // Регистрация ресивера для обновлений из виджета
        val filter = IntentFilter(KnittingCounterWidget.ACTION_WIDGET_UPDATE)
        ContextCompat.registerReceiver(
            this,
            widgetUpdateReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun openFileChooser(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)
            true
        } catch (e: Exception) {
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
            false
        }
    }

    private fun syncDataFromWidget() {
        val prefs = getSharedPreferences("KnittingWidgetPrefs", Context.MODE_PRIVATE)
        val projectsJson = prefs.getString("knittingProjects", "[]") ?: "[]"
        // Принудительно устанавливаем localStorage из SharedPreferences
        val safeJson = projectsJson.replace("'", "\\'")
        webView.evaluateJavascript(
            """
        (function() {
            try {
                localStorage.setItem('knittingProjects', JSON.stringify($safeJson));
                if (typeof renderProjects === 'function') {
                    window.projects = JSON.parse(localStorage.getItem('knittingProjects')) || [];
                    renderProjects();
                    if (window.isProjectModalOpen && typeof window.renderCounters === 'function') {
                        window.renderCounters();
                    }
                }
            } catch (e) {
                console.error('Error syncing from widget:', e);
            }
        })();
        """.trimIndent(),
            null
        )
    }

    private fun updateWebViewData(projectsJson: String) {
        val safeJson = projectsJson.replace("'", "\\'")
        webView.evaluateJavascript(
            """
        (function() {
            try {
                localStorage.setItem('knittingProjects', JSON.stringify($safeJson));
                window.projects = JSON.parse(localStorage.getItem('knittingProjects')) || [];
                if (typeof renderProjects === 'function') {
                    renderProjects();
                }
                if (window.isProjectModalOpen && typeof window.renderCounters === 'function') {
                    window.renderCounters();
                }
            } catch (e) {
                console.error('Error updating from widget:', e);
            }
        })();
        """.trimIndent(),
            null
        )
    }

    inner class WebAppInterface(private val context: Context) {
        @JavascriptInterface
        fun saveProjects(projectsJson: String) {
            context.getSharedPreferences("KnittingWidgetPrefs", Context.MODE_PRIVATE).edit()
                .putString("knittingProjects", projectsJson)
                .apply()

            // Обновляем все виджеты
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, KnittingCounterWidget::class.java)
            )
            KnittingCounterWidget.updateAppWidget(context, appWidgetManager, *ids)
        }
    }
}
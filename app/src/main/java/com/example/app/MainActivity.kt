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
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    // === Современный лаунчер для выбора файлов ===
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        filePathCallback?.onReceiveValue(
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        )
        filePathCallback = null
    }

    // === Лаунчер для разрешений (только для < Android 13) ===
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchFilePicker()
        } else {
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
        }
    }

    private val widgetUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == KnittingCounterWidget.ACTION_WIDGET_UPDATE) {
                intent.getStringExtra("knittingProjects")?.let { json ->
                    updateWebViewData(json)
                }
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
        ContextCompat.registerReceiver(
            this,
            widgetUpdateReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
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
            allowFileAccess = false // ✅ Отключено для безопасности
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
            override fun onShowFileChooser(
                webView: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
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
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "application/pdf"))
                addCategory(Intent.CATEGORY_OPENABLE)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            filePickerLauncher.launch(intent)
        } catch (e: Exception) {
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
        }
    }

    // === Безопасная передача JSON в WebView ===
    private fun injectJsonIntoWebView(jsonString: String) {
        try {
            // Проверяем валидность JSON
            JSONArray(jsonString) // или JSONObject, если это объект
            val escapedJson = jsonString.replace("\\", "\\\\").replace("\"", "\\\"")
            webView.evaluateJavascript(
                "window.updateProjectsFromAndroid(\"$escapedJson\");",
                null
            )
        } catch (e: Exception) {
            // Невалидный JSON — можно логировать
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
                // Валидация JSON на стороне Android
                JSONArray(projectsJson)
                context.getSharedPreferences("KnittingWidgetPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("knittingProjects", projectsJson)
                    .apply()

                val appWidgetManager = AppWidgetManager.getInstance(context)
                val ids = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, KnittingCounterWidget::class.java)
                )
                KnittingCounterWidget.updateAppWidget(context, appWidgetManager, *ids)
            } catch (e: Exception) {
                // Логирование ошибки
            }
        }

        @JavascriptInterface
        fun openPdf(pdfDataUrl: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                val uri: Uri = if (pdfDataUrl.startsWith("data:application/pdf;base64,")) {
                    // Извлекаем base64 часть из data: URL
                    val base64Data = pdfDataUrl.substring("data:application/pdf;base64,".length)
                    // Декодируем base64 в байты
                    val pdfBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)

                    // Создаём временный файл
                    val file = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")
                    // Записываем байты в файл
                    FileOutputStream(file).use { it.write(pdfBytes) }

                    // Создаём URI через FileProvider
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } else if (pdfDataUrl.startsWith("blob:")) {
                    val inputStream = context.contentResolver.openInputStream(Uri.parse(pdfDataUrl))
                        ?: throw Exception("Blob stream is null")
                    val file = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    inputStream.close()
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } else {
                    // Обработка обычного URI (если он есть)
                    Uri.parse(pdfDataUrl)
                }

                intent.setDataAndType(uri, "application/pdf")
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace() // Логируем ошибку для отладки
                (context as? Activity)?.runOnUiThread {
                    android.widget.Toast.makeText(
                        context,
                        "Нет приложения для просмотра PDF или ошибка открытия",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
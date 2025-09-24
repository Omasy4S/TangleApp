// src/main/java/com/example/app/WidgetConfigActivity.kt
package com.example.app

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray

class WidgetConfigActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var configContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContentView(R.layout.widget_config_layout)
        configContainer = findViewById(R.id.config_container)

        // Читаем данные из SharedPreferences
        val prefs = getSharedPreferences("KnittingWidgetPrefs", Context.MODE_PRIVATE)
        val projectsJson = prefs.getString("knittingProjects", "[]") ?: "[]"

        loadCountersFromJson(projectsJson)
    }

    private fun loadCountersFromJson(projectsJson: String) {
        runOnUiThread {
            configContainer.removeAllViews()

            if (projectsJson == "[]" || projectsJson.isBlank()) {
                showEmptyMessage()
                return@runOnUiThread
            }

            try {
                val projects = JSONArray(projectsJson)
                var hasCounters = false

                for (i in 0 until projects.length()) {
                    val project = projects.getJSONObject(i)
                    val projectName = project.getString("name")
                    val counters = project.getJSONArray("counters")

                    for (j in 0 until counters.length()) {
                        hasCounters = true
                        val counter = counters.getJSONObject(j)
                        val counterName = counter.getString("name")
                        val fullCounterName = "$projectName: $counterName"
                        val counterId = "$i-$j"

                        val item = LayoutInflater.from(this)
                            .inflate(R.layout.widget_config_item, configContainer, false) as LinearLayout
                        val textView = item.findViewById<TextView>(R.id.counter_name)
                        textView.text = fullCounterName

                        val layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        layoutParams.setMargins(0, 0, 0, 8)
                        item.layoutParams = layoutParams

                        item.setOnClickListener {
                            saveSelection(counterId, projectsJson)
                        }

                        configContainer.addView(item)
                    }
                }

                if (!hasCounters) {
                    showEmptyMessage()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                showEmptyMessage()
            }
        }
    }

    private fun showEmptyMessage() {
        val item = LayoutInflater.from(this)
            .inflate(R.layout.widget_config_item, configContainer, false) as LinearLayout
        val textView = item.findViewById<TextView>(R.id.counter_name)
        textView.text = "Создайте проект в приложении"
        textView.setTextColor(0xFF9E9E9E.toInt())
        textView.isEnabled = false
        configContainer.addView(item)
    }

    private fun saveSelection(counterId: String, projectsJson: String) {
        val prefs = getSharedPreferences("KnittingWidgetPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("widget_counter_id_$appWidgetId", counterId)
            .putString("knittingProjects", projectsJson)
            .apply()

        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }
}
// src/main/java/com/example/app/WidgetConfigActivity.kt
package com.example.app

import android.app.Activity
import android.appwidget.AppWidgetManager
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
        val projectsJson = getWidgetPrefs().getString(PrefKeys.KNITTING_PROJECTS, "[]") ?: "[]"
        loadCountersFromJson(projectsJson)
    }

    private fun loadCountersFromJson(projectsJson: String) {
        runOnUiThread {
            val container = findViewById<LinearLayout>(R.id.config_container)
            container.removeAllViews()

            if (projectsJson == "[]" || projectsJson.isBlank()) {
                showEmptyMessage(container)
                return@runOnUiThread
            }

            try {
                val projects = JSONArray(projectsJson)
                var hasCounters = false

                for (i in 0 until projects.length()) {
                    val project = projects.getJSONObject(i)
                    val counters = project.getJSONArray("counters")

                    for (j in 0 until counters.length()) {
                        hasCounters = true
                        val counter = counters.getJSONObject(j)
                        val fullCounterName = "${project.getString("name")}: ${counter.getString("name")}"
                        val counterId = "$i-$j"

                        val item = LayoutInflater.from(this)
                            .inflate(R.layout.widget_config_item, container, false) as LinearLayout
                        item.findViewById<TextView>(R.id.counter_name).text = fullCounterName

                        item.setOnClickListener {
                            saveSelection(counterId, projectsJson)
                        }
                        container.addView(item)
                    }
                }

                if (!hasCounters) showEmptyMessage(container)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                showEmptyMessage(container)
            }
        }
    }

    private fun showEmptyMessage(container: LinearLayout) {
        val item = LayoutInflater.from(this)
            .inflate(R.layout.widget_config_item, container, false) as LinearLayout
        val tv = item.findViewById<TextView>(R.id.counter_name)
        tv.text = "Создайте проект в приложении"
        tv.setTextColor(0xFF9E9E9E.toInt())
        tv.isEnabled = false
        container.addView(item)
    }

    private fun saveSelection(counterId: String, projectsJson: String) {
        getWidgetPrefs().edit {
            putString("${PrefKeys.WIDGET_COUNTER_ID_PREFIX}$appWidgetId", counterId)
            putString(PrefKeys.KNITTING_PROJECTS, projectsJson)
        }

        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        finish()
    }
}
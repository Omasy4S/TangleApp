// src/main/java/com/example/app/KnittingCounterWidget.kt
package com.example.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.json.JSONArray

class KnittingCounterWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (ACTION_UPDATE_STITCH == action || ACTION_UPDATE_ROW == action) {
            val appWidgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
            val change = intent.getIntExtra(EXTRA_CHANGE, 0)
            val isStitch = (ACTION_UPDATE_STITCH == action)

            if (appWidgetId != -1) {
                updateCounterValue(context, appWidgetId, isStitch, change)
                updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
            }
        }
    }

    private fun updateCounterValue(context: Context, appWidgetId: Int, isStitch: Boolean, change: Int) {
        val prefs = context.getSharedPreferences("KnittingWidgetPrefs", Context.MODE_PRIVATE)
        val counterId = prefs.getString("widget_counter_id_$appWidgetId", "") ?: return
        if (counterId.isEmpty()) return

        val projectsJson = prefs.getString("knittingProjects", "[]") ?: "[]"
        try {
            val projects = JSONArray(projectsJson)
            val (projectIndex, counterIndex) = counterId.split("-").map { it.toInt() }
            val project = projects.getJSONObject(projectIndex)
            val counters = project.getJSONArray("counters")
            val counter = counters.getJSONObject(counterIndex)

            val currentValue = if (isStitch) counter.getInt("stitches") else counter.getInt("rows")
            val newValue = (currentValue + change).coerceAtLeast(0)

            counter.put(if (isStitch) "stitches" else "rows", newValue)
            counters.put(counterIndex, counter)
            project.put("counters", counters)
            projects.put(projectIndex, project)

            val updatedJson = projects.toString()
            prefs.edit().putString("knittingProjects", updatedJson).apply()

            // Синхронизируем с приложением
            context.sendBroadcast(Intent("com.example.app.WIDGET_UPDATE").apply {
                putExtra("knittingProjects", updatedJson)
            })

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val ACTION_UPDATE_STITCH = "com.example.app.ACTION_UPDATE_STITCH"
        const val ACTION_UPDATE_ROW = "com.example.app.ACTION_UPDATE_ROW"
        const val EXTRA_WIDGET_ID = "EXTRA_WIDGET_ID"
        const val EXTRA_CHANGE = "EXTRA_CHANGE"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            vararg appWidgetIds: Int
        ) {
            for (appWidgetId in appWidgetIds) {
                val prefs = context.getSharedPreferences("KnittingWidgetPrefs", Context.MODE_PRIVATE)
                val counterId = prefs.getString("widget_counter_id_$appWidgetId", "") ?: ""
                var counterName = "Счётчик"
                var stitches = 0
                var rows = 0

                if (counterId.isNotEmpty()) {
                    val projectsJson = prefs.getString("knittingProjects", "[]") ?: "[]"
                    try {
                        val projects = JSONArray(projectsJson)
                        val (projectIndex, counterIndex) = counterId.split("-").map { it.toInt() }
                        val project = projects.getJSONObject(projectIndex)
                        val counter = project.getJSONArray("counters").getJSONObject(counterIndex)
                        counterName = "${project.getString("name")}: ${counter.getString("name")}"
                        stitches = counter.getInt("stitches")
                        rows = counter.getInt("rows")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val views = RemoteViews(context.packageName, R.layout.widget_counter)
                views.setTextViewText(R.id.widget_counter_name, counterName)
                views.setTextViewText(R.id.widget_stitch_value, stitches.toString())
                views.setTextViewText(R.id.widget_row_value, rows.toString())

                // Настройка кнопок
                val btnStitchInc = Intent(context, KnittingCounterWidget::class.java).apply {
                    action = ACTION_UPDATE_STITCH
                    putExtra(EXTRA_WIDGET_ID, appWidgetId)
                    putExtra(EXTRA_CHANGE, 1)
                }
                views.setOnClickPendingIntent(
                    R.id.widget_btn_stitch_inc,
                    PendingIntent.getBroadcast(context, appWidgetId * 4 + 0, btnStitchInc, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )

                val btnStitchDec = Intent(context, KnittingCounterWidget::class.java).apply {
                    action = ACTION_UPDATE_STITCH
                    putExtra(EXTRA_WIDGET_ID, appWidgetId)
                    putExtra(EXTRA_CHANGE, -1)
                }
                views.setOnClickPendingIntent(
                    R.id.widget_btn_stitch_dec,
                    PendingIntent.getBroadcast(context, appWidgetId * 4 + 1, btnStitchDec, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )

                val btnRowInc = Intent(context, KnittingCounterWidget::class.java).apply {
                    action = ACTION_UPDATE_ROW
                    putExtra(EXTRA_WIDGET_ID, appWidgetId)
                    putExtra(EXTRA_CHANGE, 1)
                }
                views.setOnClickPendingIntent(
                    R.id.widget_btn_row_inc,
                    PendingIntent.getBroadcast(context, appWidgetId * 4 + 2, btnRowInc, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )

                val btnRowDec = Intent(context, KnittingCounterWidget::class.java).apply {
                    action = ACTION_UPDATE_ROW
                    putExtra(EXTRA_WIDGET_ID, appWidgetId)
                    putExtra(EXTRA_CHANGE, -1)
                }
                views.setOnClickPendingIntent(
                    R.id.widget_btn_row_dec,
                    PendingIntent.getBroadcast(context, appWidgetId * 4 + 3, btnRowDec, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
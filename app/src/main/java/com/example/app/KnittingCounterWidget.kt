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
        appWidgetIds.forEach { id ->
            updateAppWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_UPDATE_STITCH, ACTION_UPDATE_ROW -> {
                val appWidgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                val change = intent.getIntExtra(EXTRA_CHANGE, 0)
                val isStitch = intent.action == ACTION_UPDATE_STITCH
                if (appWidgetId != -1) {
                    updateCounterValue(context, appWidgetId, isStitch, change)
                    updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                }
            }
        }
    }

    private fun updateCounterValue(
        context: Context,
        appWidgetId: Int,
        isStitch: Boolean,
        change: Int
    ) {
        val prefs = context.getWidgetPrefs()
        val counterId = prefs.getString("${PrefKeys.WIDGET_COUNTER_ID_PREFIX}$appWidgetId", "") ?: return
        if (counterId.isEmpty()) return

        val projectsJson = prefs.getString(PrefKeys.KNITTING_PROJECTS, "[]") ?: "[]"
        try {
            val projects = JSONArray(projectsJson)
            val ids = counterId.split("-").map { it.toInt() }
            if (ids.size != 2) return

            val (projectIndex, counterIndex) = ids
            if (projectIndex >= projects.length() || counterIndex < 0) return

            val project = projects.getJSONObject(projectIndex)
            val counters = project.getJSONArray("counters")

            if (counterIndex >= counters.length()) return
            val counter = counters.getJSONObject(counterIndex)

            val key = if (isStitch) "stitches" else "rows"
            val currentValue = counter.getInt(key)
            val newValue = (currentValue + change).coerceAtLeast(0)
            counter.put(key, newValue)

            counters.put(counterIndex, counter)
            project.put("counters", counters)
            projects.put(projectIndex, project)

            val updatedJson = projects.toString()
            prefs.edit { putString(PrefKeys.KNITTING_PROJECTS, updatedJson) }

            // Отправляем широковещательное сообщение для обновления приложения
            context.sendBroadcast(Intent(ACTION_WIDGET_UPDATE).apply {
                putExtra("knittingProjects", updatedJson)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val ACTION_UPDATE_STITCH = "com.example.app.ACTION_UPDATE_STITCH"
        const val ACTION_UPDATE_ROW = "com.example.app.ACTION_UPDATE_ROW"
        const val ACTION_WIDGET_UPDATE = "com.example.app.WIDGET_UPDATE"
        const val EXTRA_WIDGET_ID = "EXTRA_WIDGET_ID"
        const val EXTRA_CHANGE = "EXTRA_CHANGE"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            vararg appWidgetIds: Int
        ) {
            appWidgetIds.forEach { appWidgetId ->
                val prefs = context.getWidgetPrefs()
                val counterId = prefs.getString("${PrefKeys.WIDGET_COUNTER_ID_PREFIX}$appWidgetId", "") ?: ""

                var counterName = "Счётчик"
                var stitches = 0
                var rows = 0

                if (counterId.isNotEmpty()) {
                    val projectsJson = prefs.getString(PrefKeys.KNITTING_PROJECTS, "[]") ?: "[]"
                    try {
                        val projects = JSONArray(projectsJson)
                        val ids = counterId.split("-").map { it.toInt() }
                        if (ids.size == 2) {
                            val (projectIndex, counterIndex) = ids
                            if (projectIndex < projects.length() && counterIndex >= 0) {
                                val project = projects.getJSONObject(projectIndex)
                                val counters = project.getJSONArray("counters")
                                if (counterIndex < counters.length()) {
                                    val counter = counters.getJSONObject(counterIndex)
                                    counterName = "${project.getString("name")}: ${counter.getString("name")}"
                                    stitches = counter.getInt("stitches")
                                    rows = counter.getInt("rows")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val views = RemoteViews(context.packageName, R.layout.widget_counter)
                views.setTextViewText(R.id.widget_counter_name, counterName)
                views.setTextViewText(R.id.widget_stitch_value, stitches.toString())
                views.setTextViewText(R.id.widget_row_value, rows.toString())

                fun createPendingIntent(action: String, change: Int, requestCode: Int): PendingIntent {
                    val intent = Intent(context, KnittingCounterWidget::class.java).apply {
                        this.action = action
                        putExtra(EXTRA_WIDGET_ID, appWidgetId)
                        putExtra(EXTRA_CHANGE, change)
                    }
                    return PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }

                views.setOnClickPendingIntent(R.id.widget_btn_stitch_inc,
                    createPendingIntent(ACTION_UPDATE_STITCH, 1, appWidgetId * 4))
                views.setOnClickPendingIntent(R.id.widget_btn_stitch_dec,
                    createPendingIntent(ACTION_UPDATE_STITCH, -1, appWidgetId * 4 + 1))
                views.setOnClickPendingIntent(R.id.widget_btn_row_inc,
                    createPendingIntent(ACTION_UPDATE_ROW, 1, appWidgetId * 4 + 2))
                views.setOnClickPendingIntent(R.id.widget_btn_row_dec,
                    createPendingIntent(ACTION_UPDATE_ROW, -1, appWidgetId * 4 + 3))

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
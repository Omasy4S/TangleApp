package com.example.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
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
            ACTION_ADD_COUNTER -> {
                val appWidgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                if (appWidgetId != -1) {
                    addNewCounterToCurrentProject(context, appWidgetId)
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
            context.sendBroadcast(Intent(ACTION_WIDGET_UPDATE).apply {
                putExtra("knittingProjects", updatedJson)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addNewCounterToCurrentProject(context: Context, appWidgetId: Int) {
        val prefs = context.getWidgetPrefs()
        val counterId = prefs.getString("${PrefKeys.WIDGET_COUNTER_ID_PREFIX}$appWidgetId", "") ?: return
        if (counterId.isEmpty()) return

        val projectsJson = prefs.getString(PrefKeys.KNITTING_PROJECTS, "[]") ?: "[]"
        try {
            val projects = JSONArray(projectsJson)
            val ids = counterId.split("-").map { it.toInt() }
            if (ids.size != 2) return
            val projectIndex = ids[0]
            if (projectIndex >= projects.length()) return

            val project = projects.getJSONObject(projectIndex)
            val counters = project.getJSONArray("counters")

            // Создаём новый счётчик
            val newCounter = org.json.JSONObject().apply {
                put("name", "Новый счётчик")
                put("stitches", 0)
                put("rows", 0)
            }
            counters.put(newCounter)
            val newCounterIndex = counters.length() - 1

            // Обновляем проект и привязку виджета
            project.put("counters", counters)
            projects.put(projectIndex, project)

            val newCounterId = "$projectIndex-$newCounterIndex"
            prefs.edit {
                putString("${PrefKeys.WIDGET_COUNTER_ID_PREFIX}$appWidgetId", newCounterId)
                putString(PrefKeys.KNITTING_PROJECTS, projects.toString())
            }

            // Уведомляем приложение (если оно запущено)
            context.sendBroadcast(Intent(ACTION_WIDGET_UPDATE).apply {
                putExtra("knittingProjects", projects.toString())
            })

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val ACTION_UPDATE_STITCH = "com.example.app.ACTION_UPDATE_STITCH"
        const val ACTION_UPDATE_ROW = "com.example.app.ACTION_UPDATE_ROW"
        const val ACTION_ADD_COUNTER = "com.example.app.ACTION_ADD_COUNTER"
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

                // === Выбор layout по размеру ===
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

                val layoutRes = when {
                    minWidth >= 300 || minHeight >= 200 -> R.layout.widget_counter_large
                    minWidth >= 200 || minHeight >= 150 -> R.layout.widget_counter_medium
                    else -> R.layout.widget_counter
                }

                val views = RemoteViews(context.packageName, layoutRes)
                views.setTextViewText(R.id.widget_counter_name, counterName)
                views.setTextViewText(R.id.widget_stitch_value, stitches.toString())
                views.setTextViewText(R.id.widget_row_value, rows.toString())

                // === Клик по названию: выбор счётчика ===
                val configIntent = Intent(context, WidgetConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val configPendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    configIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_counter_name, configPendingIntent)

                // === Клик по плюсику: добавить счётчик ===
                val addCounterIntent = Intent(context, KnittingCounterWidget::class.java).apply {
                    action = ACTION_ADD_COUNTER
                    putExtra(EXTRA_WIDGET_ID, appWidgetId)
                }
                val addCounterPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 10 + 5,
                    addCounterIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_add_counter, addCounterPendingIntent)

                // === Кнопки изменения значений ===
                fun createPendingIntent(action: String, change: Int, requestCode: Int): PendingIntent {
                    val intent = Intent(context, KnittingCounterWidget::class.java).apply {
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
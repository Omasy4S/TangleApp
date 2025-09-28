package com.example.app

import Counter
import KnittingProject
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
        if (appWidgetId == -1) return

        when (intent.action) {
            ACTION_UPDATE_STITCH, ACTION_UPDATE_ROW -> {
                val change = intent.getIntExtra(EXTRA_CHANGE, 0)
                val isStitch = intent.action == ACTION_UPDATE_STITCH
                updateCounterValue(context, appWidgetId, isStitch, change)
                updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
            }
            ACTION_ADD_COUNTER -> {
                addNewCounterToCurrentProject(context, appWidgetId)
                updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
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

        val projects = loadProjects(prefs) ?: return
        val (projectIndex, counterIndex) = parseCounterId(counterId) ?: return

        if (projectIndex !in projects.indices || counterIndex !in projects[projectIndex].counters.indices) return

        val counter = projects[projectIndex].counters[counterIndex]
        val newValue = if (isStitch) {
            (counter.stitches + change).coerceAtLeast(0).also { counter.stitches = it }
        } else {
            (counter.rows + change).coerceAtLeast(0).also { counter.rows = it }
        }

        saveProjects(prefs, projects)
        notifyApp(context, projects)
    }

    private fun addNewCounterToCurrentProject(context: Context, appWidgetId: Int) {
        val prefs = context.getWidgetPrefs()
        val counterId = prefs.getString("${PrefKeys.WIDGET_COUNTER_ID_PREFIX}$appWidgetId", "") ?: return
        if (counterId.isEmpty()) return

        val projects = loadProjects(prefs) ?: return
        val (projectIndex, _) = parseCounterId(counterId) ?: return

        if (projectIndex !in projects.indices) return

        val newCounter = Counter("Новый счётчик")
        projects[projectIndex].counters.add(newCounter)
        val newCounterIndex = projects[projectIndex].counters.lastIndex
        val newCounterId = "$projectIndex-$newCounterIndex"

        prefs.edit {
            putString("${PrefKeys.WIDGET_COUNTER_ID_PREFIX}$appWidgetId", newCounterId)
            putString(PrefKeys.KNITTING_PROJECTS, Gson().toJson(projects))
        }

        notifyApp(context, projects)
    }

    // === Вспомогательные функции ===

    private fun loadProjects(prefs: android.content.SharedPreferences): MutableList<KnittingProject>? {
        return try {
            val json = prefs.getString(PrefKeys.KNITTING_PROJECTS, "[]") ?: "[]"
            val type = object : TypeToken<MutableList<KnittingProject>>() {}.type
            Gson().fromJson<MutableList<KnittingProject>>(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveProjects(prefs: android.content.SharedPreferences, projects: List<KnittingProject>) {
        prefs.edit {
            putString(PrefKeys.KNITTING_PROJECTS, Gson().toJson(projects))
        }
    }

    private fun notifyApp(context: Context, projects: List<KnittingProject>) {
        context.sendBroadcast(Intent(ACTION_WIDGET_UPDATE).apply {
            putExtra("knittingProjects", Gson().toJson(projects))
        })
    }

    private fun parseCounterId(id: String): Pair<Int, Int>? {
        return try {
            val parts = id.split("-")
            if (parts.size == 2) Pair(parts[0].toInt(), parts[1].toInt()) else null
        } catch (e: NumberFormatException) {
            null
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
                    val projects = try {
                        val json = prefs.getString(PrefKeys.KNITTING_PROJECTS, "[]") ?: "[]"
                        val type = object : TypeToken<List<KnittingProject>>() {}.type
                        Gson().fromJson<List<KnittingProject>>(json, type)
                    } catch (e: Exception) {
                        null
                    }

                    val (projectIndex, counterIndex) = runCatching {
                        val parts = counterId.split("-")
                        Pair(parts[0].toInt(), parts[1].toInt())
                    }.getOrNull() ?: Pair(-1, -1)

                    if (projects != null && projectIndex in projects.indices && counterIndex in projects[projectIndex].counters.indices) {
                        val project = projects[projectIndex]
                        val counter = project.counters[counterIndex]
                        counterName = "${project.name}: ${counter.name}"
                        stitches = counter.stitches
                        rows = counter.rows
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

                // === PendingIntent: выбор счётчика ===
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

                // === PendingIntent: добавить счётчик ===
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

                // === Кнопки изменения ===
                fun createPendingIntent(actionName: String, change: Int, requestCode: Int): PendingIntent {
                    val intent = Intent(context, KnittingCounterWidget::class.java).apply {
                        action = actionName
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
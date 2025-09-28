package com.example.app

import KnittingProject
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import com.example.app.databinding.WidgetConfigItemBinding
import com.example.app.databinding.WidgetConfigLayoutBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class WidgetConfigActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var binding: WidgetConfigLayoutBinding

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

        binding = WidgetConfigLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadAndDisplayCounters()
    }

    private fun loadAndDisplayCounters() {
        val prefs = getWidgetPrefs()
        val container = binding.configContainer
        container.removeAllViews()

        val projects = try {
            val json = prefs.getString(PrefKeys.KNITTING_PROJECTS, "[]") ?: "[]"
            val type = object : TypeToken<List<KnittingProject>>() {}.type
            Gson().fromJson<List<KnittingProject>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка загрузки проектов", Toast.LENGTH_SHORT).show()
            emptyList()
        }

        if (projects.isEmpty() || projects.all { it.counters.isEmpty() }) {
            showEmptyMessage(container)
            return
        }

        var hasCounters = false
        for ((projectIndex, project) in projects.withIndex()) {
            for ((counterIndex, counter) in project.counters.withIndex()) {
                hasCounters = true
                val fullCounterName = "${project.name}: ${counter.name}"
                val counterId = "$projectIndex-$counterIndex"

                val itemBinding = WidgetConfigItemBinding.inflate(layoutInflater, container, false)
                itemBinding.counterName.text = fullCounterName

                itemBinding.root.setOnClickListener {
                    saveSelection(counterId)
                    finishWithResult()
                }

                container.addView(itemBinding.root)
            }
        }

        if (!hasCounters) {
            showEmptyMessage(container)
        }
    }

    private fun showEmptyMessage(container: LinearLayout) {
        val itemBinding = WidgetConfigItemBinding.inflate(layoutInflater, container, false)
        with(itemBinding.counterName) {
            text = "Создайте проект в приложении"
            setTextColor(0xFF9E9E9E.toInt())
            isEnabled = false
        }
        itemBinding.root.isClickable = false
        itemBinding.root.alpha = 0.6f
        container.addView(itemBinding.root)
    }

    private fun saveSelection(counterId: String) {
        getWidgetPrefs().edit {
            putString("${PrefKeys.WIDGET_COUNTER_ID_PREFIX}$appWidgetId", counterId)
        }
        // Обновляем виджет
        AppWidgetManager.getInstance(this).also { appWidgetManager ->
            KnittingCounterWidget.updateAppWidget(this, appWidgetManager, appWidgetId)
        }
    }

    private fun finishWithResult() {
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        finish()
    }
}
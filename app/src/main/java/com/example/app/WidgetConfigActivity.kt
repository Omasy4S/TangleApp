package com.example.app

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import com.example.app.databinding.WidgetConfigItemBinding
import com.example.app.databinding.WidgetConfigLayoutBinding

/**
 * Activity для настройки виджета
 * Позволяет выбрать счётчик для отображения в виджете
 */
class WidgetConfigActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var binding: WidgetConfigLayoutBinding

    // ============================================================================
    // LIFECYCLE МЕТОДЫ
    // ============================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Получаем ID виджета
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Проверяем валидность ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Инициализируем UI
        binding = WidgetConfigLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Загружаем и отображаем счётчики
        loadAndDisplayCounters()
    }

    // ============================================================================
    // ЗАГРУЗКА И ОТОБРАЖЕНИЕ ДАННЫХ
    // ============================================================================

    /**
     * Загружает проекты и отображает список счётчиков
     */
    private fun loadAndDisplayCounters() {
        val prefs = getWidgetPrefs()
        val container = binding.configContainer
        container.removeAllViews()

        // Загружаем проекты
        val projects = prefs.loadProjects() ?: run {
            showError("Ошибка загрузки проектов")
            return
        }

        // Проверяем наличие счётчиков
        if (projects.isEmpty() || projects.all { it.counters.isEmpty() }) {
            showEmptyMessage(container)
            return
        }

        // Отображаем счётчики
        displayCounters(container, projects)
    }

    /**
     * Отображает список счётчиков
     * @param container Контейнер для элементов
     * @param projects Список проектов
     */
    private fun displayCounters(
        container: LinearLayout,
        projects: List<KnittingProject>
    ) {
        for ((projectIndex, project) in projects.withIndex()) {
            for ((counterIndex, counter) in project.counters.withIndex()) {
                val fullCounterName = "${project.name}: ${counter.name}"
                val counterId = createCounterId(projectIndex, counterIndex)
                
                addCounterItem(container, fullCounterName, counterId)
            }
        }
    }

    /**
     * Добавляет элемент счётчика в список
     * @param container Контейнер для элементов
     * @param counterName Полное название счётчика
     * @param counterId ID счётчика
     */
    private fun addCounterItem(
        container: LinearLayout,
        counterName: String,
        counterId: String
    ) {
        val itemBinding = WidgetConfigItemBinding.inflate(
            layoutInflater,
            container,
            false
        )
        
        itemBinding.counterName.text = counterName
        itemBinding.root.setOnClickListener {
            onCounterSelected(counterId)
        }
        
        container.addView(itemBinding.root)
    }

    /**
     * Отображает сообщение о пустом списке
     * @param container Контейнер для элементов
     */
    private fun showEmptyMessage(container: LinearLayout) {
        val itemBinding = WidgetConfigItemBinding.inflate(
            layoutInflater,
            container,
            false
        )
        
        with(itemBinding.counterName) {
            text = "Создайте проект в приложении"
            setTextColor(0xFF9E9E9E.toInt())
            isEnabled = false
        }
        
        itemBinding.root.apply {
            isClickable = false
            alpha = 0.6f
        }
        
        container.addView(itemBinding.root)
    }

    // ============================================================================
    // ОБРАБОТКА ВЫБОРА
    // ============================================================================

    /**
     * Обработка выбора счётчика
     * @param counterId ID выбранного счётчика
     */
    private fun onCounterSelected(counterId: String) {
        saveSelection(counterId)
        updateWidget()
        finishWithResult()
    }

    /**
     * Сохраняет выбор счётчика в SharedPreferences
     * @param counterId ID счётчика
     */
    private fun saveSelection(counterId: String) {
        getWidgetPrefs().safeEdit {
            putString(
                "${PrefKeys.WIDGET_COUNTER_ID_PREFIX}$appWidgetId",
                counterId
            )
        }
    }

    /**
     * Обновляет виджет после выбора счётчика
     */
    private fun updateWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        KnittingCounterWidget.updateAppWidget(
            this,
            appWidgetManager,
            appWidgetId
        )
    }

    /**
     * Завершает Activity с результатом
     */
    private fun finishWithResult() {
        val resultIntent = Intent().putExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            appWidgetId
        )
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    // ============================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================================================

    /**
     * Показывает сообщение об ошибке
     * @param message Текст сообщения
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

package com.example.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.google.gson.Gson

/**
 * Виджет счётчика для вязания
 * Позволяет отслеживать петли и ряды прямо с домашнего экрана
 */
class KnittingCounterWidget : AppWidgetProvider() {

    // ============================================================================
    // LIFECYCLE МЕТОДЫ
    // ============================================================================

    /**
     * Вызывается при обновлении виджета
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            updateAppWidget(context, appWidgetManager, widgetId)
        }
    }

    /**
     * Вызывается при изменении размера виджета
     */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    /**
     * Обработка действий виджета (клики по кнопкам)
     */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        val appWidgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
        if (appWidgetId == -1) return

        when (intent.action) {
            ACTION_UPDATE_STITCH, ACTION_UPDATE_ROW -> {
                val change = intent.getIntExtra(EXTRA_CHANGE, 0)
                val isStitch = intent.action == ACTION_UPDATE_STITCH
                handleCounterUpdate(context, appWidgetId, isStitch, change)
            }
            ACTION_ADD_COUNTER -> {
                handleAddCounter(context, appWidgetId)
            }
        }
    }

    // ============================================================================
    // ОБРАБОТКА ДЕЙСТВИЙ
    // ============================================================================

    /**
     * Обработка обновления значения счётчика
     * @param context Контекст приложения
     * @param appWidgetId ID виджета
     * @param isStitch true для петель, false для рядов
     * @param change Изменение значения (+1 или -1)
     */
    private fun handleCounterUpdate(
        context: Context,
        appWidgetId: Int,
        isStitch: Boolean,
        change: Int
    ) {
        val prefs = context.getWidgetPrefs()
        val counterId = prefs.getString(
            "${PrefKeys.WIDGET_COUNTER_ID_PREFIX}$appWidgetId",
            ""
        ) ?: return
        
        if (counterId.isEmpty()) return

        val projects = prefs.loadProjects() ?: return
        val (project, counter, _) = getCounterById(counterId, projects) ?: return

        // Обновляем значение
        if (isStitch) {
            counter.incrementStitches(change)
        } else {
            counter.incrementRows(change)
        }

        // Сохраняем и обновляем
        prefs.saveProjects(projects)
        notifyApp(context, projects)
        updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
    }

    /**
     * Обработка добавления нового счётчика
     * @param context Контекст приложения
     * @param appWidgetId ID виджета
     */
    private fun handleAddCounter(context: Context, appWidgetId: Int) {
        val prefs = context.getWidgetPrefs()
        val counterId = prefs.getString(
            "${PrefKeys.WIDGET_COUNTER_ID_PREFIX}$appWidgetId",
            ""
        ) ?: return
        
        if (counterId.isEmpty()) return

        val projects = prefs.loadProjects() ?: return
        val (projectIndex, _) = parseCounterId(counterId) ?: return
        
        if (projectIndex !in projects.indices) return
        val project = projects[projectIndex]

        // Добавляем новый счётчик
        val newCounterIndex = project.addCounter()
        val newCounterId = createCounterId(projectIndex, newCounterIndex)

        // Сохраняем
        prefs.safeEdit {
            putString("${PrefKeys.WIDGET_COUNTER_ID_PREFIX}$appWidgetId", newCounterId)
        }
        prefs.saveProjects(projects)
        notifyApp(context, projects)
        updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
    }

    /**
     * Отправка уведомления приложению об изменении данных
     * @param context Контекст приложения
     * @param projects Обновлённый список проектов
     */
    private fun notifyApp(context: Context, projects: List<KnittingProject>) {
        context.sendBroadcast(
            Intent(ACTION_WIDGET_UPDATE).apply {
                putExtra("knittingProjects", Gson().toJson(projects))
            }
        )
    }

    // ============================================================================
    // COMPANION OBJECT
    // ============================================================================

    companion object {
        // Действия виджета
        const val ACTION_UPDATE_STITCH = "com.example.app.ACTION_UPDATE_STITCH"
        const val ACTION_UPDATE_ROW = "com.example.app.ACTION_UPDATE_ROW"
        const val ACTION_ADD_COUNTER = "com.example.app.ACTION_ADD_COUNTER"
        const val ACTION_WIDGET_UPDATE = "com.example.app.WIDGET_UPDATE"
        
        // Extras для Intent
        const val EXTRA_WIDGET_ID = "EXTRA_WIDGET_ID"
        const val EXTRA_CHANGE = "EXTRA_CHANGE"

        /**
         * Обновление виджета(ов)
         * @param context Контекст приложения
         * @param appWidgetManager Менеджер виджетов
         * @param appWidgetIds ID виджетов для обновления
         */
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            vararg appWidgetIds: Int
        ) {
            appWidgetIds.forEach { appWidgetId ->
                val views = createWidgetViews(context, appWidgetManager, appWidgetId)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        /**
         * Создание RemoteViews для виджета
         * @param context Контекст приложения
         * @param appWidgetManager Менеджер виджетов
         * @param appWidgetId ID виджета
         * @return RemoteViews с настроенным UI
         */
        private fun createWidgetViews(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ): RemoteViews {
            // Загружаем данные счётчика
            val (counterName, stitches, rows) = loadCounterData(context, appWidgetId)

            // Выбираем layout в зависимости от размера виджета
            val layoutRes = selectLayout(appWidgetManager, appWidgetId)

            // Создаём RemoteViews
            val views = RemoteViews(context.packageName, layoutRes)
            
            // Устанавливаем данные
            views.setTextViewText(R.id.widget_counter_name, counterName)
            views.setTextViewText(R.id.widget_stitch_value, stitches.toString())
            views.setTextViewText(R.id.widget_row_value, rows.toString())

            // Настраиваем клики
            setupClickListeners(context, views, appWidgetId)

            return views
        }

        /**
         * Загрузка данных счётчика из SharedPreferences
         * @param context Контекст приложения
         * @param appWidgetId ID виджета
         * @return Triple(название, петли, ряды)
         */
        private fun loadCounterData(
            context: Context,
            appWidgetId: Int
        ): Triple<String, Int, Int> {
            val prefs = context.getWidgetPrefs()
            val counterId = prefs.getString(
                "${PrefKeys.WIDGET_COUNTER_ID_PREFIX}$appWidgetId",
                ""
            ) ?: ""

            if (counterId.isEmpty()) {
                return Triple("Счётчик", 0, 0)
            }

            val projects = prefs.loadProjects() ?: return Triple("Счётчик", 0, 0)
            val (project, counter, _) = getCounterById(counterId, projects)
                ?: return Triple("Счётчик", 0, 0)

            val counterName = "${project.name}: ${counter.name}"
            return Triple(counterName, counter.stitches, counter.rows)
        }

        /**
         * Выбор layout в зависимости от размера виджета
         * @param appWidgetManager Менеджер виджетов
         * @param appWidgetId ID виджета
         * @return ID ресурса layout
         */
        private fun selectLayout(
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ): Int {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

            return when {
                minWidth >= WidgetConstants.LARGE_WIDTH_THRESHOLD ||
                minHeight >= WidgetConstants.LARGE_HEIGHT_THRESHOLD ->
                    R.layout.widget_counter_large
                    
                minWidth >= WidgetConstants.MEDIUM_WIDTH_THRESHOLD ||
                minHeight >= WidgetConstants.MEDIUM_HEIGHT_THRESHOLD ->
                    R.layout.widget_counter_medium
                    
                else -> R.layout.widget_counter
            }
        }

        /**
         * Настройка обработчиков кликов для виджета
         * @param context Контекст приложения
         * @param views RemoteViews виджета
         * @param appWidgetId ID виджета
         */
        private fun setupClickListeners(
            context: Context,
            views: RemoteViews,
            appWidgetId: Int
        ) {
            // Клик по названию - открыть настройки
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

            // Кнопка добавления счётчика
            val addCounterIntent = Intent(context, KnittingCounterWidget::class.java).apply {
                action = ACTION_ADD_COUNTER
                putExtra(EXTRA_WIDGET_ID, appWidgetId)
            }
            val addCounterPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * WidgetConstants.REQUEST_CODE_CONFIG_MULTIPLIER +
                    WidgetConstants.REQUEST_CODE_ADD_COUNTER_OFFSET,
                addCounterIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_add_counter, addCounterPendingIntent)

            // Кнопки изменения значений
            views.setOnClickPendingIntent(
                R.id.widget_btn_stitch_inc,
                createCounterPendingIntent(
                    context, appWidgetId, ACTION_UPDATE_STITCH, 1,
                    appWidgetId * WidgetConstants.REQUEST_CODE_MULTIPLIER
                )
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_stitch_dec,
                createCounterPendingIntent(
                    context, appWidgetId, ACTION_UPDATE_STITCH, -1,
                    appWidgetId * WidgetConstants.REQUEST_CODE_MULTIPLIER + 1
                )
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_row_inc,
                createCounterPendingIntent(
                    context, appWidgetId, ACTION_UPDATE_ROW, 1,
                    appWidgetId * WidgetConstants.REQUEST_CODE_MULTIPLIER + 2
                )
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_row_dec,
                createCounterPendingIntent(
                    context, appWidgetId, ACTION_UPDATE_ROW, -1,
                    appWidgetId * WidgetConstants.REQUEST_CODE_MULTIPLIER + 3
                )
            )
        }

        /**
         * Создание PendingIntent для кнопки счётчика
         * @param context Контекст приложения
         * @param appWidgetId ID виджета
         * @param actionName Название действия
         * @param change Изменение значения (+1 или -1)
         * @param requestCode Уникальный код запроса
         * @return PendingIntent для кнопки
         */
        private fun createCounterPendingIntent(
            context: Context,
            appWidgetId: Int,
            actionName: String,
            change: Int,
            requestCode: Int
        ): PendingIntent {
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
    }
}

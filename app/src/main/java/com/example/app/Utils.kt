package com.example.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Константы для ключей SharedPreferences
 */
object PrefKeys {
    const val KNITTING_PROJECTS = "knittingProjects"
    const val WIDGET_COUNTER_ID_PREFIX = "widget_counter_id_"
    const val WIDGET_COLORS = "widgetColors"
}

/**
 * Константы для виджета
 */
object WidgetConstants {
    // Размеры виджета для выбора layout
    const val LARGE_WIDTH_THRESHOLD = 300
    const val LARGE_HEIGHT_THRESHOLD = 200
    const val MEDIUM_WIDTH_THRESHOLD = 200
    const val MEDIUM_HEIGHT_THRESHOLD = 150
    
    // Множители для request codes
    const val REQUEST_CODE_MULTIPLIER = 4
    const val REQUEST_CODE_ADD_COUNTER_OFFSET = 5
    const val REQUEST_CODE_CONFIG_MULTIPLIER = 10
}

/**
 * Extension function для получения SharedPreferences виджета
 */
fun Context.getWidgetPrefs(): SharedPreferences =
    getSharedPreferences("KnittingWidgetPrefs", Context.MODE_PRIVATE)

/**
 * Extension function для безопасного редактирования SharedPreferences
 * Автоматически применяет изменения
 */
fun SharedPreferences.safeEdit(block: SharedPreferences.Editor.() -> Unit) {
    edit().apply(block).apply()
}

/**
 * Extension function для загрузки проектов из SharedPreferences
 * @return Список проектов или null в случае ошибки
 */
fun SharedPreferences.loadProjects(): MutableList<KnittingProject>? =
    runCatching {
        val json = getString(PrefKeys.KNITTING_PROJECTS, "[]") ?: "[]"
        val type = object : TypeToken<MutableList<KnittingProject>>() {}.type
        Gson().fromJson<MutableList<KnittingProject>>(json, type) ?: mutableListOf()
    }.getOrNull()

/**
 * Extension function для сохранения проектов в SharedPreferences
 * @param projects Список проектов для сохранения
 */
fun SharedPreferences.saveProjects(projects: List<KnittingProject>) {
    safeEdit {
        putString(PrefKeys.KNITTING_PROJECTS, Gson().toJson(projects))
    }
}

/**
 * Парсинг ID счётчика в формате "projectIndex-counterIndex"
 * @param id Строка ID
 * @return Pair(projectIndex, counterIndex) или null если формат неверный
 */
fun parseCounterId(id: String): Pair<Int, Int>? =
    id.split("-")
        .takeIf { it.size == 2 }
        ?.let { parts ->
            val projectIndex = parts[0].toIntOrNull()
            val counterIndex = parts[1].toIntOrNull()
            if (projectIndex != null && counterIndex != null) {
                Pair(projectIndex, counterIndex)
            } else null
        }

/**
 * Создание ID счётчика из индексов
 * @param projectIndex Индекс проекта
 * @param counterIndex Индекс счётчика
 * @return Строка ID в формате "projectIndex-counterIndex"
 */
fun createCounterId(projectIndex: Int, counterIndex: Int): String =
    "$projectIndex-$counterIndex"

/**
 * Получение счётчика по ID
 * @param counterId ID счётчика в формате "projectIndex-counterIndex"
 * @param projects Список проектов
 * @return Triple(project, counter, indices) или null если не найден
 */
fun getCounterById(
    counterId: String,
    projects: List<KnittingProject>
): Triple<KnittingProject, Counter, Pair<Int, Int>>? {
    val (projectIndex, counterIndex) = parseCounterId(counterId) ?: return null
    
    if (projectIndex !in projects.indices) return null
    val project = projects[projectIndex]
    
    if (counterIndex !in project.counters.indices) return null
    val counter = project.counters[counterIndex]
    
    return Triple(project, counter, Pair(projectIndex, counterIndex))
}

/**
 * Data class для хранения цветов виджета
 */
data class WidgetColors(
    val primary: String = "#fcd9b8",
    val secondary: String = "#fef5ec",
    val accent: String = "#f8cdda",
    val dark: String = "#5d4a66",
    val light: String = "#fef7ff",
    val counter: String = "#ffe3b8",
    val counterBtn: String = "#fcd9b8"
)

/**
 * Extension function для загрузки цветов виджета
 * @return WidgetColors с сохранёнными цветами или цвета по умолчанию
 */
fun SharedPreferences.loadWidgetColors(): WidgetColors {
    val json = getString(PrefKeys.WIDGET_COLORS, null) ?: return WidgetColors()
    
    return runCatching {
        val gson = Gson()
        val map = gson.fromJson<Map<String, String>>(
            json,
            object : TypeToken<Map<String, String>>() {}.type
        )
        
        WidgetColors(
            primary = map["--primary"] ?: "#fcd9b8",
            secondary = map["--secondary"] ?: "#fef5ec",
            accent = map["--accent"] ?: "#f8cdda",
            dark = map["--dark"] ?: "#5d4a66",
            light = map["--light"] ?: "#fef7ff",
            counter = map["--counter"] ?: "#ffe3b8",
            counterBtn = map["--counter-btn"] ?: "#fcd9b8"
        )
    }.getOrElse { WidgetColors() }
}

/**
 * Конвертация HEX цвета в Android Color Int
 * @param hex HEX строка цвета (например "#fcd9b8")
 * @return Int представление цвета
 */
fun String.parseColor(): Int {
    return try {
        android.graphics.Color.parseColor(this)
    } catch (e: Exception) {
        android.graphics.Color.parseColor("#fcd9b8") // fallback
    }
}

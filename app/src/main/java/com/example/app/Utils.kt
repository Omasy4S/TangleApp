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

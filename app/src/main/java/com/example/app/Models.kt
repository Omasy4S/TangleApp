package com.example.app

/**
 * Модель счётчика для вязания
 * @param name Название счётчика
 * @param stitches Количество петель
 * @param rows Количество рядов
 */
data class Counter(
    val name: String,
    var stitches: Int = 0,
    var rows: Int = 0
) {
    /**
     * Увеличивает количество петель на заданное значение
     * @param amount Количество для увеличения (может быть отрицательным)
     * @return Новое количество петель (не меньше 0)
     */
    fun incrementStitches(amount: Int): Int {
        stitches = (stitches + amount).coerceAtLeast(0)
        return stitches
    }

    /**
     * Увеличивает количество рядов на заданное значение
     * @param amount Количество для увеличения (может быть отрицательным)
     * @return Новое количество рядов (не меньше 0)
     */
    fun incrementRows(amount: Int): Int {
        rows = (rows + amount).coerceAtLeast(0)
        return rows
    }
}

/**
 * Модель проекта вязания
 * @param name Название проекта
 * @param counters Список счётчиков в проекте
 */
data class KnittingProject(
    val name: String,
    val counters: MutableList<Counter> = mutableListOf()
) {
    /**
     * Добавляет новый счётчик в проект
     * @param counterName Название нового счётчика
     * @return Индекс добавленного счётчика
     */
    fun addCounter(counterName: String = "Новый счётчик"): Int {
        counters.add(Counter(counterName))
        return counters.lastIndex
    }

    /**
     * Получает счётчик по индексу безопасно
     * @param index Индекс счётчика
     * @return Counter или null если индекс невалидный
     */
    fun getCounterOrNull(index: Int): Counter? =
        counters.getOrNull(index)
}

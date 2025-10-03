package com.example.app

import android.content.Context
import android.content.SharedPreferences

object PrefKeys {
    const val KNITTING_PROJECTS = "knittingProjects"
    const val WIDGET_COUNTER_ID_PREFIX = "widget_counter_id_"
}

fun Context.getWidgetPrefs(): SharedPreferences =
    getSharedPreferences("KnittingWidgetPrefs", Context.MODE_PRIVATE)

fun SharedPreferences.safeEdit(block: SharedPreferences.Editor.() -> Unit) {
    edit().apply(block).apply()
}

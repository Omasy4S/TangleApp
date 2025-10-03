package com.example.app

import kotlinx.serialization.Serializable

@Serializable
data class Counter(
    val name: String,
    var stitches: Int = 0,
    var rows: Int = 0
)

@Serializable
data class KnittingProject(
    val name: String,
    val counters: MutableList<Counter> = mutableListOf()
)

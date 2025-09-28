data class KnittingProject(
    val name: String,
    val counters: MutableList<Counter> = mutableListOf()
)

data class Counter(
    val name: String,
    var stitches: Int = 0,
    var rows: Int = 0
)
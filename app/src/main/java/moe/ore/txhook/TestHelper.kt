package moe.ore.txhook

fun <E> List<E>.forEachL(block: (index: Int, v: E) -> Unit) {
    for (i in size - 1 downTo 0) {
        block(i, get(i))
    }
}
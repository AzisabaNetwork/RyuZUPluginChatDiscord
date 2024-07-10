package net.azisaba.ryuzupluginchatdiscord.util

object Functions {
    fun <T, R> memoize(durationMillis: Long, fn: (T) -> R) = MemoizeFunction(durationMillis, fn)
}

class MemoizeFunction<T, R>(private val durationMillis: Long = Long.MAX_VALUE, private val fn: (T) -> R) : (T) -> R {
    private val cache = mutableMapOf<T, Pair<Long, R>>()

    override operator fun invoke(t: T): R {
        val (time, value) = cache[t] ?: Pair(0L, null)
        @Suppress("UNCHECKED_CAST")
        return if (System.currentTimeMillis() - time > durationMillis) {
            val newValue = fn(t)
            cache[t] = Pair(System.currentTimeMillis(), newValue)
            newValue
        } else value ?: null as R
    }

    fun forgetAll() = cache.clear()

    fun forget(key: T) = cache.remove(key)
}

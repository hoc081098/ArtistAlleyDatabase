package com.thekeeperofpie.artistalleydatabase.compose.filter

enum class FilterIncludeExcludeState {
    DEFAULT, INCLUDE, EXCLUDE;

    companion object {

        fun <T: Any> toState(value: T, included: Collection<T>, excluded: Collection<T>) = when {
            included.contains(value) -> INCLUDE
            excluded.contains(value) -> EXCLUDE
            else -> DEFAULT
        }

        fun <Base, StateHolder, Comparison> applyFiltering(
            filters: List<StateHolder>,
            list: List<Base>,
            state: (StateHolder) -> FilterIncludeExcludeState,
            key: (StateHolder) -> Comparison,
            transform: (Base) -> List<Comparison>,
            transformIncludes: ((Base) -> List<Comparison>)? = null,
        ): List<Base> {
            val includes = filters.filter { state(it) == INCLUDE }.map(key)
            val excludes = filters.filter { state(it) == EXCLUDE }.map(key)
            if (includes.isEmpty() && excludes.isEmpty()) return list

            return list.filter {
                val target = transform(it)

                if (excludes.isNotEmpty() && !target.none(excludes::contains)) return@filter false

                val targetIncludes = transformIncludes?.invoke(it) ?: target
                includes.isEmpty() || targetIncludes.containsAll(includes)
            }
        }
    }

    fun next(): FilterIncludeExcludeState {
        val values = values()
        return values[(values.indexOf(this) + 1) % values.size]
    }
}

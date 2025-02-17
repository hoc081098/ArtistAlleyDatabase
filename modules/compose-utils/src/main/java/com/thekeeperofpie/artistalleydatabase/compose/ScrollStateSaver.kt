package com.thekeeperofpie.artistalleydatabase.compose

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface ScrollStateSaver {

    companion object {

        val STUB = object : ScrollStateSaver {
            override var position = 0
            override var offset = 0
        }

        @Composable
        operator fun invoke(): ScrollStateSaver {
            val position = rememberSaveable { mutableStateOf(0) }
            val offset = rememberSaveable { mutableStateOf(0) }
            return remember { ScrollStateSaverImpl(position, offset) }
        }

        @Composable
        fun scrollPositions() = rememberSaveable(saver = object :
            Saver<MutableMap<String, Pair<Int, Int>>, String> {
            override fun restore(value: String) =
                Json.decodeFromString<Map<String, Pair<Int, Int>>>(value)
                    .toMutableMap()

            override fun SaverScope.save(value: MutableMap<String, Pair<Int, Int>>) =
                Json.encodeToString(value)

        }) { mutableStateMapOf() }

        @Composable
        fun fromMap(key: String, map: MutableMap<String, Pair<Int, Int>>) = remember {
            object : ScrollStateSaver {
                override var position: Int
                    get() = map[key]?.first ?: 0
                    set(value) {
                        map[key] = map[key]?.copy(first = value) ?: (value to 0)
                    }
                override var offset: Int
                    get() = map[key]?.second ?: 0
                    set(value) {
                        map[key] = map[key]?.copy(second = value) ?: (value to 0)
                    }

            }
        }
    }

    var position: Int
    var offset: Int


    @Composable
    fun lazyListState() = rememberLazyListState(
        initialFirstVisibleItemIndex = position,
        initialFirstVisibleItemScrollOffset = offset,
    ).also {
        DisposableEffect(it) {
            onDispose { save(it) }
        }
    }

    private fun save(lazyListState: LazyListState) {
        position = lazyListState.firstVisibleItemIndex
        offset = lazyListState.firstVisibleItemScrollOffset
    }

    class ScrollStateSaverImpl(
        position: MutableState<Int>,
        offset: MutableState<Int>,
    ) : ScrollStateSaver {
        override var position by position
        override var offset by offset
    }
}

package com.thekeeperofpie.artistalleydatabase.form

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.thekeeperofpie.artistalleydatabase.compose.observableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.util.function.UnaryOperator

sealed class EntrySection(lockState: LockState? = null) {

    private var lockState_ by mutableStateOf(lockState)
    var lockStateFlow = MutableStateFlow(lockState)
    var lockState: LockState? = lockState
        get() = lockState_
        set(value) {
            field = value
            wasEverDifferent = wasEverDifferent || value == LockState.DIFFERENT
            lockState_ = value
            lockStateFlow.tryEmit(value)
        }

    private var wasEverDifferent = false

    enum class LockState(val editable: Boolean) {
        LOCKED(editable = false),
        UNLOCKED(editable = true),
        DIFFERENT(editable = true),
        ;

        fun toSerializedValue() = when (this) {
            LOCKED -> true
            UNLOCKED -> false
            DIFFERENT -> null
        }

        companion object {
            fun from(value: Boolean?) = value?.let {
                if (it) LOCKED else UNLOCKED
            } ?: DIFFERENT
        }
    }

    open fun rotateLockState() {
        lockState = when (lockState) {
            LockState.LOCKED -> LockState.UNLOCKED
            LockState.UNLOCKED -> if (wasEverDifferent) LockState.DIFFERENT else LockState.LOCKED
            LockState.DIFFERENT -> LockState.LOCKED
            null -> null
        }
    }

    class MultiText(
        @StringRes val headerZero: Int,
        @StringRes val headerOne: Int,
        @StringRes val headerMany: Int,
        initialPendingValue: String = "",
        lockState: LockState? = null
    ) : EntrySection(lockState) {
        val contents = mutableStateListOf<Entry>()
        private var contentUpdates = MutableStateFlow(emptyList<Entry>())

        private var pendingValueUpdates = MutableStateFlow("")
        var pendingValue by observableStateOf(
            initialPendingValue,
            pendingValueUpdates::tryEmit
        )

        private val predictionChosenUpdates = MutableSharedFlow<Entry>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val predictionChosen = predictionChosenUpdates.asSharedFlow()

        // TODO: Predictions for existing prefilled fields
        var predictions by mutableStateOf(emptyList<Entry>())

        fun pendingEntry() = Entry.Custom(pendingValue)

        fun contentSize() = contents.size

        fun content(index: Int) = contents[index]

        fun onPredictionChosen(index: Int) {
            val entry = predictions[index]
            addContent(entry)
            pendingValue = ""
            predictionChosenUpdates.tryEmit(entry)
        }

        fun setContents(entries: Collection<Entry>, lockState: LockState?) {
            contents.clear()
            contents.addAll(entries)
            contentUpdates.tryEmit(contents.toList())
            this.lockState = lockState
        }

        fun replaceContents(operator: UnaryOperator<Entry>) {
            contents.replaceAll(operator)
            contentUpdates.tryEmit(contents.toList())
        }

        fun replaceContent(entry: Entry.Prefilled<*>) {
            val index = indexOf(entry)
            if (index >= 0) {
                contents[index] = entry
            }
            contentUpdates.tryEmit(contents.toList())
        }

        fun addContent(index: Int, entry: Entry) {
            contents.add(index, entry)
            contentUpdates.tryEmit(contents.toList())
        }

        fun addContent(entry: Entry) {
            contents += entry
            contentUpdates.tryEmit(contents.toList())
        }

        fun addOrReplaceContent(entry: Entry.Prefilled<*>) {
            val index = indexOf(entry)
            if (index >= 0) {
                contents[index] = entry
            } else {
                contents += entry
            }
            contentUpdates.tryEmit(contents.toList())
        }

        fun addOrReplaceContents(entries: Collection<Entry.Prefilled<*>>) {
            entries.forEach {
                val index = indexOf(it)
                if (index >= 0) {
                    contents[index] = it
                } else {
                    contents += it
                }
            }
            contentUpdates.tryEmit(contents.toList())
        }

        fun removeContentAt(index: Int) {
            contents.removeAt(index)
            contentUpdates.tryEmit(contents.toList())
        }

        fun swapContent(firstIndex: Int, secondIndex: Int) {
            val oldValue = contents[firstIndex]
            contents[firstIndex] = contents[secondIndex]
            contents[secondIndex] = oldValue
            contentUpdates.tryEmit(contents.toList())
        }

        fun setContent(index: Int, entry: Entry) {
            contents[index] = entry
            contentUpdates.tryEmit(contents.toList())
        }

        inline fun forEachContentIndexed(action: (index: Int, Entry) -> Unit) =
            contents.forEachIndexed(action)

        fun contentUpdates() = contentUpdates.asStateFlow()

        fun valueUpdates() = pendingValueUpdates.asStateFlow()

        fun finalContents() = (contents + Entry.Custom(pendingValue.trim()))
            .filterNot { it.serializedValue.isBlank() }

        suspend fun subscribePredictions(
            localCall: suspend (String) -> List<Flow<Entry?>>,
            networkCall: suspend (query: String) -> Flow<List<Entry>> = {
                flowOf(emptyList())
            },
        ) {
            @Suppress("OPT_IN_USAGE")
            valueUpdates()
                .flatMapLatest { query ->
                    val localFlows = localCall(query)
                    val database = if (localFlows.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        combine(localFlows) { it.toList() }
                    }
                    val aniList = if (query.isBlank()) flowOf(emptyList()) else networkCall(query)
                    combine(database, aniList) { local, network ->
                        local.filterNotNull().toMutableList().apply {
                            removeIf { source ->
                                network.any { target -> target.text.trim() == source.text.trim() }
                            }
                        } + network
                    }
                }
                .collectLatest {
                    withContext(Dispatchers.Main) {
                        predictions = it.toMutableList()
                    }
                }
        }

        private fun indexOf(entry: Entry.Prefilled<*>) =
            contents.indexOfFirst { it is Entry.Prefilled<*> && it.id == entry.id }

        sealed class Entry(
            val text: String,
            val trailingIcon: ImageVector? = null,
            @StringRes val trailingIconContentDescription: Int? = null,
            val serializedValue: String = text,
            val searchableValue: String = text,
        ) {
            class Custom(text: String, val id: String? = null, trailingIcon: ImageVector? = null) :
                Entry(text, trailingIcon)

            object Different : Entry(
                text = "",
                trailingIcon = Icons.Default.ViewKanban,
                trailingIconContentDescription =
                R.string.different_indicator_content_description
            )

            class Prefilled<T>(
                val value: T,
                val id: String,
                text: String,
                val image: String? = null,
                val imageLink: String? = null,
                val titleText: String = text,
                val subtitleText: String? = null,
                trailingIcon: ImageVector? = null,
                trailingIconContentDescription: Int? = null,
                serializedValue: String,
                searchableValue: String,
            ) : Entry(
                text,
                trailingIcon,
                trailingIconContentDescription,
                serializedValue,
                searchableValue
            )
        }
    }

    class LongText(
        @StringRes val headerRes: Int,
        initialPendingValue: String = "",
        lockState: LockState? = null,
    ) : EntrySection(lockState) {
        var value by mutableStateOf(initialPendingValue)

        fun setContents(value: String?, lockState: LockState?) {
            this.value = value.orEmpty()
            this.lockState = lockState
        }
    }

    open class Dropdown(
        @StringRes val headerRes: Int,
        @StringRes val arrowContentDescription: Int,
        var options: SnapshotStateList<Item> = mutableStateListOf(),
        lockState: LockState? = null,
    ) : EntrySection(lockState) {
        var expanded by mutableStateOf(false)
        var selectedIndex by mutableStateOf(0)

        open fun selectedItem() = options[selectedIndex]

        interface Item {

            val hasCustomView: Boolean

            @Composable
            fun fieldText(): String

            @Composable
            fun DropdownItemText()

            @Composable
            fun Content(lockState: LockState?) {
            }

            class Basic<T>(val value: T, @StringRes val textRes: Int) : Item {

                override val hasCustomView = false

                @Composable
                override fun fieldText() = stringResource(textRes)

                @Composable
                override fun DropdownItemText() = Text(fieldText())
            }
        }
    }
}
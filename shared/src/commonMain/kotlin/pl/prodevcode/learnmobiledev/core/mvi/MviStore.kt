package pl.prodevcode.learnmobiledev.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * The MVI engine: an intent queue, a pure reduction, a new state and — optionally —
 * side effects.
 *
 * ### Why a queue (Channel) instead of calling `state.update {}` at the call site?
 * Because reduction must be **serialized**. When two coroutines dispatch at the same
 * time, a single consumer guarantees ordering and ensures that the `before`/`after` pair
 * seen by middleware and [onIntentProcessed] is consistent. That is the difference
 * between "works on my machine" and production code.
 *
 * ### Splitting intents (the key senior-level idea)
 * - `Intent.Ui` — what the user does (a click, typing).
 * - `Intent.Internal` — the result of asynchronous work (data, an error, a timeout).
 *
 * Both kinds go through the same reducer, so **every** state change has a name and shows
 * up on the timeline. There is no `_state.value = ...` scattered around the codebase.
 *
 * @param initialState the state rendered before anything happens
 * @param reducer the pure transition function
 * @param middlewares observers (logging/analytics); they cannot change the state
 * @param timelineLimit how many recent steps to keep for the time-travel debugger
 */
abstract class MviStore<S : MviState, I : MviIntent, E : MviEffect>(
    initialState: S,
    private val reducer: Reducer<S, I>,
    private val middlewares: List<Middleware<S, I>> = emptyList(),
    private val timelineLimit: Int = 50,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)

    /** The single source of truth for the UI. */
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effects = Channel<E>(capacity = Channel.BUFFERED)

    /** One-off events. A Channel (rather than a SharedFlow) loses nothing and duplicates nothing. */
    val effects: Flow<E> = _effects.receiveAsFlow()

    private val _timeline = MutableStateFlow(
        listOf(TimelineEntry<S, I>(index = 0, intent = null, state = initialState)),
    )

    /** History of (intent, state) pairs — the basis of the debug panel and time travel. */
    val timeline: StateFlow<List<TimelineEntry<S, I>>> = _timeline.asStateFlow()

    private val inbox = Channel<I>(capacity = Channel.UNLIMITED)

    private val _processedIntents = MutableSharedFlow<I>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /**
     * Stream of processed intents. Useful when Flow operators are needed (for example
     * `debounce` plus `flatMapLatest` in a search field) instead of managing Jobs by hand.
     */
    protected val processedIntents: SharedFlow<I> = _processedIntents.asSharedFlow()

    init {
        // One loop means one logical thread of reduction.
        viewModelScope.launch {
            for (intent in inbox) {
                val before = _state.value
                val after = reducer.reduce(before, intent)
                _state.value = after

                middlewares.forEach { it.afterReduce(intent, before, after) }
                recordOnTimeline(intent, after)
                _processedIntents.emit(intent)

                // Side effects run AFTER the reduction and must not block the loop.
                onIntentProcessed(intent, before, after)
            }
        }
    }

    /** The entry point for the view. Non-blocking: it only queues the intent. */
    fun dispatch(intent: I) {
        inbox.trySend(intent)
    }

    /**
     * Where I/O belongs: network, database, timers. The equivalent of an Executor in
     * MVIKotlin.
     *
     * The rule: **never change the state directly here**. Launch a coroutine and send its
     * result back through [dispatch] as an `Intent.Internal`.
     */
    protected open fun onIntentProcessed(intent: I, before: S, after: S) = Unit

    /** Sends a one-off event to the view. */
    protected fun emitEffect(effect: E) {
        _effects.trySend(effect)
    }

    /**
     * Time travel: restores a state from the timeline without emitting an intent.
     * A teaching and debugging tool — keep it behind a debug flag in production.
     */
    fun jumpTo(index: Int) {
        _timeline.value.getOrNull(index)?.let { _state.value = it.state }
    }

    private fun recordOnTimeline(intent: I, state: S) {
        val current = _timeline.value
        val entry = TimelineEntry(index = current.size, intent = intent, state = state)
        _timeline.value = (current + entry).takeLast(timelineLimit)
    }

    override fun onCleared() {
        inbox.close()
        _effects.close()
        super.onCleared()
    }
}

/** A simple middleware that logs every reduction, showing UDF live in the console. */
class LoggingMiddleware<S : MviState, I : MviIntent>(
    private val tag: String,
    private val log: (String) -> Unit = ::println,
) : Middleware<S, I> {
    override fun afterReduce(intent: I, before: S, after: S) {
        if (before == after) {
            log("[$tag] $intent -> state unchanged")
        } else {
            log("[$tag] $intent\n    before: $before\n    after:  $after")
        }
    }
}

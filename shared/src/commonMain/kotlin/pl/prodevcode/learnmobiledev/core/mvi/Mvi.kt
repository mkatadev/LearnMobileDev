package pl.prodevcode.learnmobiledev.core.mvi

/**
 * # The MVI core (Model - View - Intent)
 *
 * Three pillars that set MVI apart from MVVM:
 *
 * 1. **Single source of truth** — the whole screen is described by ONE immutable
 *    [MviState]. There are no separate `isLoading`, `error` and `data` streams that can
 *    drift apart.
 * 2. **Unidirectional data flow** — the view emits an [MviIntent], the store reduces it
 *    into a new state, the view renders that state. Never the other way round.
 * 3. **Pure reduction** — a [Reducer] is a function `(State, Intent) -> State`. No I/O,
 *    no clock, no randomness. That makes it fully testable and enables time-travel
 *    debugging.
 *
 * ```
 *        dispatch(Intent)              reduce(State, Intent)
 *  View ──────────────────► Store ──────────────────────────► State ──► View (render)
 *                             │
 *                             └── side effects (I/O) ──► Intent.Internal(result) ──► reduce
 * ```
 */

/** Immutable model of an entire screen. Always a `data class`. */
interface MviState

/** A user intention or the result of an asynchronous operation. Always a `sealed interface`. */
interface MviIntent

/**
 * A one-off event (navigation, snackbar, haptics).
 *
 * Why keep it out of the state? Because state is **replayable** — after a configuration
 * change it would be rendered a second time. An effect is consumed exactly once.
 */
interface MviEffect

/**
 * The pure state transition function. The heart of MVI.
 *
 * The contract must not be broken:
 * - no side effects (no network calls, no logging, no database writes),
 * - deterministic: same arguments, same result,
 * - always returns a new object (or the same one when nothing changes).
 */
fun interface Reducer<S : MviState, I : MviIntent> {
    fun reduce(state: S, intent: I): S
}

/**
 * Observer of the intent stream — logging, analytics, crash reporting.
 *
 * Middleware **cannot change** the state; that restriction is deliberate, so that no
 * second (hidden) mutation point appears next to the reducer.
 */
fun interface Middleware<S : MviState, I : MviIntent> {
    fun afterReduce(intent: I, before: S, after: S)
}

/** A timeline entry — the basis of the time-travel debugger in `MviStore`. */
data class TimelineEntry<S : MviState, I : MviIntent>(
    val index: Int,
    val intent: I?,
    val state: S,
)

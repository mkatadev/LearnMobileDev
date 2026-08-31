package pl.prodevcode.learnmobiledev.data.concurrency

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import pl.prodevcode.learnmobiledev.domain.model.ScenarioResult
import pl.prodevcode.learnmobiledev.domain.repository.ConcurrencyLab

/**
 * Coroutine-based concurrency lab.
 *
 * **Every scenario is deterministic.** None of them rely on "sometimes it happens":
 * races are forced by an explicit suspension point (`yield()`) between read and write.
 *
 * That is the key lesson of this screen: a race in coroutines **does not require multiple
 * threads**. A suspension point between reading and writing is enough for another
 * coroutine to slip in, even on a single thread.
 *
 * This class holds no user-facing text — descriptions live in `scenarios.json` and are
 * supplied by `ScenarioRepository`.
 */
class CoroutineConcurrencyLab(
    private val workers: Int = DEFAULT_WORKERS,
) : ConcurrencyLab {

    override suspend fun run(scenarioId: String): ScenarioResult = when (scenarioId) {
        LOST_UPDATE -> runLostUpdate()
        MUTEX -> runWithMutex()
        STATE_FLOW_VALUE -> runStateFlowValue()
        STATE_FLOW_UPDATE -> runStateFlowUpdate()
        MVI_LOOP -> runMviLoop()
        COLLECT_LATEST -> runCollectLatest()
        CHANNEL_VS_SHARED_FLOW -> runChannelVsSharedFlow()
        else -> ScenarioResult(scenarioId, "-", "unknown scenario", passed = false)
    }

    private suspend fun runLostUpdate(): ScenarioResult {
        var counter = 0
        coroutineScope {
            List(workers) {
                async {
                    val read = counter
                    yield() // the gap other coroutines slip into
                    counter = read + 1
                }
            }.awaitAll()
        }
        return result(LOST_UPDATE, expected = workers, actual = counter)
    }

    private suspend fun runWithMutex(): ScenarioResult {
        val mutex = Mutex()
        var counter = 0
        coroutineScope {
            List(workers) {
                async {
                    mutex.withLock {
                        val read = counter
                        yield()
                        counter = read + 1
                    }
                }
            }.awaitAll()
        }
        return result(MUTEX, expected = workers, actual = counter)
    }

    private suspend fun runStateFlowValue(): ScenarioResult {
        val flow = MutableStateFlow(0)
        coroutineScope {
            List(workers) {
                async {
                    val read = flow.value
                    yield()
                    flow.value = read + 1
                }
            }.awaitAll()
        }
        return result(STATE_FLOW_VALUE, expected = workers, actual = flow.value)
    }

    private suspend fun runStateFlowUpdate(): ScenarioResult {
        val flow = MutableStateFlow(0)
        coroutineScope {
            List(workers) {
                async {
                    yield()
                    flow.update { it + 1 }
                }
            }.awaitAll()
        }
        return result(STATE_FLOW_UPDATE, expected = workers, actual = flow.value)
    }

    /** Miniature MviStore: a queue, a single consumer and a pure reduction. */
    private suspend fun runMviLoop(): ScenarioResult = coroutineScope {
        val inbox = Channel<Int>(Channel.UNLIMITED)
        var state = 0

        val loop = launch {
            for (intent in inbox) {
                val read = state
                yield() // safe even with a suspension point in the middle…
                state = read + intent // …because there is exactly one consumer
            }
        }

        List(workers) { async { inbox.send(1) } }.awaitAll()
        inbox.close()
        loop.join()

        result(MVI_LOOP, expected = workers, actual = state)
    }

    private suspend fun runCollectLatest(): ScenarioResult = coroutineScope {
        val requests = Channel<Int>(Channel.UNLIMITED)
        val completed = mutableListOf<Int>()

        val consumer = launch {
            requests.receiveAsFlow().collectLatest { id ->
                delay(50) // cancellable "work"
                completed += id
            }
        }

        repeat(5) { requests.send(it + 1) }
        delay(200)
        requests.close()
        consumer.cancel()

        ScenarioResult(
            scenarioId = COLLECT_LATEST,
            expected = "completed: [5]",
            actual = "completed: $completed",
            passed = completed == listOf(5),
            log = listOf(
                "requests sent: 1, 2, 3, 4, 5",
                "each new request cancelled the previous one",
            ),
        )
    }

    private suspend fun runChannelVsSharedFlow(): ScenarioResult = coroutineScope {
        val channel = Channel<String>(Channel.BUFFERED)
        val sharedFlow = MutableSharedFlow<String>()

        // Emit BEFORE anyone subscribes.
        channel.trySend("efekt")
        sharedFlow.tryEmit("efekt")

        val fromChannel = mutableListOf<String>()
        val fromSharedFlow = mutableListOf<String>()

        val c1 = launch { channel.receiveAsFlow().collect { fromChannel += it } }
        val c2 = launch { sharedFlow.collect { fromSharedFlow += it } }
        delay(50)
        c1.cancel()
        c2.cancel()

        ScenarioResult(
            scenarioId = CHANNEL_VS_SHARED_FLOW,
            expected = "Channel: 1 event, SharedFlow: 0 events",
            actual = "Channel: ${fromChannel.size}, SharedFlow: ${fromSharedFlow.size}",
            passed = fromChannel.size == 1 && fromSharedFlow.isEmpty(),
            log = listOf(
                "event emitted before any collector subscribed",
                "Channel buffers it, SharedFlow without replay does not",
            ),
        )
    }

    private fun result(id: String, expected: Int, actual: Int) = ScenarioResult(
        scenarioId = id,
        expected = expected.toString(),
        actual = actual.toString(),
        passed = expected == actual,
        log = if (expected == actual) {
            listOf("all $workers operations were applied")
        } else {
            listOf("lost ${expected - actual} of $expected operations")
        },
    )

    companion object {
        const val DEFAULT_WORKERS = 100

        const val LOST_UPDATE = "lost-update"
        const val MUTEX = "mutex"
        const val STATE_FLOW_VALUE = "stateflow-value"
        const val STATE_FLOW_UPDATE = "stateflow-update"
        const val MVI_LOOP = "mvi-loop"
        const val COLLECT_LATEST = "collect-latest"
        const val CHANNEL_VS_SHARED_FLOW = "channel-vs-sharedflow"
    }
}

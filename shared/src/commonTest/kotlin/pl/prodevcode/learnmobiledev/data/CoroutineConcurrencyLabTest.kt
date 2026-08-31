package pl.prodevcode.learnmobiledev.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import pl.prodevcode.learnmobiledev.data.concurrency.CoroutineConcurrencyLab

/**
 * Tests for the lab: they verify that the demonstrations actually demonstrate.
 *
 * A "buggy" scenario MUST fail and a fixed one MUST pass — otherwise the lesson lies.
 * This is one of the rare cases where `assertFalse(result.passed)` is the correct
 * assertion.
 */
class CoroutineConcurrencyLabTest {

    private val lab = CoroutineConcurrencyLab(workers = 100)

    @Test
    fun `lost update actually drops writes`() = runTest {
        val result = lab.run(CoroutineConcurrencyLab.LOST_UPDATE)

        assertFalse(result.passed, "the scenario was meant to demonstrate a race, but passed")
        assertTrue(
            result.actual.toInt() < 100,
            "expected lost writes, got ${result.actual}",
        )
    }

    @Test
    fun `mutex fixes lost updates`() = runTest {
        val result = lab.run(CoroutineConcurrencyLab.MUTEX)

        assertTrue(result.passed)
        assertEquals("100", result.actual)
    }

    @Test
    fun `writing to StateFlow value is race prone`() = runTest {
        val result = lab.run(CoroutineConcurrencyLab.STATE_FLOW_VALUE)

        assertFalse(result.passed)
    }

    @Test
    fun `StateFlow update is safe`() = runTest {
        val result = lab.run(CoroutineConcurrencyLab.STATE_FLOW_UPDATE)

        assertTrue(result.passed)
        assertEquals("100", result.actual)
    }

    @Test
    fun `MVI loop serializes intents despite parallel dispatches`() = runTest {
        val result = lab.run(CoroutineConcurrencyLab.MVI_LOOP)

        assertTrue(result.passed, "the MVI loop lost intents: ${result.actual}")
        assertEquals("100", result.actual)
    }

    @Test
    fun `collectLatest completes only the newest request`() = runTest {
        val result = lab.run(CoroutineConcurrencyLab.COLLECT_LATEST)

        assertTrue(result.passed, "expected only the last request, got: ${result.actual}")
    }

    @Test
    fun `Channel delivers an effect emitted before any collector subscribed`() = runTest {
        val result = lab.run(CoroutineConcurrencyLab.CHANNEL_VS_SHARED_FLOW)

        assertTrue(result.passed, result.actual)
    }

    @Test
    fun `unknown scenario id fails instead of throwing`() = runTest {
        val result = lab.run("does-not-exist")

        assertFalse(result.passed)
    }
}

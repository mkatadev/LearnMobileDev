package pl.prodevcode.learnmobiledev.domain.model

/**
 * A scenario demonstrating the behaviour of concurrent code.
 *
 * Every scenario declares an **expected** outcome. When the actual outcome differs,
 * that is not a broken test — it is proof that the demonstrated coding style is unsound.
 */
data class ConcurrencyScenario(
    val id: String,
    val title: String,
    val description: String,
    /** What we expect and why — shown before the scenario is executed. */
    val expectation: String,
    /** Whether the scenario is meant to fail, because it demonstrates a bug. */
    val demonstratesBug: Boolean,
    /** Explanation revealed after the run. */
    val explanation: String,
)

/** Outcome of a single scenario run. */
data class ScenarioResult(
    val scenarioId: String,
    val expected: String,
    val actual: String,
    val passed: Boolean,
    val log: List<String> = emptyList(),
)

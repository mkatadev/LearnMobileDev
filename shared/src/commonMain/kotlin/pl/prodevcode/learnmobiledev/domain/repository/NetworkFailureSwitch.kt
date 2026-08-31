package pl.prodevcode.learnmobiledev.domain.repository

/**
 * Port controlling simulated failures — for teaching purposes only.
 *
 * A separate interface rather than an extra method on [UserRepository]: a real network
 * implementation should not need to know anything about demo mode
 * (Interface Segregation Principle).
 */
interface NetworkFailureSwitch {
    var failNextLoad: Boolean
}

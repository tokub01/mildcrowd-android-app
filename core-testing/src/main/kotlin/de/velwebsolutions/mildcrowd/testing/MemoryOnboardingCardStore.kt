package de.velwebsolutions.mildcrowd.testing

import de.velwebsolutions.mildcrowd.core.onboarding.OnboardingCardStore
import java.time.Instant

/** [OnboardingCardStore] in memory, for tests. */
class MemoryOnboardingCardStore(var until: Instant? = null) : OnboardingCardStore {
    override fun snoozedUntil(): Instant? = until

    override fun snooze(until: Instant) {
        this.until = until
    }
}

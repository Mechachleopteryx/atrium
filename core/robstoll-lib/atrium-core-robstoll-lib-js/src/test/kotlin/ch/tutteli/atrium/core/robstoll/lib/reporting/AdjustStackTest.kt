package ch.tutteli.atrium.core.robstoll.lib.reporting

import ch.tutteli.atrium.api.fluent.en_GB.*
import ch.tutteli.atrium.api.verbs.internal.AssertionVerb
import ch.tutteli.atrium.api.verbs.internal.expect
import ch.tutteli.atrium.core.ExperimentalNewExpectTypes
import ch.tutteli.atrium.core.coreFactory
import ch.tutteli.atrium.core.polyfills.stackBacktrace
import ch.tutteli.atrium.logic.creating.RootExpectBuilder
import ch.tutteli.atrium.logic.creating.RootExpectOptions
import ch.tutteli.atrium.reporting.AtriumErrorAdjuster
import ch.tutteli.atrium.reporting.reporter
import kotlin.test.Test

class AdjustStackTest {

    @Test
    fun noOp_containsMochaAndAtrium() {
        expect {
            assertNoOp(1).toBe(2)
        }.toThrow<AssertionError> {
            feature(AssertionError::stackBacktrace).contains(
                { contains("mocha") },
                { contains("atrium-core-api-js.js") }
            )
        }
    }

    @Test
    fun removeRunner_containsAtriumButNotMocha() {
        expect {
            assertRemoveRunner(1).toBe(2)
        }.toThrow<AssertionError> {
            feature(AssertionError::stackBacktrace)
                .containsNot.entry { contains("mocha") }
                .contains { contains("atrium-core-api-js.js") }
        }
    }

    @Test
    fun removeRunner_containsAtriumButNotMochaInCause() {
        val adjuster = coreFactory.newRemoveRunnerAtriumErrorAdjuster()
        val throwable = IllegalArgumentException("hello", UnsupportedOperationException("world"))
        adjuster.adjust(throwable)
        expect(throwable.cause!!.stackBacktrace)
            .containsNot.entry { contains("mocha") }
            .contains { contains("atrium-core-robstoll-lib-js") }
    }

    @Test
    fun removeAtrium_containsMochaButNotAtrium() {
        expect {
            assertRemoveAtrium(1).toBe(2)
        }.toThrow<AssertionError> {
            feature(AssertionError::stackBacktrace)
                .contains { contains("mocha") }
                .containsNot.entry { contains("atrium-core-api-js.js") }
        }
    }

    @Test
    fun removeAtrium_containsMochaButNotAtriumInCause() {
        val adjuster = coreFactory.newRemoveAtriumFromAtriumErrorAdjuster()
        val throwable = IllegalArgumentException("hello", UnsupportedOperationException("world"))
        adjuster.adjust(throwable)
        expect(throwable.cause!!.stackBacktrace)
            .contains { contains("mocha") }
            .containsNot.entry { contains("atrium-core-robstoll-lib-js") }
    }

    private fun <T : Any> assertNoOp(subject: T) = createExpect(
        subject, coreFactory.newNoOpAtriumErrorAdjuster()
    )

    private fun <T : Any> assertRemoveRunner(subject: T) = createExpect(
        subject, coreFactory.newRemoveRunnerAtriumErrorAdjuster()
    )

    private fun <T : Any> assertRemoveAtrium(subject: T) = createExpect(
        subject, coreFactory.newRemoveAtriumFromAtriumErrorAdjuster()
    )

    @Suppress("DEPRECATION" /* OptIn is only available since 1.3.70 which we cannot use if we want to support 1.2 */)
    @UseExperimental(ExperimentalNewExpectTypes::class)
    private fun <T : Any> createExpect(subject: T, adjuster: AtriumErrorAdjuster) =
        RootExpectBuilder.forSubject(subject)
            .withVerb(AssertionVerb.EXPECT)
            .withOptions(RootExpectOptions(reporter = DelegatingReporter(reporter, adjuster)))
            .build()
}

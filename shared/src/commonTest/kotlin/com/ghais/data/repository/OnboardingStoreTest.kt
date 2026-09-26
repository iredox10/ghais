package com.ghais.data.repository

import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Guards the onboarding gate: a returning user must never be bounced into
 * onboarding, and only an explicitly new account may be put back through it.
 *
 * Two rules carry that contract:
 *  - **Unbound fails open.** `setOwner` runs on SyncTriggers' own collector, so
 *    the first frames after a session appears — and every frame for an offline
 *    user whose refresh failed — are still on the "local" sentinel. An
 *    unidentified visitor reports done, never "not done".
 *  - **Write order does not matter.** A done/restart that lands while unbound is
 *    held and replayed onto the account bound next, where it wins over that
 *    account's own stored flag.
 *
 * Scope note: `OnboardingStore` builds its `Settings` lazily behind `private val
 * settings by lazy { Settings() }`, and on the Android unit-test classpath that
 * no-arg factory throws (no `Context` is installed by the androidx.startup
 * initializer in a JVM test), so every read/write degrades to a silent no-op
 * there. These tests therefore assert only *write-driven* flow transitions —
 * never a value loaded back out of storage — which makes them identical on a
 * no-op backend and on a real one (iOS simulator, NSUserDefaults). Owner ids
 * are randomised per run so a namespace is always provably empty, which keeps
 * the "another owner is not done" assertion true even when the backend does
 * persist between runs.
 */
class OnboardingStoreTest {

    /**
     * A settings key space that provably has nothing stored under it, on any
     * backend and across repeated runs.
     */
    private fun newOwnerId(tag: String): String = "$tag-${Random.nextLong().toString(16)}"

    /**
     * `pendingDone` is private, and a bind is the only public way to consume it.
     * Binding a throwaway owner and then unbinding leaves the store genuinely
     * fresh: no pending statement, so the next read of `isDoneForCurrentUser`
     * comes from `doneWhileUnbound()` rather than from a leftover write.
     */
    private fun flushPendingDone() {
        OnboardingStore.setOwner(newOwnerId("flush"))
        OnboardingStore.setOwner("local")
    }

    @BeforeTest
    fun setUp() {
        // OnboardingStore is an `object` singleton, so its flows and owner
        // namespace leak between tests in a run. There is no reset API, only the
        // ordinary writers, so normalise through them: unbind to the "local"
        // sentinel and land on a known finished state (done = true, seen = true,
        // step = 0). Every test below re-establishes the exact precondition it
        // needs via flushPendingDone(), so this only has to be *a* clean state.
        OnboardingStore.setOwner("local")
        OnboardingStore.markDoneForCurrentUser()
        OnboardingStore.setStep(0)
    }

    @Test
    fun testUnboundStoreReportsDoneSoReturningUserSkipsOnboarding() {
        // "We cannot identify this person" must not read as "this person is
        // new" — that is what would drop a returning user into onboarding
        // during the unbound window.
        flushPendingDone()

        assertTrue(
            OnboardingStore.isDoneForCurrentUser.value,
            "An unbound store must report done, otherwise a returning login lands in onboarding",
        )
    }

    @Test
    fun testUnbindResetsToDoneWhenTheLastWriteWasBoundToThePreviousOwner() {
        // Sign-out unbinds the namespace. A restart made while *bound* is not a
        // held statement, so the unbound view goes back to failing open rather
        // than stranding a returning user in the previous owner's "not done".
        flushPendingDone()
        OnboardingStore.setOwner(newOwnerId("signed-out-user"))
        OnboardingStore.restartForNewUser()
        assertFalse(OnboardingStore.isDoneForCurrentUser.value)

        OnboardingStore.setOwner("local")

        assertTrue(OnboardingStore.isDoneForCurrentUser.value)
    }

    @Test
    fun testMarkDoneForCurrentUserIsIdempotentWhileUnbound() {
        // A sign-in callback runs before the session collector has rebound the
        // stores, so this is the shape that actually ships. Calling it twice
        // must be a no-op the second time, and must not touch the step.
        flushPendingDone()
        OnboardingStore.setStep(3)

        OnboardingStore.markDoneForCurrentUser()
        assertTrue(OnboardingStore.isDoneForCurrentUser.value)
        assertTrue(OnboardingStore.seen.value)
        assertEquals(3, OnboardingStore.step.value, "markDone must leave the step alone")

        OnboardingStore.markDoneForCurrentUser()
        assertTrue(OnboardingStore.isDoneForCurrentUser.value)
        assertTrue(OnboardingStore.seen.value)
        assertEquals(3, OnboardingStore.step.value)
    }

    @Test
    fun testMarkDoneForCurrentUserIsIdempotentForBoundOwner() {
        flushPendingDone()
        val owner = newOwnerId("mark-done-owner")
        OnboardingStore.setOwner(owner)
        OnboardingStore.setStep(2)

        OnboardingStore.markDoneForCurrentUser()
        assertTrue(OnboardingStore.isDoneForCurrentUser.value)
        assertTrue(OnboardingStore.seen.value)
        assertEquals(2, OnboardingStore.step.value)

        OnboardingStore.markDoneForCurrentUser()
        assertTrue(OnboardingStore.isDoneForCurrentUser.value)
        assertTrue(OnboardingStore.seen.value)
    }

    @Test
    fun testPendingUnboundDoneMarkIsReplayedOntoTheNextOwnerAndWins() {
        // The whole point of the pending field: the write is not lost across
        // the bind, and it overrides the flag the bound account had stored.
        flushPendingDone()
        val owner = newOwnerId("pending-owner")

        // The bound account has onboarding outstanding.
        OnboardingStore.setOwner(owner)
        OnboardingStore.restartForNewUser()
        assertFalse(OnboardingStore.isDoneForCurrentUser.value, "Owner starts not done")
        assertEquals(0, OnboardingStore.step.value)

        // Sign-out unbinds the namespace.
        OnboardingStore.setOwner("local")

        // The returning-user sign-in callback runs here, before the session
        // collector has rebound anything: the flow flips immediately.
        OnboardingStore.markDoneForCurrentUser()
        assertTrue(OnboardingStore.isDoneForCurrentUser.value)

        // The collector finally binds. The held write is replayed onto this
        // account and beats the "not done" it was stored with above.
        OnboardingStore.setOwner(owner)
        assertTrue(
            OnboardingStore.isDoneForCurrentUser.value,
            "A done-mark made while unbound must survive the bind and win over the stored flag",
        )
    }

    @Test
    fun testPendingDoneMarkLandsOnExactlyTheNextBoundOwner() {
        // "Applied to — and only to — the account that binds next": the write
        // must not follow the session on to a second user.
        flushPendingDone()
        val ownerA = newOwnerId("sole-owner-a")
        val ownerB = newOwnerId("sole-owner-b")

        OnboardingStore.setOwner("local")
        OnboardingStore.markDoneForCurrentUser()
        assertTrue(OnboardingStore.isDoneForCurrentUser.value)

        OnboardingStore.setOwner(ownerA)
        assertTrue(OnboardingStore.isDoneForCurrentUser.value, "The next bind receives the write")

        OnboardingStore.setOwner(ownerB)
        assertFalse(
            OnboardingStore.isDoneForCurrentUser.value,
            "A later owner must not inherit an already-replayed write",
        )
    }

    @Test
    fun testMarkingDoneForOneOwnerDoesNotMarkAnotherOwnerDone() {
        // Namespacing: a shared "done" would let a second user on the device
        // skip the goal/daily-minutes setup they never did.
        flushPendingDone()
        val ownerA = newOwnerId("owner-a")
        val ownerB = newOwnerId("owner-b")

        OnboardingStore.setOwner(ownerA)
        OnboardingStore.markDoneForCurrentUser()
        assertTrue(OnboardingStore.isDoneForCurrentUser.value)

        OnboardingStore.setOwner(ownerB)
        assertFalse(
            OnboardingStore.isDoneForCurrentUser.value,
            "Owner B must not inherit owner A's completed onboarding",
        )
    }

    @Test
    fun testRestartForNewUserReopensOnboardingForABoundOwner() {
        flushPendingDone()
        val owner = newOwnerId("restart-owner")
        OnboardingStore.setOwner(owner)
        OnboardingStore.setStep(3)
        OnboardingStore.markDoneForCurrentUser()
        assertTrue(OnboardingStore.isDoneForCurrentUser.value)

        OnboardingStore.restartForNewUser()
        assertFalse(OnboardingStore.isDoneForCurrentUser.value)
        assertFalse(OnboardingStore.seen.value)
        assertEquals(0, OnboardingStore.step.value)

        // Idempotent, like the mark path.
        OnboardingStore.restartForNewUser()
        assertFalse(OnboardingStore.isDoneForCurrentUser.value)
        assertFalse(OnboardingStore.seen.value)
        assertEquals(0, OnboardingStore.step.value)
    }

    @Test
    fun testRestartForNewUserReportsNotDoneImmediatelyWhileUnbound() {
        // A sign-up callback runs while the store is still on the previous
        // owner, so restart has to open the gate without waiting for a bind.
        flushPendingDone()
        OnboardingStore.setOwner(newOwnerId("established-user"))
        OnboardingStore.setOwner("local")
        OnboardingStore.markDoneForCurrentUser()
        assertTrue(OnboardingStore.isDoneForCurrentUser.value)

        OnboardingStore.restartForNewUser()
        assertFalse(OnboardingStore.isDoneForCurrentUser.value)
        assertFalse(OnboardingStore.seen.value)
        assertEquals(0, OnboardingStore.step.value)
    }

    @Test
    fun testCompleteSetsDoneAndResetsStepToZeroForABoundOwner() {
        flushPendingDone()
        val owner = newOwnerId("complete-owner")
        OnboardingStore.setOwner(owner)
        OnboardingStore.setStep(4)
        assertEquals(4, OnboardingStore.step.value)

        OnboardingStore.complete()
        assertTrue(OnboardingStore.isDoneForCurrentUser.value)
        assertTrue(OnboardingStore.seen.value)
        assertEquals(0, OnboardingStore.step.value, "complete must reset the step to 0")
    }

    @Test
    fun testCompleteWhileUnboundSetsDoneAndResetsStepToZero() {
        // Skip / finish on the last page, same ordering caveat as the mark path.
        flushPendingDone()
        OnboardingStore.setStep(2)

        OnboardingStore.complete()
        assertTrue(OnboardingStore.isDoneForCurrentUser.value)
        assertTrue(OnboardingStore.seen.value)
        assertEquals(0, OnboardingStore.step.value)
    }

    @Test
    fun testResetForDebugBehavesLikeRestartForNewUser() {
        flushPendingDone()
        val owner = newOwnerId("debug-owner")
        OnboardingStore.setOwner(owner)
        OnboardingStore.markDoneForCurrentUser()
        assertTrue(OnboardingStore.isDoneForCurrentUser.value)

        OnboardingStore.resetForDebug()
        assertFalse(OnboardingStore.isDoneForCurrentUser.value)
        assertFalse(OnboardingStore.seen.value)
        assertEquals(0, OnboardingStore.step.value)
    }
}

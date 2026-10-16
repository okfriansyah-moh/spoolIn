package com.spoolin.core.operations.work

import com.spoolin.support.work.LeaseStatus
import com.spoolin.support.work.WorkFence
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * ATDD-287: Reject stale lease owners before renewal or sending.
 *
 * Fixture: LEASED ownerW1 epoch7 until T; original deadline D (well beyond T).
 *
 * Verifies:
 * - W1/epoch7 at T-1µs → eligible (live owner, live epoch, live lease).
 * - W1/epoch6 at T-1µs → rejected (stale epoch).
 * - W2/epoch7 at T-1µs → rejected (wrong owner).
 * - W1/epoch7 at T (exact expiry) → rejected (lease expired).
 * - W1/epoch7 at T+1µs → rejected (lease expired).
 * - Any worker on DONE/EXHAUSTED/CANCELLED → rejected (terminal state).
 * - W1/epoch7 at deadline D → rejected (deadline reached).
 */
class WorkFenceRulesTest {

    private val w1 = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val w2 = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val workId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")

    private val leaseUntil = Instant.parse("2026-10-20T10:00:30Z")
    private val deadline = Instant.parse("2026-10-20T11:00:00Z")
    private val beforeExpiry = leaseUntil.minusNanos(1000)
    private val atExpiry = leaseUntil
    private val afterExpiry = leaseUntil.plusNanos(1000)

    private fun leasedStatus(
        owner: UUID = w1,
        epoch: Long = 7L,
        until: Instant? = leaseUntil,
        dl: Instant = deadline,
        state: String = "LEASED",
    ) = LeaseStatus(workId, state, owner, epoch, until, dl)

    @Test
    fun `live owner with matching epoch before expiry is eligible`() {
        assertTrue(WorkFence.isEligible(leasedStatus(), w1, 7L, beforeExpiry))
    }

    @Test
    fun `stale epoch is rejected even with correct owner`() {
        assertFalse(WorkFence.isEligible(leasedStatus(), w1, 6L, beforeExpiry))
    }

    @Test
    fun `wrong owner is rejected even with correct epoch`() {
        assertFalse(WorkFence.isEligible(leasedStatus(), w2, 7L, beforeExpiry))
    }

    @Test
    fun `lease at exact expiry instant is rejected`() {
        assertFalse(WorkFence.isEligible(leasedStatus(), w1, 7L, atExpiry))
    }

    @Test
    fun `lease after expiry is rejected`() {
        assertFalse(WorkFence.isEligible(leasedStatus(), w1, 7L, afterExpiry))
    }

    @Test
    fun `DONE state is rejected`() {
        assertFalse(WorkFence.isEligible(leasedStatus(state = "DONE", until = null), w1, 7L, beforeExpiry))
    }

    @Test
    fun `EXHAUSTED state is rejected`() {
        assertFalse(WorkFence.isEligible(leasedStatus(state = "EXHAUSTED", until = null), w1, 7L, beforeExpiry))
    }

    @Test
    fun `CANCELLED state is rejected`() {
        assertFalse(WorkFence.isEligible(leasedStatus(state = "CANCELLED", until = null), w1, 7L, beforeExpiry))
    }

    @Test
    fun `READY state is rejected`() {
        assertFalse(WorkFence.isEligible(leasedStatus(state = "READY", until = null), w1, 7L, beforeExpiry))
    }

    @Test
    fun `work deadline reached rejects even live lease`() {
        // Now is exactly the deadline — not strictly before
        assertFalse(WorkFence.isEligible(leasedStatus(dl = beforeExpiry), w1, 7L, beforeExpiry))
    }

    @Test
    fun `deadline past rejects even with live lease`() {
        // Work deadline has already passed — now is AT the deadline (not strictly before)
        val workDeadline = Instant.parse("2026-10-20T09:00:00Z") // before leaseUntil
        val nowAtDeadline = workDeadline // now == deadline → !isAfter → rejected
        assertFalse(
            WorkFence.isEligible(leasedStatus(dl = workDeadline), w1, 7L, nowAtDeadline),
            "At work deadline: no new effect allowed",
        )
        val nowPastDeadline = workDeadline.plusSeconds(1)
        assertFalse(
            WorkFence.isEligible(leasedStatus(dl = workDeadline), w1, 7L, nowPastDeadline),
            "After work deadline: no new effect allowed",
        )
    }

    @Test
    fun `null lease_until rejects even correct owner and epoch`() {
        assertFalse(WorkFence.isEligible(leasedStatus(until = null), w1, 7L, beforeExpiry))
    }
}

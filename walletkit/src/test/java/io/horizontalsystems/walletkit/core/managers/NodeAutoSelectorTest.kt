package io.horizontalsystems.walletkit.core.managers

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

class NodeAutoSelectorTest {

    private var enabled = true
    private var current = "a"
    private var probes: suspend () -> List<SourceProbe<String>> = { emptyList() }
    private var probeCount = 0
    private val switches = mutableListOf<String>()

    private fun up(node: String, ms: Double, height: Long = 1000) = SourceProbe(node, ms, height)
    private fun down(node: String) = SourceProbe<String>(node, null, null)

    // Leaves the current node only when it failed or is behind, as Zano does
    private val selector = NodeAutoSelector(
        isEnabled = { enabled },
        current = { current },
        probe = {
            probeCount++
            probes()
        },
        switchTo = {
            switches.add(it)
            current = it
        },
        startupChoice = { current, probes -> SourceRepick.choose(current, probes, lagThreshold = 5, switchForSpeed = false) },
        lagThreshold = 5,
        timeout = 200.milliseconds,
    )

    // Startup

    @Test
    fun startupDoesNotProbeWhenOff() = runBlocking {
        enabled = false
        probes = { listOf(down("a"), up("b", 100.0)) }

        selector.pickOnStartup()

        assertEquals(0, probeCount)
        assertTrue(switches.isEmpty())
    }

    @Test
    fun startupLeavesADeadNode() = runBlocking {
        probes = { listOf(down("a"), up("b", 100.0)) }

        selector.pickOnStartup()

        assertEquals(listOf("b"), switches)
    }

    @Test
    fun startupLeavesANodeThatIsBehind() = runBlocking {
        probes = { listOf(up("a", 50.0, height = 900), up("b", 400.0, height = 1000)) }

        selector.pickOnStartup()

        assertEquals(listOf("b"), switches)
    }

    @Test
    fun startupKeepsASlowerNodeThatWorks() = runBlocking {
        probes = { listOf(up("a", 3000.0), up("b", 100.0)) }

        selector.pickOnStartup()

        assertTrue(switches.isEmpty())
    }

    @Test
    fun startupKeepsTheNodeWhenProbingFails() = runBlocking {
        probes = { throw IOException("no network") }

        selector.pickOnStartup()

        assertTrue(switches.isEmpty())
    }

    @Test
    fun startupKeepsTheNodeWhenProbingTakesTooLong() = runBlocking {
        probes = {
            delay(60_000)
            listOf(down("a"), up("b", 100.0))
        }

        selector.pickOnStartup()

        assertTrue(switches.isEmpty())
    }

    @Test
    fun startupKeepsTheNodeWhenProbingIsNotAvailable() = runBlocking {
        selector.pickOnStartup()

        assertTrue(switches.isEmpty())
    }

    @Test
    fun nodePickedByTheUserDuringTheProbeIsKept() = runBlocking {
        probes = {
            current = "c"
            listOf(down("a"), up("b", 100.0))
        }

        selector.pickOnStartup()

        assertTrue(switches.isEmpty())
        assertEquals("c", current)
    }

    @Test
    fun switchTurnedOffDuringTheProbeIsRespected() = runBlocking {
        probes = {
            enabled = false
            listOf(down("a"), up("b", 100.0))
        }

        assertFalse(selector.reselect())
        assertTrue(switches.isEmpty())
    }

    // Reselect

    @Test
    fun reselectDoesNothingWhenOff() = runBlocking {
        enabled = false
        probes = { listOf(down("a"), up("b", 100.0)) }

        assertFalse(selector.reselect())
        assertEquals(0, probeCount)
    }

    @Test
    fun reselectMovesToTheFastestNodeThatAnswers() = runBlocking {
        probes = { listOf(down("a"), up("b", 900.0), up("c", 300.0)) }

        assertTrue(selector.reselect())
        assertEquals(listOf("c"), switches)
    }

    @Test
    fun reselectNeverPicksANodeThatIsBehind() = runBlocking {
        probes = { listOf(down("a"), up("b", 10.0, height = 900), up("c", 800.0, height = 1000)) }

        assertTrue(selector.reselect())
        assertEquals(listOf("c"), switches)
    }

    @Test
    fun reselectReportsNoSwitchWhenTheCurrentNodeIsTheFastest() = runBlocking {
        probes = { listOf(up("a", 100.0), up("b", 500.0)) }

        assertFalse(selector.reselect())
        assertTrue(switches.isEmpty())
    }

    // Every node failing means the device has no usable network
    @Test
    fun reselectKeepsTheNodeWhenNothingAnswers() = runBlocking {
        probes = { listOf(down("a"), down("b")) }

        assertFalse(selector.reselect())
        assertTrue(switches.isEmpty())
    }

    @Test
    fun reselectKeepsTheNodeWhenProbingTakesTooLong() = runBlocking {
        probes = {
            delay(60_000)
            listOf(down("a"), up("b", 100.0))
        }

        assertFalse(selector.reselect())
        assertTrue(switches.isEmpty())

        // and the next attempt is not blocked by the one that timed out
        probes = { listOf(down("a"), up("b", 100.0)) }
        assertTrue(selector.reselect())
    }

    @Test
    fun secondReselectWhileOneIsRunningDoesNothing() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        probes = {
            started.complete(Unit)
            release.await()
            listOf(down("a"), up("b", 100.0))
        }
        val slow = NodeAutoSelector(
            isEnabled = { true },
            current = { current },
            probe = { probes() },
            switchTo = { switches.add(it); current = it },
            startupChoice = { _, _ -> null },
        )

        val first = async { slow.reselect() }
        started.await()

        assertFalse(slow.reselect())
        release.complete(Unit)
        assertTrue(first.await())
        assertEquals(listOf("b"), switches)
    }

    // Asking the current node directly, for a kit that does not notice its node dying

    @Test
    fun currentNodeThatAnswersIsNotDown() = runBlocking {
        assertFalse(selector.isCurrentDown { listOf(up(it, 100.0)) })
    }

    @Test
    fun currentNodeThatFailsIsDown() = runBlocking {
        assertTrue(selector.isCurrentDown { listOf(down(it)) })
    }

    @Test
    fun currentNodeThatNeverAnswersIsDown() = runBlocking {
        assertTrue(selector.isCurrentDown {
            delay(60_000)
            listOf(up(it, 100.0))
        })
    }

    @Test
    fun currentNodeIsNotAskedWhenOff() = runBlocking {
        enabled = false
        var asked = false

        assertFalse(selector.isCurrentDown { asked = true; listOf(down(it)) })
        assertFalse(asked)
    }

    @Test
    fun currentNodeIsNotDownWhenProbingIsNotAvailable() = runBlocking {
        assertFalse(selector.isCurrentDown { emptyList() })
    }

    // The startup rule Monero and Zcash use: always the fastest node that answered
    @Test
    fun fastestStartupRuleMovesToAFasterNode() = runBlocking {
        val fastest = NodeAutoSelector(
            isEnabled = { true },
            current = { current },
            probe = { listOf(up("a", 500.0, height = 0), up("b", 100.0, height = 0), down("c")) },
            switchTo = { switches.add(it); current = it },
            startupChoice = { _, probes -> NodeAutoSelector.fastest(probes) },
        )

        fastest.pickOnStartup()
        fastest.pickOnStartup()

        assertEquals(listOf("b"), switches)
    }
}

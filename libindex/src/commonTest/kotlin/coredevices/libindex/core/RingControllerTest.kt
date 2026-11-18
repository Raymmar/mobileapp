package coredevices.libindex.core

import coredevices.libindex.IndexDevices
import coredevices.libindex.Rings
import coredevices.libindex.Scanning
import coredevices.libindex.device.DiscoveredIndexDevice
import coredevices.libindex.device.IndexDevice
import coredevices.libindex.device.IndexIdentifier
import coredevices.libindex.device.IndexImage
import coredevices.libindex.device.IndexPairingResult
import coredevices.libindex.device.IndexPairingState
import coredevices.libindex.device.InterviewedIndexDevice
import coredevices.libindex.device.PairableIndexDevice
import coredevices.libindex.device.RSSIMeasurement
import coredevices.libindex.device.RepairableIndexDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration

class RingControllerTest {

    private class FakeScanning : Scanning {
        var starts = 0
        var stops = 0
        override val isScanning: StateFlow<Boolean> = MutableStateFlow(false)
        override fun startScan() { starts++ }
        override fun stopScan() { stops++ }
    }

    private class FakeRings(initial: List<IndexDevice>) : Rings {
        val devices = MutableStateFlow(initial)
        override val rings: IndexDevices = devices
        override fun warnIfNoCompanionAssociations() {}
    }

    private class FakeInterviewed(id: String, val onRemove: () -> Unit = {}) : InterviewedIndexDevice {
        override val identifier = IndexIdentifier(id)
        override val name = "Pebble Index ${id.takeLast(3)}"
        override val firmwareVersion = "1.2.3"
        override val serialNumber = "SN$id"
        override val mac = id
        override val updating = false
        override fun remove() = onRemove()
        override suspend fun measureRSSI(connectionTimeout: Duration) = RSSIMeasurement(-50f, -50f)
    }

    private class FakePairable(
        id: String,
        override val pairingState: IndexPairingState = IndexPairingState.NotPaired,
        val result: IndexPairingResult = IndexPairingResult.Success,
    ) : PairableIndexDevice {
        var pairCalls = 0
        override val identifier = IndexIdentifier(id)
        override val name = "Pebble Index ${id.takeLast(3)}"
        override val rssi = -60
        override val currentImage = IndexImage.Primary
        override suspend fun pair(): IndexPairingResult { pairCalls++; return result }
    }

    private class FakeRepairable(id: String) : RepairableIndexDevice {
        var forced = 0
        override val identifier = IndexIdentifier(id)
        override val name = "Pebble Index ${id.takeLast(3)}"
        override val rssi = -60
        override val currentImage = IndexImage.ProductionTest
        override suspend fun forceFailsafe() { forced++ }
    }

    private fun failsafe(id: String) = object : DiscoveredIndexDevice {
        override val identifier = IndexIdentifier(id)
        override val name = "Pebble Index ${id.takeLast(3)}"
        override val rssi = -60
        override val currentImage = IndexImage.Failsafe
    }

    private fun controller(vararg devices: IndexDevice): Triple<RingController, FakeScanning, FakeRings> {
        val scanning = FakeScanning()
        val rings = FakeRings(devices.toList())
        return Triple(RingController(scanning, rings), scanning, rings)
    }

    @Test
    fun noDevicesMeansNoRing() {
        assertEquals(RingStatus.NoRing, ringStatus(emptyList()))
    }

    @Test
    fun pairedRingWinsOverScanResults() {
        val status = ringStatus(listOf(FakePairable("AABBCCDDEE01"), FakeInterviewed("AABBCCDDEE02")))
        assertIs<RingStatus.Paired>(status)
        assertEquals("1.2.3", status.firmwareVersion)
        assertEquals("SNAABBCCDDEE02", status.serialNumber)
    }

    @Test
    fun pairingStatesAreReported() {
        val pairing = ringStatus(listOf(FakePairable("AABBCCDDEE01", IndexPairingState.Pairing)))
        assertIs<RingStatus.AvailableToPair>(pairing)
        assertTrue(pairing.pairing)
        val failed = ringStatus(
            listOf(FakePairable("AABBCCDDEE01", IndexPairingState.Error(IndexPairingResult.Success)))
        )
        assertIs<RingStatus.AvailableToPair>(failed)
        assertTrue(failed.pairingFailed)
    }

    @Test
    fun recoveryStatesAreReported() {
        assertIs<RingStatus.NeedsRestore>(ringStatus(listOf(FakeRepairable("AABBCCDDEE01"))))
        assertIs<RingStatus.Restoring>(ringStatus(listOf(failsafe("AABBCCDDEE01"))))
    }

    @Test
    fun findRingScansOnlyWhenNoRingIsPaired() {
        val (empty, emptyScan, _) = controller()
        assertEquals(FindRingResult.Scanning, empty.findRing())
        assertEquals(1, emptyScan.starts)

        val (paired, pairedScan, _) = controller(FakeInterviewed("AABBCCDDEE01"))
        assertEquals(FindRingResult.AlreadyPaired, paired.findRing())
        assertEquals(0, pairedScan.starts)
    }

    @Test
    fun pairTargetsTheMatchingRing() = runTest {
        val other = FakePairable("AABBCCDDEE01")
        val target = FakePairable("AABBCCDDEE02")
        val (ctl, _, _) = controller(other, target)
        assertSame(IndexPairingResult.Success, ctl.pair(target.identifier))
        assertEquals(1, target.pairCalls)
        assertEquals(0, other.pairCalls)
        assertNull(ctl.pair(IndexIdentifier("FFFFFFFFFFFF")))
    }

    @Test
    fun restoreForcesFailsafeAndRescans() = runTest {
        val ring = FakeRepairable("AABBCCDDEE01")
        val (ctl, scan, _) = controller(ring)
        assertTrue(ctl.restore(ring.identifier))
        assertEquals(1, ring.forced)
        assertEquals(1, scan.starts)
        assertFalse(ctl.restore(IndexIdentifier("FFFFFFFFFFFF")))
    }

    @Test
    fun forgetRemovesPairedRing() {
        var removed = false
        val (ctl, _, _) = controller(FakeInterviewed("AABBCCDDEE01") { removed = true })
        assertTrue(ctl.forget())
        assertTrue(removed)
        assertFalse(controller().first.forget())
    }

    @Test
    fun statusFollowsDeviceList() = runTest {
        val (ctl, _, rings) = controller()
        assertEquals(RingStatus.NoRing, ctl.status.first())
        rings.devices.value = listOf(FakeInterviewed("AABBCCDDEE01"))
        assertIs<RingStatus.Paired>(ctl.status.first())
    }
}

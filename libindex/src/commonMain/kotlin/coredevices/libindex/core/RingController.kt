package coredevices.libindex.core

import coredevices.libindex.Rings
import coredevices.libindex.Scanning
import coredevices.libindex.device.IndexIdentifier
import coredevices.libindex.device.IndexPairingResult
import coredevices.libindex.device.KnownIndexDevice
import coredevices.libindex.device.PairableIndexDevice
import coredevices.libindex.device.RepairableIndexDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * UI-independent entry point for ring setup and lifecycle. Screens, background services
 * and tests drive the ring through this instead of reaching into scan results directly.
 */
class RingController(
    private val scanning: Scanning,
    private val rings: Rings,
) {
    val status: Flow<RingStatus> = rings.rings.map(::ringStatus).distinctUntilChanged()
    val isScanning: StateFlow<Boolean> = scanning.isScanning

    fun currentStatus(): RingStatus = ringStatus(rings.rings.value)

    fun pairedRing(): KnownIndexDevice? =
        rings.rings.value.firstOrNull { it is KnownIndexDevice } as KnownIndexDevice?

    /** Starts looking for a ring unless one is already paired (only one ring is supported). */
    fun findRing(): FindRingResult {
        if (pairedRing() != null) return FindRingResult.AlreadyPaired
        scanning.startScan()
        return FindRingResult.Scanning
    }

    fun stopFinding() = scanning.stopScan()

    suspend fun pair(identifier: IndexIdentifier): IndexPairingResult? {
        val ring = rings.rings.value.firstOrNull {
            it is PairableIndexDevice && it.identifier == identifier
        } as PairableIndexDevice? ?: return null
        return ring.pair()
    }

    /** Puts a ring stuck on test firmware back into recovery, then rescans so it reappears. */
    suspend fun restore(identifier: IndexIdentifier): Boolean {
        val ring = rings.rings.value.firstOrNull {
            it is RepairableIndexDevice && it.identifier == identifier
        } as RepairableIndexDevice? ?: return false
        ring.forceFailsafe()
        scanning.startScan()
        return true
    }

    fun forget(): Boolean {
        val ring = pairedRing() ?: return false
        ring.remove()
        return true
    }
}

enum class FindRingResult { Scanning, AlreadyPaired }

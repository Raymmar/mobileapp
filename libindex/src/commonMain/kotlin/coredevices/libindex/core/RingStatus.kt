package coredevices.libindex.core

import coredevices.libindex.device.DiscoveredIndexDevice
import coredevices.libindex.device.IndexDevice
import coredevices.libindex.device.IndexIdentifier
import coredevices.libindex.device.IndexImage
import coredevices.libindex.device.IndexPairingState
import coredevices.libindex.device.InterviewedIndexDevice
import coredevices.libindex.device.KnownIndexDevice
import coredevices.libindex.device.PairableIndexDevice
import coredevices.libindex.device.RepairableIndexDevice

/** What the app should tell the user about their ring, derived only from libindex state. */
sealed interface RingStatus {
    data object NoRing : RingStatus

    data class Paired(
        val identifier: IndexIdentifier,
        val name: String,
        val firmwareVersion: String?,
        val serialNumber: String?,
        val updating: Boolean,
    ) : RingStatus

    data class AvailableToPair(
        val identifier: IndexIdentifier,
        val name: String,
        val pairing: Boolean,
        val pairingFailed: Boolean,
    ) : RingStatus

    data class NeedsRestore(val identifier: IndexIdentifier, val name: String) : RingStatus

    data class Restoring(val identifier: IndexIdentifier, val name: String) : RingStatus
}

fun ringStatus(devices: List<IndexDevice>): RingStatus {
    devices.firstOrNull { it is KnownIndexDevice }?.let { ring ->
        val interviewed = ring as? InterviewedIndexDevice
        return RingStatus.Paired(
            identifier = ring.identifier,
            name = ring.name,
            firmwareVersion = interviewed?.firmwareVersion,
            serialNumber = interviewed?.serialNumber,
            updating = interviewed?.updating == true,
        )
    }
    devices.firstOrNull { it is PairableIndexDevice }?.let { ring ->
        val state = (ring as PairableIndexDevice).pairingState
        return RingStatus.AvailableToPair(
            identifier = ring.identifier,
            name = ring.name,
            pairing = state == IndexPairingState.Pairing,
            pairingFailed = state is IndexPairingState.Error,
        )
    }
    devices.firstOrNull { it is RepairableIndexDevice }?.let {
        return RingStatus.NeedsRestore(it.identifier, it.name)
    }
    devices.firstOrNull { it is DiscoveredIndexDevice && it.currentImage == IndexImage.Failsafe }?.let {
        return RingStatus.Restoring(it.identifier, it.name)
    }
    return RingStatus.NoRing
}

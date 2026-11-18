package coredevices.coreapp.ui.screens.ringhome

import CommonRoutes
import CoreNav
import CoreRoute
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.russhwolf.settings.Settings
import coredevices.libindex.LibIndex
import coredevices.libindex.core.FindRingResult
import coredevices.libindex.core.RingController
import coredevices.libindex.core.RingStatus
import coredevices.libindex.device.InterviewedIndexDevice
import coredevices.pebble.PebbleDeepLinkHandler
import coredevices.pebble.ui.PebbleNavBarRoutes
import coredevices.pebble.ui.PebbleRoutes
import coredevices.pebble.ui.PermissionsIntroDialog
import coredevices.pebble.ui.RingItem
import coredevices.pebble.ui.TopBarParams
import coredevices.pebble.ui.hasSeenPermissionsIntro
import coredevices.pebble.ui.hasSeenRingOnboarding
import coredevices.pebble.ui.scanPermission
import coredevices.pebble.ui.setHasSeenPermissionsIntro
import coredevices.ring.ui.navigation.RingRoute
import coredevices.ring.ui.navigation.RingRoutes
import coredevices.ring.ui.navigation.addRingRoutes
import coredevices.ring.ui.screens.IndexScreen
import coredevices.util.CompanionDevice
import coredevices.util.CoreConfigHolder
import coredevices.util.PermissionRequester
import coredevices.util.PermissionResult
import coredevices.util.rememberUiContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
data object RingFeedTab

@Serializable
data object RingDeviceTab

enum class RingHomeTab(val label: String, val icon: ImageVector) {
    Feed("Feed", Icons.AutoMirrored.Outlined.Notes),
    Ring("Ring", Icons.Outlined.RadioButtonUnchecked),
    Settings("Settings", Icons.Outlined.Tune),
}

private fun RingHomeTab.route(): Any = when (this) {
    RingHomeTab.Feed -> RingFeedTab
    RingHomeTab.Ring -> RingDeviceTab
    RingHomeTab.Settings -> RingRoutes.Settings
}

fun launchTab(status: RingStatus): RingHomeTab =
    if (status is RingStatus.Paired) RingHomeTab.Feed else RingHomeTab.Ring

@Composable
fun RingHomeScreen(coreNav: CoreNav) {
    val ringController: RingController = koinInject()
    val libIndex: LibIndex = koinInject()
    val settings: Settings = koinInject()
    val coreConfigHolder: CoreConfigHolder = koinInject()
    val deepLinks: PebbleDeepLinkHandler = koinInject()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val startTab = remember { launchTab(ringController.currentStatus()) }

    LaunchedEffect(Unit) {
        val config = coreConfigHolder.config.value
        if (!config.enableIndex) coreConfigHolder.update(config.copy(enableIndex = true))
    }

    val rings by libIndex.rings.collectAsState()
    var hasSeenRingOnboarding by remember { mutableStateOf(settings.hasSeenRingOnboarding()) }
    var startingOnboarding = false
    if (rings.any { it is InterviewedIndexDevice } && !hasSeenRingOnboarding) {
        hasSeenRingOnboarding = true
        coreNav.navigateTo(CommonRoutes.RingOnboardingRoute)
        startingOnboarding = true
    }
    var hasSeenPermissionsIntro by remember { mutableStateOf(settings.hasSeenPermissionsIntro()) }
    if (!startingOnboarding && hasSeenRingOnboarding && !hasSeenPermissionsIntro) {
        PermissionsIntroDialog(onDismissRequest = {
            hasSeenPermissionsIntro = true
            settings.setHasSeenPermissionsIntro(true)
        })
    }

    fun selectTab(tab: RingHomeTab) {
        navController.navigate(tab.route()) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(Unit) {
        deepLinks.navigateToPebbleDeepLink.collect {
            if (it == null || it.consumed || it.route != PebbleNavBarRoutes.IndexRoute) return@collect
            it.consumed = true
            selectTab(RingHomeTab.Feed)
        }
    }

    val scopedCoreNav = remember(navController, coreNav) {
        object : CoreNav {
            override fun navigateTo(route: CoreRoute) {
                if (route is RingRoute) navController.navigate(route) else coreNav.navigateTo(route)
            }

            override fun goBack() {
                if (navController.previousBackStackEntry != null) {
                    navController.popBackStack()
                } else {
                    coreNav.goBack()
                }
            }

            override fun goBackToPebble() = coreNav.goBackToPebble()

            override fun replaceWith(route: CoreRoute) {
                if (route is RingRoute) {
                    navController.popBackStack()
                    navController.navigate(route)
                } else {
                    coreNav.replaceWith(route)
                }
            }
        }
    }
    val scrollToTop = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
    val topBarParams = remember {
        TopBarParams(
            searchAvailable = {},
            actions = {},
            title = {},
            overrideGoBack = MutableSharedFlow(),
            showSnackbar = { scope.launch { snackbarHostState.showSnackbar(it) } },
            scrollToTop = scrollToTop,
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val destination = backStackEntry?.destination
            NavigationBar {
                RingHomeTab.entries.forEach { tab ->
                    val selected = when (tab) {
                        RingHomeTab.Feed -> destination?.hasRoute<RingFeedTab>() == true
                        RingHomeTab.Ring -> destination?.hasRoute<RingDeviceTab>() == true
                        RingHomeTab.Settings -> destination?.hasRoute<RingRoutes.Settings>() == true
                    }
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (selected && tab == RingHomeTab.Feed) scrollToTop.tryEmit(Unit) else selectTab(tab)
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        RingHomeNavHost(
            navController = navController,
            startTab = startTab,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            coreNav = coreNav,
            scopedCoreNav = scopedCoreNav,
            topBarParams = topBarParams,
        )
    }
}

@Composable
private fun RingHomeNavHost(
    navController: NavHostController,
    startTab: RingHomeTab,
    modifier: Modifier,
    coreNav: CoreNav,
    scopedCoreNav: CoreNav,
    topBarParams: TopBarParams,
) {
    NavHost(navController, startDestination = startTab.route(), modifier = modifier) {
        composable<RingFeedTab> { IndexScreen(scopedCoreNav, topBarParams) }
        composable<RingDeviceTab> { RingDeviceScreen(coreNav, topBarParams) }
        addRingRoutes(scopedCoreNav)
    }
}

@Composable
fun RingDeviceScreen(coreNav: CoreNav, topBarParams: TopBarParams) {
    val ringController: RingController = koinInject()
    val libIndex: LibIndex = koinInject()
    val companionDevice: CompanionDevice = koinInject()
    val permissionRequester: PermissionRequester = koinInject()
    val uiContext = rememberUiContext()
    val scope = rememberCoroutineScope()
    val status by ringController.status.collectAsState(ringController.currentStatus())
    val scanning by ringController.isScanning.collectAsState()
    val rings by libIndex.rings.collectAsState()

    fun findRing() {
        val ctx = uiContext ?: return
        scope.launch {
            val permission = scanPermission()
            if (permission != null && permission in permissionRequester.missingPermissions.value &&
                permissionRequester.requestPermission(permission, ctx) != PermissionResult.Granted
            ) {
                topBarParams.showSnackbar("Bluetooth permission is needed to find your ring")
                return@launch
            }
            if (ringController.findRing() == FindRingResult.AlreadyPaired) {
                topBarParams.showSnackbar("Your ring is already paired")
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Text(
            "Your ring",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
        )
        RingStatusCard(status, scanning, onFind = ::findRing, onStop = ringController::stopFinding)
        if (rings.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Card(Modifier.fillMaxWidth()) {
                rings.forEach { ring ->
                    RingItem(
                        ring = ring,
                        scope = scope,
                        onRequestCompanion = { identifier ->
                            uiContext?.let { companionDevice.registerDevice(identifier, it, true) }
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = { coreNav.navigateTo(CommonRoutes.RingOnboardingRoute) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Ring guide and setup") }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = { coreNav.navigateTo(PebbleRoutes.WatchHomeRoute) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.Watch, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Pebble watch and plugin store")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RingStatusCard(status: RingStatus, scanning: Boolean, onFind: () -> Unit, onStop: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ringStatusTitle(status, scanning), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(ringStatusDetail(status, scanning), style = MaterialTheme.typography.bodyMedium)
            when {
                scanning -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = onStop) { Text("Stop searching") }
                }
                status is RingStatus.NoRing -> Button(onClick = onFind) { Text("Find my ring") }
            }
        }
    }
}

fun ringStatusTitle(status: RingStatus, scanning: Boolean): String = when (status) {
    RingStatus.NoRing -> if (scanning) "Looking for your ring" else "No ring paired"
    is RingStatus.Paired -> if (status.updating) "Updating ${status.name}" else status.name
    is RingStatus.AvailableToPair -> when {
        status.pairing -> "Pairing ${status.name}"
        status.pairingFailed -> "Pairing failed"
        else -> "Found ${status.name}"
    }
    is RingStatus.NeedsRestore -> "${status.name} needs a restore"
    is RingStatus.Restoring -> "Restoring ${status.name}"
}

fun ringStatusDetail(status: RingStatus, scanning: Boolean): String = when (status) {
    RingStatus.NoRing ->
        if (scanning) "Press the ring's button to wake it up." else "Pair your Index 01 to start capturing notes."
    is RingStatus.Paired -> listOfNotNull(
        status.firmwareVersion?.let { "Firmware $it" },
        status.serialNumber?.let { "Serial $it" },
    ).joinToString(" · ").ifEmpty { "Paired" }
    is RingStatus.AvailableToPair ->
        if (status.pairingFailed) "Tap Pair below to try again." else "Tap Pair below to connect it to this phone."
    is RingStatus.NeedsRestore -> "Tap Restore firmware below to put it back on its normal firmware."
    is RingStatus.Restoring -> "Keep the ring near your phone until this finishes."
}

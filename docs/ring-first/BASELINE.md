# Ring-first baseline (milestone 1)

Baseline taken at tag `2.0.0` (`9eeddf05`) of `coredevices/mobileapp`.

## Target device and platforms

- Device: **Pebble Index 01 ring** ("Index"/"Ring"), handled by `:libindex` (pairing, scanning, transfer storage) and `:experimental` (`coredevices.ring`: sync, recordings, agent, feed, settings, UI).
- Platforms: **Android** (`:androidApp` shell + `:composeApp`) and **iOS** (`iosApp` Swift shell + `ComposeApp` framework). Both must remain supported (CLAUDE.md platform rule).
- The ring is not always connected; it is scanned for continuously (`RingSync.kt:327-356`, `satelliteManager.startScanning()`).
- The ring recording pipeline (BLE audio capture → preprocessing → transcription → agent) must not change (CLAUDE.md).

## Baseline verification in sandbox (Linux, JDK 17, Android SDK 36)

| Check | Result |
| --- | --- |
| `./gradlew :androidApp:assembleDebug` | PASS – 81.8 MB debug APK |
| `:experimental:testAndroidHostTest :libindex:testAndroidHostTest :util:testAndroidHostTest :index-ai:testAndroidHostTest :mcp:allTests` | PASS – 1240 tests, 0 failures, 146 test classes |
| iOS build (`:composeApp:compileKotlinIosArm64`) | **Not runnable here** – requires macOS/Xcode (CI `ios` job runs on `macos-15`) |

## Module graph (before)

```
androidApp ─► composeApp ─► pebble ─► libpebble3 (watch)
                    │          └────► util, cactus, libindex
                    ├─► experimental (ring) ─► libpebble3, util, libindex, index-ai, mcp, cactus, resampler
                    ├─► libindex ─► libpebble3, index-ai, cactus
                    └─► util ─► libpebble3, libindex, index-ai, cactus
```

Every ring module reaches `:libpebble3` directly. No ring module depends on `:pebble` directly, but the app shell (`composeApp`) routes the ring UI through the watch home screen (`pebble/.../WatchHomeScreen.kt`).

## Watch coupling found

239 watch imports in 96 ring/shared files (experimental 200/73, libindex 12/4, util 27/19). Grouped:

| Concept | Where | Ring features using it | Decision |
| --- | --- | --- | --- |
| Plugin runtime (`Plugins`, `PluginMetadata`, sources/actions, JS plugins, permissions, `InstallReview`) | `ring/agent/pebble_plugin/*`, builtin_servlets, viewmodels, settings | Plugin destinations, agent tools, store installs, default destinations | **Keep** – move generic runtime into a ring-owned module; drop watch-only built-ins (`WatchSettingsPlugin`, `WatchStatePlugin`) and pairing-based filtering |
| Locker (plugin storage) | `LockerPluginLoader`, `AppstorePluginStore`, `viewmodelModule` | Installed plugin discovery | **Keep behaviour**, replace watch locker with a ring plugin store |
| Calendar (`Calendar`, `CalendarEntity`, `NewCalendarEvent`) | calendar servlet, `TargetCalendarDialog`, `AddIntegration` | Target calendar, agent event creation | **Keep** – ring-owned calendar interface |
| Geolocation (`SystemGeolocation`) | `AiLocationContext`, geofence editor | Agent location context, geofence reminders | **Keep** – ring-owned location interface |
| `AppContext`, BLE bond constants, `BleScanResult` | `libindex` pairing/scanning, util storage | Pairing, scanning, storage | **Keep** – ring-owned platform context / constants |
| Secure storage (`KeystoreSecureStorage`/`KeychainSecureStorage`) | `IntegrationTokenStorage` | Integration credentials | **Keep** – move to util |
| Connectivity (`plugin.Connections`) | `HybridTranscriptionService` | Offline fallback / warm-up | **Keep** – ring-owned constants |
| `RoomTypeConverters` | `CoreDatabase` | Shared DB | **Keep** needed converters only, schema unchanged |
| Watch notification mirroring (`WatchNotifier`, timeline pins/reminders, `notificationWindowState`) | `IndexNotificationManager.ios`, `ReminderScheduler.ios`, `ExperimentalDevices`, `util/WatchNotifications` | Mirrors ring notifications/reminders onto a watch | **Remove** (watch-only); phone notifications kept |
| `ReplyToWatchNotificationTool` | agent notifications servlet | Replies to the notification shown on the watch | **Remove** (watch-only) – needs sign-off, see below |
| Watch firmware schedule (`QuietTimeSchedule` in `CoreConfig`) | util | Watch firmware update window | **Remove** |
| Watch analytics heartbeat (`WatchHardwarePlatform`) | `CoreAnalytics` | Watch metrics | **Remove** watch fields, keep ring/plugin analytics |
| Misnamed util UI (`coredevices.pebble.ui.TopBarParams/IndexTabBadge/DebugOptions`) | util | Ring chrome, badge, debug | **Keep** – already in util; repackage |

## Capability inventory → parity checklist

See [PARITY_CHECKLIST.md](PARITY_CHECKLIST.md) for the capability-by-capability checklist, the evidence for each item (automated test or physical-ring test), and its status.

## Watch-only features that do not carry over

These are not ring capabilities. They go away with the watch:

- Watch pairing, watchfaces/watchapps, locker browsing for watch apps, PebbleKit JS for watchapps, watch firmware updates, health/activity, weather sync, watch notification relay, calls, music control *from the watch*, watch settings, QEMU/dev watch connection, watch onboarding.
- Mirroring ring notifications/reminders to a watch timeline.
- `ReplyToWatchNotificationTool` (agent tool: "reply to the notification displayed on the watch"). It needs a watch to function, so it is removed. **Flagged for the user** because it is an agent tool exposed in the ring app.

## App shell assembly (before)

- Android: `MainApplication` starts Koin with `watchModule` + ring modules; `MainActivity` renders `App()`; `PebbleService` is the foreground service that actually runs `RingSync` and `IndexNotificationManager` (must be kept, rebranded).
- iOS: `IOSDelegate` boots the same modules + `watchModule`; `RingDelegate.ios` starts `BackgroundRingService`.
- Navigation: `App()` → `PebbleRoutes.WatchHomeRoute` (watch tab shell) → Index tab → `IndexScreen`. Ring onboarding is triggered from the watch shell. General onboarding asks watch / ring / both.
- Required permissions are the union of watch and ring permissions.

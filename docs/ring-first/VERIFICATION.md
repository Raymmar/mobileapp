# Ring-first verification

Branch `ring-first`, built from tag `2.0.0` in sandbox `ring-mobileapp`.

## Scope

The original goal was to remove every watch dependency. The user later relaxed that: *"You do not need to strip out all the watch dependencies fully. Just take out the stuff we do not need but keep it in there if it makes it easier"* and *"The real goal here is just to make it ring first"*. So this branch:

- makes the ring the app's home, onboarding and navigation;
- adds a UI-independent ring core (`libindex/core`) with headless tests;
- keeps `:libpebble3` and `:pebble` as internal dependencies. Plugins, the plugin store, MCP and parts of settings are built on them, and they are ring capabilities;
- keeps the watch screens reachable from one link on the Ring tab ("Pebble watch and plugin store"), not the default experience.

No ring capability was removed. "Reply to Notifications" (`ReplyToWatchNotificationTool`, parity row 15w) only works with a paired watch. Removing it is a capability change that needs the user's approval, so it stays for now.

## What changed

| Area | Change |
|---|---|
| Ring core | `libindex/core/RingStatus.kt`: `ringStatus(devices)` turns libindex device state into one `RingStatus`: NoRing, AvailableToPair, Paired, NeedsRestore or Restoring. `libindex/core/RingController.kt`: find (scan unless already paired), pair, restore, forget, and status/scanning flows. Registered in Koin. No Compose, no watch imports. |
| Home | New `CommonRoutes.RingHomeRoute` / `RingHomeScreen` with three tabs: **Feed** (existing Index feed), **Ring** (status card, find/pair/restore list, ring guide, link to watch screens) and **Settings** (existing Index settings). The app starts here after onboarding instead of `WatchHomeRoute`. Opens on Feed when a ring is paired, otherwise on Ring. |
| Onboarding | The "Watch / Index 01 / Both" device picker is skipped. Get Started enables Index and goes to permissions, sign-in, then ring home. The copy is about the ring. First interview of a ring still opens the existing `RingOnboardingScreen`. |
| Deep links | `navbar/index` deep links and ring onboarding's "finish to feed" now open ring home. Other watch deep links still open the watch home. |

## Automated results

| Check | Result |
|---|---|
| `:androidApp:assembleDebug` | PASS |
| `:libindex:testAndroidHostTest --tests coredevices.libindex.core.*` (new ring core, no UI mounted) | PASS: 9 tests, 0 failures |
| Full suite: `:experimental:testAndroidHostTest :libindex:testAndroidHostTest :util:testAndroidHostTest :index-ai:testAndroidHostTest :mcp:allTests` | PASS: 1249 tests, 0 failures, 147 classes (baseline 1240 / 146; the 9 new ones are ring-core tests) |
| iOS (`:composeApp:compileKotlinIosArm64`) | **BLOCKED**: no macOS/Xcode in the sandbox. Not verified. |
| Android instrumented tests (`androidDeviceTest`) | **BLOCKED**: no emulator/KVM in the sandbox. Not run. |

## Physical-ring results

Physical-ring behavior is **unverified** until the user runs [HARDWARE_TEST_PLAN.md](HARDWARE_TEST_PLAN.md) H1–H18 on a real Index 01 and records the results there.

## Open items

- iOS build and on-device run (needs a Mac).
- Hardware checks H1–H18 (needs the user's ring).
- Decide whether to remove watch-notification reply (row 15w) and the watch-screen link.

# Physical-ring test plan

Run on a real Pebble Index 01 ring with the ring-first build, on each platform you ship (Android, iOS). Record the result in the table at the bottom (pass / fail + note). Automated tests cannot cover these; until a result is recorded the item is **unverified**.

Setup: uninstall the old app (or use a fresh device), install the ring-first build, have the ring charged and nearby. Note your app build (commit SHA) and ring firmware version (Settings → Ring).

| ID | Test | Steps | Expected |
|---|---|---|---|
| H1 | First run, permissions, sign-in | Launch fresh install | Ring welcome → permissions (Bluetooth, notifications, microphone, speech, reminders/alarms where applicable) → sign-in → pairing. No watch options appear anywhere. Denying a permission shows the in-app permission warning in Settings with a way to fix it. |
| H2 | Pair ring | Follow pairing; press ring button when asked | Ring discovered and paired; Home shows ring connected with battery / firmware. |
| H3 | Background discovery | Lock phone for 5 min, move ring out of range and back | Ring reconnects on its own; Android shows the persistent ring service notification; no crash in logs. |
| H4 | App restart / reboot | Force-close the app, then reboot the phone | Ring scanning resumes without opening the app (Android) / on next background wake (iOS). |
| H5 | Record a note | Hold ring button and speak "note: buy milk" | Recording transfers, is transcribed, appears in feed as a note in Todos/Notes. |
| H6 | Phone mic and typed input | Use Home's mic and text input | Same pipeline result as ring recording. |
| H7 | Ask a question | Hold and ask "what is 12 times 14" | Answer appears in feed / notification. |
| H8 | Manage recordings and lists | Open recording, play audio, retry, delete; create a list, add/complete item | All actions work; audio plays. |
| H9 | Destination | Set default notes destination to a connected integration (e.g. Notion/Obsidian/plugin) and record a note | Note is delivered to that destination. |
| H10 | Reminders | "Remind me in 2 minutes to stretch"; a location reminder; "add lunch tomorrow at noon to my calendar" | Notification fires on time; calendar event created; tapping notification opens the item. |
| H11 | Timers/alarms | "Set a timer for 1 minute" | Timer fires (iOS 26+ AlarmKit; Android clock app). |
| H12 | Button actions | Map double-click to play/pause, triple-click to a webhook or agent | Mapped actions run on press. |
| H13 | Webhook | Configure webhook, send test, then real recording | Delivery shown as succeeded in webhook status. |
| H14 | Plugins & MCP | Install a store plugin from destinations; add an MCP server | Permission review appears; plugin/MCP tools usable by the agent. |
| H15 | Notifications | Trigger an answer notification; use notification action / shortcut | Notification appears, actions work, auto-dismiss honours setting. |
| H16 | Firmware update | Settings → Ring → check for update (do **not** force-flash unless you intend to) | Current firmware shown; update available/none reported correctly. |
| H17 | Battery & diagnostics | Open battery and diagnostics, sync inspector, file a bug report | Data shown; bug report includes ring logs/traces, no watch sections. |
| H18 | Backup & export | Run backup, export ZIP | Backup completes; ZIP contains recordings/audio. |

## Results

| ID | Android | iOS | Notes |
|---|---|---|---|
| H1 | | | |
| H2 | | | |
| H3 | | | |
| H4 | | | |
| H5 | | | |
| H6 | | | |
| H7 | | | |
| H8 | | | |
| H9 | | | |
| H10 | | | |
| H11 | | | |
| H12 | | | |
| H13 | | | |
| H14 | | | |
| H15 | | | |
| H16 | | | |
| H17 | | | |
| H18 | | | |

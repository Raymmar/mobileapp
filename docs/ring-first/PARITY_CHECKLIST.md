# Ring capability parity checklist

Each ring capability from the 2.0.0 app, the evidence used to show it still works in the ring-first app, and its status.

- **Auto** = automated tests that run in CI / the sandbox (`testAndroidHostTest`, `allTests`).
- **Ring** = physical-ring test the user runs (IDs `H*` refer to [HARDWARE_TEST_PLAN.md](HARDWARE_TEST_PLAN.md)).
- Status: `baseline` = passed at 2.0.0; `pass` = passed on the ring-first branch; `pending-user` = waiting on a physical-ring result; `n/a` = not applicable.

| # | Capability | Key code | Auto evidence | Ring evidence | Status |
|---|---|---|---|---|---|
| 1 | Onboarding, sign-in, discover and pair Index | `RingOnboardingScreen`, `RingPairing`, `IndexDeviceManager`, `IndexPairing` | RingPairingKeyMonitorTest, RingListTest, PairedRingResolutionTest, PairedRingReconcileTest | H1, H2 | baseline |
| 2 | Continuous discovery, reconnect, background operation, scan watchdog | `RealScanning`, `RingSync.startSyncJob`, `RingScanWatchdog`, `PebbleService`→`RingService` | RingScanWatchdogTest, UsePendingIntentScanPreferenceTest | H3, H4 | baseline |
| 3 | Recording transfer and sync; stale/orphan transfer recovery | `RingSync`, `RingTransferRepository`, `RecordingProcessingQueue` | PersistentQueueSchedulerTest (+ Android device tests, not run here) | H5 | baseline |
| 4 | Phone-mic capture, typed commands, chat, voice replies | `ListenDialog`, `IndexFeedViewModel.submitText`, `TextOnlyRecordingOperation`, `ConversationResumeOperation` | TextChatModeTest, ConversationTurnsTest | H6 | baseline |
| 5 | Audio processing and transcription (pipeline unchanged) | `RecordingPreprocessor`, `ButterworthHighPassFilter`, `HybridTranscriptionService`, Cactus, resampler | RecordingPreprocessorTest, ButterworthHighPassFilterTest, WarmUpPolicyTest, ValidateContainsSpeechTest, SpeechModelPromptTest, Bcp47Test, LocalTimeoutTest, TranscriptionAnalyticsTest, PlatformModelStateTest, M4AReaderTest | H5, H7 | baseline |
| 6 | Recording management: browse, play, export, retry, delete, inspect | `RecordingDetailsViewModel`, `RecordingActions` | RecordingActionsTest, FullFeedViewModelComputeTest, RecordingFilesTest | H8 | baseline |
| 7 | AI assistant, intent routing, confirmation, JS/calc, bug report tool | `ring/agent/*`, `index-ai` | IntentAgentRouterTest, RecordingIntentClassifierTest, NenyaSystemOneAgentTest, FallbackAgentTest, SystemOneToolCallValidatorTest, AgentUserConfirmationTest, LocalModelSchemaCheckpointTest, IterativeAgentTest, CreateBugReportToolTest | H7 | baseline |
| 8 | Index feed, notes, lists, todos | `ItemFactory`, `DefaultListsBootstrap`, `IndexFeedViewModel`, `ObjectDetailViewModel` | IndexFeedViewModelComputeTest, AllListsViewModelComputeTest, NormalizeParentListsTest, ItemFactoryCreateFromSemanticResultTest, DefaultListsBootstrapTest, DefaultChildKindTest, ItemRepositoryDoneTransitionTest | H8 | baseline |
| 9 | Note/reminder destinations (local, Notion, Obsidian, Google Tasks, iOS Reminders, Tasker, plugins) | `BuiltInIntegrationProviders`, `IntegrationProviderRepository` | LocalNoteClientTest, GTasksIntegrationTest, ObsidianIntegrationTest, DelegatedIntegrationItemsTest, IntegrationProviderRepositoryTest, DefaultDestinationPrompterTest, LegacyDestinationsTest | H9 | baseline |
| 10 | Timed and location reminders, calendar events | reminders servlet, `ReminderScheduler`, `GeofenceLocationSyncService`, calendar servlet | UpcomingRemindersTest, ReminderReconcilerTest, ReminderDeepLinkResolverTest, ReminderCompleterTest, CreateListToolTest, CreateCalendarEventToolTest, TargetCalendarSeederTest, TargetCalendarDialogTest, AiLocationContextTest | H10 | baseline |
| 11 | Timers and alarms | clock servlet, `LocalClock` | LocalClockTest, LocalClockViewModelTest | H11 | baseline |
| 12 | Button customization and media control | `RingGestureRouting`, `IndexButtonActionHandler`, `ButtonSwitchboard` | GestureRoutingTest, IndexButtonActionHandlerTest, RingButtonViewModelTest, ButtonSwitchboardTest, DestinationCatalogTest | H12 | baseline |
| 13 | Webhooks | `ring/external/indexwebhook/*` | IndexWebhook* tests (9 classes), WebhookNeedsOwnTranscriptionTest, WebhookDeliveryErrorTest | H13 | baseline |
| 14 | MCP servers, plugins, store installs with permission review | `mcp`, `ring/agent/pebble_plugin`, `StorePluginInstaller` | HttpMcpIntegrationTest, LazyLoadingMcpSessionTest, ToolSearchTest, McpServerRepositoryTest, PluginActionsTest, IndexServletPluginsTest, PblPlugin* tests, PluginsServletTest, ReadSourceToolTest, PluginGrantRepositoryTest, BuiltInServletPluginTest | H14 | baseline |
| 15 | Phone notifications, shortcuts, auto-dismiss, reminder actions/deep links, Beeper messaging | `IndexNotificationManager`, messaging servlet | BeeperRecipientMatchingTest, ShareActionHandlerTest, CoreDeepLinkHandlerTest | H15 | baseline |
| 15w | Reply to notification shown on a **watch** | `ReplyToWatchNotificationTool` | ReplyToWatchNotificationToolTest | – | **watch-only; removal flagged** |
| 16 | Firmware OTA, battery, diagnostics, traces, sync inspector, bug reports | `RingOta`, `battery/`, `bugreport/`, `util/trace` | RingOtaTest, RingBatteryLifeStoreTest, IndexRebootLogStoreTest, RingRssiHeartbeatMetricsTest, RecentRecordingsExportTest | H16, H17 | baseline |
| 17 | Cloud backup, encryption, export | `BackupManager`, `encryption/`, `SyncManager` | BackupManagerTest, BackupExporterTest, BackupArchiveTest, BackupZipTest, RecordingUploadSyncHandlerTest, SyncManagerTest | H18 | baseline |
| 18 | Permissions nag UI and settings | `PermissionRequester`, (watch) `PermissionsScreen` → ring settings | AndroidPermissionRequesterTest | H1 | baseline |

## Ring-first verification results

Filled in on the ring-first branch: see the "Verification" section of the PR and [VERIFICATION.md](VERIFICATION.md).

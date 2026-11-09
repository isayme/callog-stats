# 2026-11-09 — 锁屏浮层、号码来源与通话记录匹配

## 决策

1. **锁屏浮层改用无障碍浮层。**
   - MIUI 即使授予“锁屏显示”权限，也会在锁屏时隐藏 `TYPE_APPLICATION_OVERLAY` 窗口。
   - 新增 `OverlayAccessibilityService`，启用后由该服务提供 `TYPE_ACCESSIBILITY_OVERLAY` 窗口，可显示在锁屏和系统来电界面之上。
   - 无障碍服务只提供窗口 token，不读取屏幕内容（`canRetrieveWindowContent="false"`），不消费无障碍事件。
   - 未启用无障碍服务时回退到 `TYPE_APPLICATION_OVERLAY`，普通场景不受影响。
   - 设置页新增“锁屏显示（无障碍）”授权入口；`readyForMonitoring` 允许“悬浮窗”或“无障碍浮层”任一可用。

2. **Android 10 的号码来源增加 `PhoneStateListener` 回退。**
   - Android 10 / MIUI 上，带 `EXTRA_INCOMING_NUMBER` 的 `PHONE_STATE` 广播需要 `READ_PRIVILEGED_PHONE_STATE`，普通应用只收到号码为空的广播。
   - 前台服务同时注册已废弃的 `PhoneStateListener`（`onCallStateChanged` 仍会为持有 `READ_CALL_LOG` 的应用提供号码），与广播共同驱动同一状态机。
   - 两条通道可能重复或先后到达：保留“首个非空号码生效、空号码不覆盖已有号码”的规则。

3. **通话记录改为内存归一化匹配。**
   - 部分 ROM（如 MIUI）在 `CallLog.Calls.NUMBER` 中保存带空格或国家码前缀的号码，`NUMBER IN (...)` 精确查询会漏匹配。
   - 查询本机通话记录后，在内存中用 `PhoneNumberMatcher.matches` 按当前匹配模式过滤。
   - 数据量限定为个人本机通话记录，查询在 `Dispatchers.IO` 执行，避免阻塞主线程。

## 已知限制

- Android 10 / MIUI 若连同 `PhoneStateListener` 也隐藏号码，则真实来电只能显示“无号码”状态，无法统计；后续可评估 `CallScreeningService` 角色。
- 无障碍浮层需要用户手动在系统设置中启用服务；部分 ROM 可能在应用更新后关闭该服务。
- 无障碍浮层位于状态栏之上，位置沿用原有 `y = 64dp` 的布局。

# 2026-11-14 — 未接来电类型覆盖不全导致来电次数为 0

## 问题

用户反馈：某号码来电数次、从未接听，浮层却显示无来电记录。

排查 K80 的 `CallLog` 后发现，MIUI 对未接来电使用多种 `type`：

| type | 含义 | K80 记录数 | duration 特征 |
| --- | --- | --- | --- |
| 1 | `INCOMING` | 4375 | 接通后为通话时长 |
| 2 | `OUTGOING` | 2484 | 接通后为通话时长 |
| 3 | `MISSED` | 2675 | 30–59 秒（响铃到超时） |
| 5 | `REJECTED` | 1719 | 3–6 秒（对方提前挂断） |
| 7 | `ANSWERED_EXTERNALLY` | 7 | 其他设备接听 |

原实现只把 `type=1` 与 `type=3` 计为来电，`type=5` 的响铃未接来电被完全忽略，因此“只响铃、从未接听”的号码会显示为 0 次。

## 决策

1. **统一归类所有入站类型。**
   - `INCOMING`、`MISSED`、`VOICEMAIL`、`REJECTED`、`BLOCKED`、`ANSWERED_EXTERNALLY` 均计入来电总数。
   - 这些类型同时参与重复来电（`recentIncomingCount`）与“首次来电”判定。
2. **接听与未接的判定。**
   - 接听：`INCOMING` 且 `duration > 0`，或 `ANSWERED_EXTERNALLY`。
   - 未接：其余入站类型，以及 `duration == 0` 的 `INCOMING`（部分 ROM 这样记录未接）。
3. **保持统计口径不变。** 只统计配置窗口内的记录；窗口外的记录仅影响“是否首次来电”。

## 验证

- 新增 4 个单元测试，覆盖 `REJECTED`、`BLOCKED`/`VOICEMAIL`、零时长 `INCOMING`、`ANSWERED_EXTERNALLY`，以及 `REJECTED` 参与重复来电计数。
- 单测、`lintDebug`、`assembleDebug` 均通过。

## 已知限制

- 各 ROM 对 `type` 的使用并不统一，`duration` 的语义也可能不同（例如 `REJECTED` 的 duration 可能是响铃时长）。当前按“类型优先”归类，未使用 duration 推断未接。

# 2026-11-10 — GitHub Actions 自动打包与发布

## 决策

1. **触发方式：推送 `v*` tag。**
   - `.github/workflows/release.yml` 监听 `push: tags: v*`，同时支持 `workflow_dispatch` 手动指定已存在的 tag。
   - 发布流程不依赖沙箱环境或本地机器，避免“本地能装、CI 出的包装不上”。

2. **版本号从 tag 提取。**
   - tag `v1.2.3` → `versionName=1.2.3`，`versionCode=10203`（`major*10000 + minor*100 + patch`）。
   - 通过 Gradle 属性 `-Pcallstats.versionName` / `-Pcallstats.versionCode` 注入，`app/build.gradle.kts` 在属性缺失时回退到默认值，本地构建行为不变。
   - tag 不符合 `vMAJOR.MINOR.PATCH` 时直接失败，避免发布错误版本。

3. **release 变体复用 debug 签名，并把 keystore 提交进仓库。**
   - 应用通过 sideload 分发，不经过应用商店，无需独立的发布签名。
   - CI 与本地构建必须使用同一签名，否则新发布的 APK 无法覆盖安装已安装版本。
   - 因此将标准 Android debug keystore（别名 `androiddebugkey`，密码为公开的 `android`）放在 `app/signing/debug.keystore` 并纳入版本控制。
   - debug 变体包含 `CallStateSimulatorReceiver`，不适合对外发布，所以发布流程构建 `assembleRelease`。

4. **产物。**
   - `call-stats-<versionName>.apk`
   - `SHA256SUMS.txt`
   - 使用 `softprops/action-gh-release@v2` 创建/更新对应 tag 的 Release，并自动生成 release notes。

## 发版步骤

```bash
git tag v0.1.0
git push origin v0.1.0
```

或在 Actions 页面手动运行 `Release APK` 并填写已存在的 tag。

## 已知限制

- 签名密钥是公开的 debug key，任何人都能伪造同签名 APK；本应用不涉及账号与支付，风险可接受。
- 若将来上架或需要防止伪造签名，应改为在仓库 Secrets 中存放正式 keystore。
- `versionCode` 依赖 semver 的 major/minor/patch 均在 `0-99` 范围内。

# 运动

离线个人运动追踪 App（桌面名「运动」）。原生 Kotlin + Jetpack Compose，纯黑暗色主题，数据存在本机 Room / SQLite，没有账号，也没有网络请求。

按「本周 → 本月 → 指标 → 设置」四栏使用：记有氧 / 力量 / 平衡，按周看阶段目标。计划页从本周入口进入，不占底栏。本月页是周一开头的月历；加号只出现在本月页，默认写入当前选中的日期。

## 规则（和界面一致）

- 一周从周一起算。本周页按周汇总；本月页是月历，点某一天看当天条目。
- 月历圆点：有氧绿、力量紫、平衡橙。本月汇总用原始分钟（有氧 / 力量）和平衡次数，不加权。
- 有氧计入分钟 = 时长 × 系数（低 0 / 中 1 / 高 2，固定不可改）。
- 力量按原始分钟；平衡按次数，可选目标默认 2 次/周，**不影响**本周是否达标。
- 本周达标 = 有氧计入 ≥ 目标 **且** 力量分钟 ≥ 目标。
- 渐进计划默认：阶段 1 = 60/40，阶段 2 = 80/50，阶段 3 及维持 = 100/60。这些分钟和「连续达标 N 周」升阶条件都可以在设置里改。阶段没有固定周数。连续 N 个**已结束周**达标升阶；未达标则连续达标清零、留在当前阶段。
- 类别名一律是「力量」，不是「无氧（力量）」。没有快走功能（日常快走在 App 外计入）。

## 安装 APK（侧载）

构建产物：

- `artifacts/exercise-tracker.apk`（debug 签名，约 17MB，可直接侧载）
- 本机构建：`./scripts/build-apk.sh` 会覆盖同一路径

手机上：

1. 把 `exercise-tracker.apk` 传到 Android 手机（至少 Android 8.0 / API 26）。
2. 打开该文件，允许「安装未知来源应用」。
3. 安装后打开「运动」。debug 签名，仅供自用；包名 / 签名不变，可覆盖安装升级。

## 图标候选

第一批 A–F 是单色薄荷绿。第二批 G–L 是彩色渐变方案，优先从这里选：

- `artifacts/icons/G-run.png` 橙粉跑步
- `artifacts/icons/H-rings.png` 三色活动环
- `artifacts/icons/I-pulse.png` 青绿脉搏
- `artifacts/icons/J-dong.png` 紫粉「动」
- `artifacts/icons/K-flame.png` 暖色火焰
- `artifacts/icons/L-combo.png` 哑铃 + 环
- `artifacts/icons/comparison-G-L.png` 对照表

L 的细化变体（几何更利落，对照表含 48px / 96px）：

- `artifacts/icons/L1-teal.png` 青绿 · 环内
- `artifacts/icons/L2-orange.png` 橙粉 · 更大叠环
- `artifacts/icons/L3-purple.png` 紫粉 · 斜角
- `artifacts/icons/L4-lime.png` 柠青 · 粗环
- `artifacts/icons/L5-multi.png` 多色环
- `artifacts/icons/L6-diagonal.png` 斜叠穿过
- `artifacts/icons/comparison-L-variants.png` 对照表

## 本地构建

需要 JDK 17+（21 也可）和 Android SDK（platform 35、build-tools 35.0.0）。

```bash
export ANDROID_HOME=$HOME/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
# 若还没有 SDK：
# sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"

./gradlew test assembleDebug
mkdir -p artifacts
cp app/build/outputs/apk/debug/app-debug.apk artifacts/exercise-tracker.apk
```

或直接：

```bash
./scripts/build-apk.sh
```

单测覆盖周汇总和阶段升级：`./gradlew test`。

CI（`.github/workflows/android.yml`）会跑同样的测试并上传 `exercise-tracker` artifact。

## 备份

设置页可以导出运动 CSV，以及备份 / 恢复整包 JSON（记录、自定义项目、指标、设置）。恢复会覆盖本机数据。

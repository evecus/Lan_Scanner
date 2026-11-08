# LAN Scanner · Kotlin Compose 重写版

把原来的 **Flutter 深色版** 局域网扫描器，用 **原生 Kotlin + Jetpack Compose (Material 3)** 完整重写，界面改为**白色 / 现代 / 灵动**风格。

## 界面变化

| 项目 | 原 Flutter 版 | 本 Kotlin Compose 版 |
|------|--------------|---------------------|
| 主题 | GitHub 暗色 `#0D1117` | 白色亮色 `#F5F7FC`，固定不跟随系统深色 |
| 主色 | 蓝 `#58A6FF` / 绿 `#238636` | 蓝紫渐变 `#4F6DF5 → #8B7CF6` + 青色点缀 |
| 卡片 | 深色描边方块、6dp 圆角 | 白色悬浮卡片、20~24dp 大圆角 |
| 头部 | 📡 emoji + 普通 AppBar | 呼吸式雷达波纹动画 + 无边框大标题 |
| 进度 | 3px 直线进度条 | 渐变圆角进度条 + 百分比 + 计数 |
| 按钮 | 实心绿 / 描边红 | 渐变胶囊按钮 + 按压缩放反馈 |
| 端口标签 | 深色描边数字 | 彩色语义标签（端口 · 服务名） |
| 设备图标 | emoji | Material 圆角矢量图标 + 渐变底 |
| 列表 | 无动画 | 逐项淡入上滑动画 |
| 空状态 | emoji + 两行灰字 | 雷达动画 + 引导文案 |
| 端口选择 | 点 chip 追加文本 | chip 选中高亮，可添加/移除 |

## 功能（与原版对齐）

- 输入 CIDR 网段（如 `192.168.1.0/24`）扫描在线设备
- 启动自动识别当前 WiFi 网段，也可点定位按钮重新获取
- 自定义端口，留空使用默认 13 个常用端口；「默认」展开可点选
- 并发 TCP 探测：连接成功 = 端口开放；连接被拒绝(RST) = 主机在线；超时 = 离线
- mDNS / Bonjour 设备发现（自行实现的轻量组播 DNS 解析，补充主机名与仅组播设备）
- 反向 DNS 解析主机名
- 实时进度、在线数量统计
- 点击复制 IP、清空结果、分享/导出扫描结果
- 无需 Root

### 默认端口

`21 22 23 80 443 445 1883 3389 5000 5555 8080 8443 9100`

## 技术栈

- Kotlin 1.9.24 / JVM 17
- Jetpack Compose + Material 3（Compose BOM 2024.06.00）
- Coroutines + Flow 并发扫描
- AndroidViewModel + StateFlow 状态管理
- minSdk 21 / targetSdk 34 / compileSdk 34

## 目录结构

```
app/src/main/kotlin/com/example/lan_scanner/
├── MainActivity.kt              # 入口
├── ScanViewModel.kt             # 状态、扫描调度、导出
├── data/Models.kt               # DeviceInfo / PortCatalog / UiState
├── scan/
│   ├── CidrParser.kt            # CIDR -> IP 列表
│   ├── PortParser.kt            # 自定义端口解析
│   ├── NetworkUtils.kt          # 自动获取 WiFi 网段
│   ├── ScannerService.kt        # 并发 TCP 端口扫描（channelFlow）
│   └── MdnsDiscovery.kt         # mDNS 组播发现 + DNS 报文解析
└── ui/
    ├── ScanScreen.kt            # 主界面组装
    ├── theme/                   # 颜色 / 字体 / 主题
    └── components/              # 雷达动画、输入面板、进度区、设备卡片、空状态
```

## APK 精简

| 措施 | 精简前 | 精简后 |
|------|--------|--------|
| R8 代码压缩 `isMinifyEnabled=true` | 37.37 MiB dex / 2 个（`material-icons-extended` 全量打入） | 1.79 MiB dex / 1 个 |
| 资源压缩 `isShrinkResources=true` | 0.43 MiB | 0.01 MiB |
| ABI 分包 `splits.abi include("arm64-v8a")` | 通用包 | 仅 `app-arm64-v8a-release.apk` |
| 语言裁剪 `resourceConfigurations=zh,en` | 依赖库全语言 | 仅中/英 |
| 依赖清理 | `ui-tooling-preview` 进入 release | 移除（工程无 `@Preview`） |
| **APK 体积** | **9.75 MiB** | **0.91 MiB（-90.7%）** |

`app/proguard-rules.pro` 中 `-keep class com.example.lan_scanner.** { *; }` 保证自身业务代码不被混淆或内联移除，R8 只裁第三方冗余，扫描 / mDNS / UI 逻辑无回归。

## 编译与发布

### GitHub Actions（推荐）

`.github/workflows/main.yml` **由原 Flutter 工程的 workflow 改写**，手动触发（Actions → Run workflow）：

| 输入 | 说明 |
|------|------|
| `tag` | Release 标签，如 `v2.0.0`（用于建 tag 与发布） |
| `release_notes` | Release 说明 |

先在 **Settings → Secrets and variables → Actions** 配置 4 个 secrets（与原 workflow 一致）：

| Secret | 说明 |
|--------|------|
| `KEYSTORE_BASE64` | `release.jks` 的 base64（如 `base64 -w0 release.jks`） |
| `STORE_PASSWORD` | keystore 密码 |
| `KEY_PASSWORD` | key 密码 |
| `KEY_ALIAS` | 别名 |

流程：注入 keystore → `./gradlew assembleRelease` → 定位 arm64 APK → 上传 artifact `android-apk` → 建 tag → 发布 GitHub Release（附件 `lan_scanner-arm64.apk`）。

> 签名方式与原 workflow 完全一致：secrets → `app/release.jks` + 项目根 `key.properties`，由 `app/build.gradle.kts` 的 `signingConfigs.release` 读取；亦支持同名环境变量兜底。

### 本地

需要 JDK 17 + Android SDK 34（`local.properties` 或 `ANDROID_HOME` 指向 SDK）：

```bash
./gradlew assembleDebug     # 调试包
./gradlew assembleRelease   # arm64 release
```

产物：`app/build/outputs/apk/release/app-arm64-v8a-release.apk`。

> 本地无 `key.properties` 时 release 自动回退 debug 签名以便直接安装；CI 注入 keystore 后自动切换为正式签名（已验证 APK 证书指纹与 keystore 一致）。

### 国内网络加速（可选）

`gradle-wrapper.properties` 默认使用官方 `services.gradle.org`。若下载 Gradle 发行包超时，把 `distributionUrl` 换成腾讯云镜像即可：

```
distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-8.7-bin.zip
```

依赖仓库已在 `settings.gradle.kts` 中预置腾讯 / 阿里云镜像，无需额外配置。

## 权限

| 权限 | 用途 |
|------|------|
| `INTERNET` | TCP 端口扫描 |
| `ACCESS_NETWORK_STATE` / `ACCESS_WIFI_STATE` | 读取当前 WiFi IP |
| `CHANGE_WIFI_MULTICAST_STATE` | mDNS 组播发现 |

## 免责声明

本工具仅供网络管理与学习用途，请勿扫描未授权的网络。

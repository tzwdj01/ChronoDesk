# ChronoDesk Android 构建与部署指南 (ANDROID_BUILD.md)

## 一、环境要求

本项目为标准 Android 原生应用（Kotlin + Jetpack Compose + Material 3）。

- **操作系统**: macOS / Linux / Windows
- **JDK 版本**: OpenJDK 17 或 21（推荐 OpenJDK 17 LTS）
- **Android SDK**:
  - `compileSdk`: 34 (Android 14)
  - `minSdk`: 26 (Android 8.0 Oreo) - 确保支持 95%+ 以上旧机型作为智能桌面时钟
  - `targetSdk`: 34
  - `buildToolsVersion`: 34.0.0
- **Gradle**: 8.4+ (使用 Gradle Wrapper `gradlew`)
- **Android Gradle Plugin (AGP)**: 8.3.0+
- **Kotlin**: 1.9.22+ (Compose Compiler 1.5.8)

---

## 二、环境变量配置

在 macOS / Linux 下，建议在终端环境（如 `~/.zshrc` 或 `~/.bashrc`）配置：

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
```

项目中提供了 `local.properties` 模板，指定 SDK 路径：
```properties
sdk.dir=/opt/homebrew/share/android-commandlinetools
```

---

## 三、常用构建命令

### 1. 运行单元测试
```bash
./gradlew testDebugUnitTest
```
该命令会自动运行所有数据解析、时区转换、Keep-Last-Good 降级策略、双账号隔离与用量模型映射的单元测试。

### 2. 编译并打包 Debug APK
```bash
./gradlew assembleDebug
```
产物位置：
`app/build/outputs/apk/debug/app-debug.apk`

### 3. 安装到已连接设备或模拟器
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
# 启动应用
adb shell am start -n com.chronodesk.app/.MainActivity
```

---

## 四、签名与 Release 打包说明

为保障安全，正式 Release APK 请使用私有 Keystore 签名，切勿将 `keystore.jks` 或密钥口令提交至版本控制：

1. 生成签名密钥：
   ```bash
   keytool -genkey -v -keystore chronodesk-release.jks -alias chronodesk -keyalg RSA -keysize 2048 -validity 10000
   ```
2. 在环境变量中设置 `KEYSTORE_FILE`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`。
3. 执行 `./gradlew assembleRelease`。

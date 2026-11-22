# ChronoDesk Proguard / R8 Rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# 保留 Gson 和 Kotlin 序列化注解字段
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @kotlinx.serialization.SerialName <fields>;
}

# 保留数据模型与网络帧对象（Codex、DeepSeek、Gemini、TeslaMate、Open-Meteo、通用设置备份 JSON）
-keep class com.chronodesk.app.data.model.** { *; }
-keep class com.chronodesk.app.data.network.** { *; }
-keep class com.chronodesk.app.data.repository.GeneralSettingsBackup { *; }
-keep class com.chronodesk.app.data.repository.SettingsValidationResult { *; }

# 忽略第三方网络库已知非致命告警
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

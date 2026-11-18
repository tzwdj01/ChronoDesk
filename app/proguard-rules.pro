# ChronoDesk Proguard Rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.chronodesk.app.data.model.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

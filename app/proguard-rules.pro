# NEXUS Browser ProGuard / R8 Rules
-keep class org.mozilla.geckoview.** { *; }
-dontwarn org.mozilla.geckoview.**
-keep class org.mozilla.gecko.** { *; }
-dontwarn org.mozilla.gecko.**
-dontwarn java.beans.**
-dontwarn org.yaml.snakeyaml.**

# Keep Room entities & DAOs
-keep class com.nexus.browser.core.database.** { *; }

# Keep Serialization / JSON models
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Room entities / daos
-keep class com.ste03.smartscan.data.** { *; }

# ML Kit / Play Services
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# OpenCV — JNI-heavy
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**

# security-crypto / Tink — errorprone annotations are compile-time only
-dontwarn com.google.errorprone.annotations.**

# PDFBox (Android port) — heavy reflection use
-keep class com.tom_roush.** { *; }
-dontwarn com.tom_roush.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**

# Coil
-keep class coil.** { *; }

# kotlinx.coroutines
-dontwarn kotlinx.coroutines.**

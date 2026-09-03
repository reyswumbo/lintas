# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses, Signature, ExceptionTable
-keep class kotlinx.serialization.Serializable
-keep,includedescriptorclasses class com.lintas.app.data.** { *; }
-keepclassmembers class com.lintas.app.data.** { *** Companion; *** companion; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**

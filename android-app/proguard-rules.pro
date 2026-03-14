# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.onetill.shared.**$$serializer { *; }
-keepclassmembers class com.onetill.shared.** {
    *** Companion;
}
-keepclasseswithmembers class com.onetill.shared.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Stripe Terminal SDK
-keep class com.stripe.stripeterminal.** { *; }
-dontwarn com.stripe.stripeterminal.**

# Koin
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# ML Kit Barcode Scanning
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Napier
-keep class io.github.aakira.napier.** { *; }

# Coil
-keep class coil3.** { *; }
-dontwarn coil3.**

# OkHttp (used by Coil's Ktor engine)
-dontwarn okhttp3.**
-dontwarn okio.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

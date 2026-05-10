-keepattributes Signature
-keepattributes *Annotation*
-keep class com.rykle.kmregistratie.data.remote.dto.** { *; }
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

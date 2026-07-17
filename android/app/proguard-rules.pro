# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.docscan.pro.**$$serializer { *; }
-keepclassmembers class com.docscan.pro.** {
    *** Companion;
}
-keepclasseswithmembers class com.docscan.pro.** {
    kotlinx.serialization.KSerializer serializer(...);
}

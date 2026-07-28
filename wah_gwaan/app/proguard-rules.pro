# kotlinx.serialization keeps
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class bs.wahgwaan.data.network.** {
    *** Companion;
}
-keepclasseswithmembers class bs.wahgwaan.data.network.** {
    kotlinx.serialization.KSerializer serializer(...);
}

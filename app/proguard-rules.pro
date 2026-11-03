# Keep the upstream Onionmasq Android bridge and its JNI entry points.
-keep class org.torproject.onionmasq.** { *; }
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

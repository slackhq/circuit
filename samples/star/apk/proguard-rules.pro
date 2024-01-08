# Can't obfuscate when generating baseline profiles
-dontobfuscate

# Proguard rules from a yet-unreleased Retrofit release. Needed for newer R8 versions
# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Referenced by kotlinx-datetime
-dontwarn kotlinx.serialization.KSerializer
-dontwarn kotlinx.serialization.Serializable
-dontwarn org.slf4j.impl.StaticLoggerBinder
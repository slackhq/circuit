# ReflectiveSerializableCircuitSaver stores class names in saved state and resolves them with
# Class.forName() on restore. Keep implementation names so lookup works after obfuscation.
-keepnames class * implements com.slack.circuit.runtime.screen.CircuitSaveable

# Retain reflective serializer() lookup for @Serializable CircuitSaveables.
-if @kotlinx.serialization.Serializable class ** implements com.slack.circuit.runtime.screen.CircuitSaveable
-keepclassmembers class <1> {
  static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** implements com.slack.circuit.runtime.screen.CircuitSaveable
-keepclassmembers class <1>$Companion {
  kotlinx.serialization.KSerializer serializer(...);
}

# Serializable objects are looked up via their INSTANCE field.
-if @kotlinx.serialization.Serializable class ** implements com.slack.circuit.runtime.screen.CircuitSaveable
-keepclassmembers class <1> {
  public static <1> INSTANCE;
  kotlinx.serialization.KSerializer serializer(...);
}

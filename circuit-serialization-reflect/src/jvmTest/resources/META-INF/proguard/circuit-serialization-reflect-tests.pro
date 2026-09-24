-keep class com.slack.circuit.serialization.reflect.ReflectiveSerializableCircuitSaverR8Test {
  public <init>();
  @org.junit.Test public void *(...);
}

# Gradle uses reflection to invoke JUnit, including fields such as Description.TEST_MECHANISM.
-keep,includedescriptorclasses class org.junit.** {
  *;
}

# Keep @Test, @Ignore, and other runtime annotations used by the test runner.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

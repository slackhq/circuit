// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.serialization

import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.serialization.ClassDiscriminatorMode
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import com.slack.circuit.runtime.screen.CircuitSaveable
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.screen.restorePopResult
import com.slack.circuit.runtime.screen.restoreScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

object TestScope

@CircuitSerializable(TestScope::class) data class StringScreen(val value: String) : Screen

@Serializable data object ObjectScreen : Screen

@Serializable data class IntPopResult(val value: Int) : PopResult

@Serializable data class StringPopResult(val value: String) : PopResult

@Serializable data class DefaultScreen(val value: String = "default") : Screen

@Serializable data class ScreenWithNestedScreen(val screen: Screen?) : Screen

@Serializable data class ScreenWithNestedPopResult(val result: PopResult) : Screen

@CircuitSerializable(TestScope::class)
@Serializable(with = CustomStringScreenSerializer::class)
data class CustomStringScreen(val value: String) : Screen

object CustomStringScreenSerializer : KSerializer<CustomStringScreen> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("CustomStringScreen", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: CustomStringScreen) {
    encoder.encodeString("custom:${value.value}")
  }

  override fun deserialize(decoder: Decoder): CustomStringScreen {
    return CustomStringScreen(decoder.decodeString().removePrefix("custom:"))
  }
}

data class UnregisteredScreen(val value: String) : Screen

class SerializableCircuitSaverTest {

  private val saver =
    SerializableCircuitSaver(
      listOf(
        registration(StringScreen::class, StringScreen.serializer()),
        registration(ObjectScreen::class, ObjectScreen.serializer()),
        registration(IntPopResult::class, IntPopResult.serializer()),
        registration(StringPopResult::class, StringPopResult.serializer()),
        registration(CustomStringScreen::class, CustomStringScreen.serializer()),
        registration(ScreenWithNestedScreen::class, ScreenWithNestedScreen.serializer()),
        registration(
          ScreenWithNestedPopResult::class,
          ScreenWithNestedPopResult.serializer(),
        ),
      )
    )

  @Test
  fun screen_round_trip() {
    val screen = StringScreen("hello")
    val saved = assertNotNull(saver.save(screen))
    assertEquals(screen, saver.restoreScreen<StringScreen>(saved))
  }

  @Test
  fun object_screen_round_trip() {
    val saved = assertNotNull(saver.save(ObjectScreen))
    assertEquals(ObjectScreen, saver.restoreScreen<ObjectScreen>(saved))
  }

  @Test
  fun pop_result_round_trip() {
    val result = IntPopResult(42)
    val saved = assertNotNull(saver.save(result))
    assertEquals(result, saver.restorePopResult<IntPopResult>(saved))
  }

  @Test
  fun custom_serializer_round_trip() {
    val screen = CustomStringScreen("hello")
    val saved = assertNotNull(saver.save(screen))
    assertEquals(screen, saver.restoreScreen<CustomStringScreen>(saved))
  }

  @Test
  fun nested_screen_round_trip() {
    val screen = ScreenWithNestedScreen(StringScreen("hello"))
    val saved = assertNotNull(saver.save(screen))
    assertEquals(screen, saver.restoreScreen<ScreenWithNestedScreen>(saved))
  }

  @Test
  fun nested_pop_result_round_trip() {
    val screen = ScreenWithNestedPopResult(IntPopResult(42))
    val saved = assertNotNull(saver.save(screen))
    assertEquals(screen, saver.restoreScreen<ScreenWithNestedPopResult>(saved))
  }

  @OptIn(ExperimentalSerializationApi::class)
  @Test
  fun registrations_create_a_polymorphic_module() {
    val module =
      circuitSerializersModule(listOf(registration(StringScreen::class, StringScreen.serializer())))

    assertSame(
      StringScreen.serializer(),
      module.getPolymorphic(CircuitSaveable::class, StringScreen("hello")),
    )
  }

  @Test
  fun nested_values_use_configuration_registrations() {
    val configuration = SavedStateConfiguration {
      serializersModule = SerializersModule {
        polymorphic(CircuitSaveable::class) {
          subclass(StringScreen::class)
          subclass(IntPopResult::class)
          subclass(ScreenWithNestedScreen::class)
          subclass(ScreenWithNestedPopResult::class)
        }
      }
    }
    val configuredSaver = SerializableCircuitSaver(configuration)

    val screen = ScreenWithNestedScreen(StringScreen("hello"))
    assertEquals(
      screen,
      configuredSaver.restoreScreen<ScreenWithNestedScreen>(
        assertNotNull(configuredSaver.save(screen))
      ),
    )
    val screenWithResult = ScreenWithNestedPopResult(IntPopResult(42))
    assertEquals(
      screenWithResult,
      configuredSaver.restoreScreen<ScreenWithNestedPopResult>(
        assertNotNull(configuredSaver.save(screenWithResult))
      ),
    )
  }

  @Test
  fun existing_screen_registration_is_preserved() {
    val configuration = SavedStateConfiguration {
      serializersModule = SerializersModule {
        polymorphic(Screen::class) { subclass(StringScreen::class) }
      }
    }
    val configuredSaver =
      SerializableCircuitSaver(
        listOf(
          registration(StringScreen::class, StringScreen.serializer()),
          registration(ScreenWithNestedScreen::class, ScreenWithNestedScreen.serializer()),
        ),
        configuration,
      )

    val screen = ScreenWithNestedScreen(StringScreen("hello"))
    assertEquals(
      screen,
      configuredSaver.restoreScreen<ScreenWithNestedScreen>(
        assertNotNull(configuredSaver.save(screen))
      ),
    )
  }

  @Test
  fun existing_screen_default_provider_is_preserved() {
    val configuration = SavedStateConfiguration {
      serializersModule = SerializersModule {
        polymorphicDefaultSerializer(Screen::class) { value ->
          if (value is DefaultScreen) DefaultScreenPolymorphicSerializer else null
        }
        polymorphicDefaultDeserializer(Screen::class) { className ->
          if (className == DefaultScreenPolymorphicSerializer.descriptor.serialName) {
            DefaultScreenPolymorphicSerializer
          } else {
            null
          }
        }
      }
    }
    val configuredSaver =
      SerializableCircuitSaver(
        listOf(registration(ScreenWithNestedScreen::class, ScreenWithNestedScreen.serializer())),
        configuration,
      )

    val screen = ScreenWithNestedScreen(DefaultScreen())
    assertEquals(
      screen,
      configuredSaver.restoreScreen<ScreenWithNestedScreen>(
        assertNotNull(configuredSaver.save(screen))
      ),
    )
  }

  @OptIn(ExperimentalSerializationApi::class)
  @Test
  fun nested_screen_rejects_a_pop_result_discriminator() {
    val configuration = SavedStateConfiguration {
      serializersModule =
        circuitSerializersModule(
            listOf(registration(IntPopResult::class, IntPopResult.serializer()))
          )
          .withCircuitSaveableFallbacks()
    }
    val saved =
      encodeToSavedState(
        PolymorphicSerializer(CircuitSaveable::class),
        IntPopResult(42),
        configuration,
      )

    assertFailsWith<SerializationException> {
      decodeFromSavedState(PolymorphicSerializer(Screen::class), saved, configuration)
    }
  }

  @Test
  fun registration_module_is_combined_with_existing_configuration() {
    val configuration = SavedStateConfiguration {
      serializersModule = SerializersModule {
        polymorphic(CircuitSaveable::class) { subclass(StringScreen::class) }
      }
      encodeDefaults = true
    }
    val combinedSaver =
      SerializableCircuitSaver(
        listOf(registration(IntPopResult::class, IntPopResult.serializer())),
        configuration,
      )

    val screen = StringScreen("existing")
    val result = IntPopResult(42)
    assertEquals(
      screen,
      combinedSaver.restoreScreen<StringScreen>(assertNotNull(combinedSaver.save(screen))),
    )
    assertEquals(
      result,
      combinedSaver.restorePopResult<IntPopResult>(assertNotNull(combinedSaver.save(result))),
    )
  }

  @Test
  fun existing_configuration_options_are_preserved() {
    val configuration = SavedStateConfiguration {
      encodeDefaults = true
      classDiscriminatorMode = ClassDiscriminatorMode.ALL_OBJECTS
    }
    val configuredSaver =
      SerializableCircuitSaver(
        listOf(registration(DefaultScreen::class, DefaultScreen.serializer())),
        configuration,
      )

    val saved = assertIs<SavedState>(configuredSaver.save(DefaultScreen()))
    saved.read {
      getSavedState("value").read {
        assertTrue("type" in this)
        assertTrue("value" in this)
      }
    }
  }

  @Test
  fun conflicting_registration_fails() {
    val configuration = SavedStateConfiguration {
      serializersModule = SerializersModule {
        polymorphic(CircuitSaveable::class) { subclass(StringScreen::class) }
      }
    }

    assertFailsWith<IllegalArgumentException> {
      SerializableCircuitSaver(
        listOf(registration(StringScreen::class, AlternateStringScreenSerializer)),
        configuration,
      )
    }
  }

  @Test
  fun duplicate_registrations_fail() {
    assertFailsWith<IllegalArgumentException> {
      circuitSerializersModule(
        listOf(
          registration(StringScreen::class, StringScreen.serializer()),
          registration(StringScreen::class, AlternateStringScreenSerializer),
        )
      )
    }
  }

  @Test
  fun raw_values_restore_without_reporting_an_error() {
    var reported: Throwable? = null
    val reportingSaver = SerializableCircuitSaver(onRestoreError = { reported = it })
    val screen = StringScreen("legacy")
    val result = IntPopResult(42)

    assertEquals(screen, reportingSaver.restoreScreen<StringScreen>(screen))
    assertEquals(result, reportingSaver.restorePopResult<IntPopResult>(result))
    assertNull(reported)
  }

  @Test
  fun screen_cannot_restore_as_pop_result() {
    val screen = StringScreen("hello")
    val saved = assertNotNull(saver.save(screen))
    assertFailsWith<IllegalStateException> { saver.restorePopResult<IntPopResult>(saved) }
    var mismatched: CircuitSaveable? = null
    assertNull(saver.restorePopResult<IntPopResult>(saved, onTypeMismatch = { mismatched = it }))
    assertEquals(screen, mismatched)
  }

  @Test
  fun pop_result_cannot_restore_as_screen() {
    val result = IntPopResult(42)
    val saved = assertNotNull(saver.save(result))
    assertFailsWith<IllegalStateException> { saver.restoreScreen<StringScreen>(saved) }
    var mismatched: CircuitSaveable? = null
    assertNull(saver.restoreScreen<StringScreen>(saved, onTypeMismatch = { mismatched = it }))
    assertEquals(result, mismatched)
  }

  @Test
  fun screen_subtype_mismatch_fails_by_default() {
    val screen = StringScreen("hello")
    val saved = assertNotNull(saver.save(screen))
    assertFailsWith<IllegalStateException> { saver.restoreScreen<ObjectScreen>(saved) }
    var mismatched: CircuitSaveable? = null
    assertNull(saver.restoreScreen<ObjectScreen>(saved, onTypeMismatch = { mismatched = it }))
    assertEquals(screen, mismatched)
  }

  @Test
  fun pop_result_subtype_mismatch_fails_by_default() {
    val result = StringPopResult("hello")
    val saved = assertNotNull(saver.save(result))
    assertFailsWith<IllegalStateException> { saver.restorePopResult<IntPopResult>(saved) }
    var mismatched: CircuitSaveable? = null
    assertNull(saver.restorePopResult<IntPopResult>(saved, onTypeMismatch = { mismatched = it }))
    assertEquals(result, mismatched)
  }

  @Test
  fun save_unregistered_screen_fails() {
    assertFailsWith<IllegalArgumentException> { saver.save(UnregisteredScreen("nope")) }
  }

  @Test
  fun restore_unregistered_screen_returns_null() {
    val saved = assertNotNull(saver.save(StringScreen("hello")))
    val unconfiguredSaver = SerializableCircuitSaver()
    var absentHandled = false
    assertNull(
      unconfiguredSaver.restoreScreen<StringScreen>(
        saved,
        onAbsent = { absentHandled = true },
      )
    )
    assertTrue(absentHandled)
  }

  @Test
  fun restore_error_reports_to_callback() {
    val saved = assertNotNull(saver.save(StringScreen("hello")))
    var reported: Throwable? = null
    val unconfiguredSaver = SerializableCircuitSaver(onRestoreError = { reported = it })
    assertNull(unconfiguredSaver.restoreScreen<StringScreen>(saved))
    assertNotNull(reported)
  }

  @Test
  fun restore_unexpected_value_returns_null() {
    assertNull(saver.restoreScreen<StringScreen>("garbage"))
  }
}

private object AlternateStringScreenSerializer :
  KSerializer<StringScreen> by StringScreen.serializer()

private object DefaultScreenPolymorphicSerializer : KSerializer<Screen> {
  override val descriptor: SerialDescriptor = DefaultScreen.serializer().descriptor

  override fun serialize(encoder: Encoder, value: Screen) {
    encoder.encodeSerializableValue(DefaultScreen.serializer(), value as DefaultScreen)
  }

  override fun deserialize(decoder: Decoder): Screen {
    return decoder.decodeSerializableValue(DefaultScreen.serializer())
  }
}

private fun <T : CircuitSaveable> registration(
  subclass: kotlin.reflect.KClass<T>,
  serializer: KSerializer<T>,
): CircuitSerializerRegistration = CircuitSerializerRegistration { builder ->
  builder.subclass(subclass, serializer)
}

package amazon.lastmile.inject

import com.slack.circuit.codegen.annotations.CircuitInject
import kotlin.reflect.KClass
import software.amazon.lastmile.kotlin.inject.anvil.`internal`.Origin

// TODO temporary until https://github.com/amzn/kotlin-inject-anvil/issues/24
@Origin(value = CircuitInject::class)
public val comSlackCircuitCodegenAnnotationsCircuitInject: KClass<CircuitInject> =
    CircuitInject::class

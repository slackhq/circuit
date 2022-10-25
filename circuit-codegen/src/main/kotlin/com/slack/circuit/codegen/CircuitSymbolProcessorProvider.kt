package com.slack.circuit.codegen

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.slack.circuit.CircuitInject
import com.slack.circuit.Presenter
import com.slack.circuit.Ui
import com.squareup.kotlinpoet.ksp.toTypeName

private val CIRCUIT_INJECT_ANNOTATION = CircuitInject::class.java.canonicalName
private val CIRCUIT_PRESENTER = Presenter::class.java.canonicalName
private val CIRCUIT_UI = Ui::class.java.canonicalName

@AutoService(CircuitSymbolProcessorProvider::class)
public class CircuitSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return CircuitSymbolProcessor(environment.logger, environment.codeGenerator)
    }
}

private class CircuitSymbolProcessor(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getSymbolsWithAnnotation(CIRCUIT_INJECT_ANNOTATION).forEach {
            check(it is KSClassDeclaration)
            // @CircuitInject<HomeScreen>
            // class HomePresenter(..) : Presenter<State>
            val circuitInjectAnnotation = it.annotations.first {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == CIRCUIT_INJECT_ANNOTATION
            }
            val screenType =
                circuitInjectAnnotation.annotationType.element!!.typeArguments.get(0).toTypeName()

           val factoryType = it.getAllSuperTypes().mapNotNull{
                when (it.declaration.qualifiedName?.asString()) {
                    CIRCUIT_UI -> FactoryType.UI
                    CIRCUIT_PRESENTER -> FactoryType.PRESENTER
                    else -> null
                }
            }.first()

            val className = it.simpleName.getShortName()
            val packageName = it.packageName.getShortName()

        }
        return emptyList()
    }

}

private enum class FactoryType {
    PRESENTER,
    UI
}

/**
 * @ContributesTo(AppScope::class)
class HomeUiFactory @Inject constructor() : UiFactory {
override fun create(screen: Screen): ScreenView? {
return when (screen) {
is HomeScreen -> ScreenView(homeUi())
else -> null
}
}
}

private fun homeUi() = ui<HomeState, HomeEvent> { state, eventSink -> Home(state, eventSink) |
 */
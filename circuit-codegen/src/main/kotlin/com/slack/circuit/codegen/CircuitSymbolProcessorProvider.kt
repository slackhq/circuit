package com.slack.circuit.codegen

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated

@AutoService(CircuitSymbolProcessorProvider::class)
public class CircuitSymbolProcessorProvider : SymbolProcessorProvider{
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
       return CircuitSymbolProcessor(environment.logger, environment.codeGenerator)
    }
}

private class CircuitSymbolProcessor(
    private val logger : KSPLogger,
    private val codeGenerator: CodeGenerator
) : SymbolProcessor{
    override fun process(resolver: Resolver): List<KSAnnotated> {
        TODO("Not yet implemented")
    }

}
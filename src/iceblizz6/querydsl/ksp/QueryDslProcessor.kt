package iceblizz6.querydsl.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.writeTo

class QueryDslProcessor(
    private val settings: KspSettings,
    private val codeGenerator: CodeGenerator
) : SymbolProcessor {
    val typeProcessor = QueryModelExtractor(settings)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (settings.enable) {
            QueryModelType.entries.forEach { type ->
                resolver.getSymbolsWithAnnotation(type.associatedAnnotation)
                    .map { it as KSClassDeclaration }
                    .filter { isIncluded(it) }
                    .forEach { declaration -> typeProcessor.add(declaration, type) }
            }
        }
        return emptyList()
    }

    override fun finish() {
        val models = typeProcessor.process()
        models.forEach { model ->
            FileSpec.builder(model.className)
                .indent(settings.indent)
                .addType(QueryModelRenderer.renderInterface(model))
                .addType(QueryModelRenderer.renderClass(model))
                .build()
                .writeTo(
                    codeGenerator = codeGenerator,
                    aggregating = false,
                    originatingKSFiles = listOf(model.originatingFile)
                )
        }
    }

    private fun isIncluded(declaration: KSClassDeclaration): Boolean {
        val className = declaration.qualifiedName!!.asString()
        if (settings.excludedPackages.any { className.startsWith(it) }) {
            return false
        } else if (settings.excludedClasses.any { it == className }) {
            return false
        } else if (settings.includedClasses.isNotEmpty()) {
            return settings.includedClasses.any { it == className }
        } else if (settings.includedPackages.isNotEmpty()) {
            return settings.includedPackages.any { className.startsWith(it) }
        } else {
            return true
        }
    }
}

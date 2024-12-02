package iceblizz6.querydsl.ksp

import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName

class QueryModel(
    val originalClassName: ClassName,
    val typeParameterCount: Int,
    val className: ClassName,
    val interfaceName: ClassName,
    val type: QueryModelType,
    val originatingFile: KSFile
) {
    var superclass: QueryModel? = null
    val properties = mutableListOf<QProperty>()
    val constraint
        get() =
            if (typeParameterCount > 0)
                originalClassName.parameterizedBy(List(typeParameterCount) { STAR })
            else
                originalClassName
    val parametrizedTypeName
        get() = Class::class.asClassName().parameterizedBy(WildcardTypeName.producerOf(constraint))
}

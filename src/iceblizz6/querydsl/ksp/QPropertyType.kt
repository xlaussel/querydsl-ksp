package iceblizz6.querydsl.ksp

import com.querydsl.core.types.dsl.EnumPath
import com.querydsl.core.types.dsl.ListPath
import com.querydsl.core.types.dsl.MapPath
import com.querydsl.core.types.dsl.SetPath
import com.querydsl.core.types.dsl.SimplePath
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

sealed interface QPropertyType {
    val pathTypeName: TypeName
    val pathClassName: ClassName
    val originalClassName: ClassName
    val originalTypeName: TypeName

    fun render(name:String) : PropertySpec

    class ListCollection(
        val innerType: QPropertyType
    ) : QPropertyType {
        override val originalClassName: ClassName
            get() = List::class.asClassName()

        override val originalTypeName: TypeName
            get() = List::class.asTypeName().parameterizedBy(innerType.originalTypeName)

        override val pathClassName: ClassName
            get() = ListPath::class.asClassName()

        override val pathTypeName: TypeName
            get() = ListPath::class.asClassName().parameterizedBy(innerType.originalTypeName, innerType.pathTypeName)

        override fun render(name: String): PropertySpec = PropertySpec
            .builder(name, ListPath::class.asClassName().parameterizedBy(innerType.originalTypeName, innerType.pathTypeName))
            .initializer("createList(\"$name\", ${innerType.originalClassName}::class.java, ${innerType.pathClassName}::class.java, null)")
            .build()

    }

    class SetCollection(
        val innerType: QPropertyType
    ) : QPropertyType {
        override val originalClassName: ClassName
            get() = Set::class.asClassName()

        override val originalTypeName: TypeName
            get() = Set::class.asTypeName().parameterizedBy(innerType.originalTypeName)

        override val pathClassName: ClassName
            get() = SetPath::class.asClassName()

        override val pathTypeName: TypeName
            get() = SetPath::class.asClassName().parameterizedBy(innerType.originalTypeName, innerType.pathTypeName)

        override fun render(name: String): PropertySpec = PropertySpec
            .builder(name, SetPath::class.asClassName().parameterizedBy(innerType.originalTypeName, innerType.pathTypeName))
            .initializer("createSet(\"$name\", ${innerType.originalClassName}::class.java, ${innerType.pathClassName}::class.java, null)")
            .build()
    }

    class MapCollection(
        val keyType: QPropertyType,
        val valueType: QPropertyType
    ) : QPropertyType {
        override val originalClassName: ClassName
            get() = Map::class.asClassName()

        override val originalTypeName: TypeName
            get() = Map::class.asTypeName().parameterizedBy(keyType.originalTypeName, valueType.originalTypeName)

        override val pathClassName: ClassName
            get() = MapPath::class.asClassName()

        override val pathTypeName: TypeName
            get() = MapPath::class.asTypeName().parameterizedBy(keyType.originalTypeName, valueType.originalTypeName, valueType.pathTypeName)

        override fun render(name: String): PropertySpec = PropertySpec
            .builder(name, MapPath::class.asClassName().parameterizedBy(keyType.originalTypeName, valueType.originalTypeName, valueType.pathTypeName))
            .initializer("createMap(\"$name\", ${keyType.originalClassName}::class.java, ${valueType.originalClassName}::class.java, ${valueType.pathClassName}::class.java)")
            .build()
    }

    class Simple(
        val type: SimpleType
    ) : QPropertyType {
        override val originalClassName: ClassName
            get() = type.className

        override val originalTypeName: TypeName
            get() = type.className

        override val pathClassName: ClassName
            get() = type.pathClassName

        override val pathTypeName: TypeName
            get() = type.pathTypeName

        override fun render(name: String): PropertySpec = type.render(name)

    }

    class Unknown(
        private val innerClassName: ClassName,
        private val innerTypeName: TypeName
    ) : QPropertyType {
        override val originalClassName: ClassName
            get() = innerClassName

        override val originalTypeName: TypeName
            get() = innerTypeName

        override val pathClassName: ClassName
            get() = SimplePath::class.asClassName()

        override val pathTypeName: TypeName
            get() = SimplePath::class.asTypeName().parameterizedBy(innerTypeName)

        override fun render(name: String): PropertySpec= PropertySpec
            .builder(name, SimplePath::class.asClassName().parameterizedBy(originalTypeName))
            .initializer("createSimple(\"$name\", ${originalClassName}::class.java)")
            .build()

    }

    class EnumReference(
        val enumClassName: ClassName
    ) : QPropertyType {
        override val originalClassName: ClassName
            get() = enumClassName

        override val originalTypeName: TypeName
            get() = enumClassName

        override val pathClassName: ClassName
            get() = EnumPath::class.asClassName()

        override val pathTypeName: TypeName
            get() = EnumPath::class.asTypeName().parameterizedBy(enumClassName)

        override fun render(name: String): PropertySpec = PropertySpec
                .builder(name, EnumPath::class.asClassName().parameterizedBy(enumClassName))
                .initializer("createEnum(\"${name}\", ${enumClassName}::class.java)")
                .build()
    }

    class ObjectReference(
        val entityClassName: ClassName,
        val queryClassName: ClassName,
        val typeArgs: List<TypeName>
    ) : QPropertyType {
        override val originalClassName: ClassName
            get() = entityClassName

        override val originalTypeName: TypeName
            get() {
                if (typeArgs.isEmpty()) {
                    return entityClassName
                } else {
                    return entityClassName.parameterizedBy(typeArgs)
                }
            }

        override val pathClassName: ClassName
            get() = queryClassName

        override val pathTypeName: TypeName
            get() = queryClassName

        override fun render(name: String): PropertySpec =PropertySpec
            .builder(name, queryClassName)
            .delegate(
                CodeBlock.builder()
                    .beginControlFlow("lazy")
                    .addStatement("${queryClassName}(forProperty(\"${name}\"))")
                    .endControlFlow()
                    .build()
            )
            .build()
    }
}

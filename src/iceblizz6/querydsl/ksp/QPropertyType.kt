package iceblizz6.querydsl.ksp

import com.querydsl.core.types.dsl.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

sealed class QPropertyType {
    abstract val pathTypeName: TypeName
    abstract val pathClassName: ClassName
    abstract val originalClassName: ClassName
    abstract val originalTypeName: TypeName

    fun renderAbstract(name: String) : PropertySpec =
        PropertySpec
            .builder(name,pathTypeName)
            .build()

    abstract fun PropertySpec.Builder.define(name:String) : PropertySpec.Builder

    fun render(name:String) : PropertySpec =
        PropertySpec.builder(name,pathTypeName).define(name).addModifiers(KModifier.OVERRIDE).build()

    class ListCollection(
        val innerType: QPropertyType
    ) : QPropertyType() {
        override val originalClassName: ClassName
            get() = List::class.asClassName()

        override val originalTypeName: TypeName
            get() = List::class.asTypeName().parameterizedBy(innerType.originalTypeName)

        override val pathClassName: ClassName
            get() = ListPath::class.asClassName()

        override val pathTypeName: TypeName
            get() = ListPath::class.asClassName().parameterizedBy(innerType.originalTypeName, innerType.pathTypeName)

        override fun PropertySpec.Builder.define(name:String) =
            initializer("createList(\"$name\", ${innerType.originalClassName}::class.java, ${innerType.pathClassName}::class.java, null)")
    }

    class SetCollection(
        val innerType: QPropertyType
    ) : QPropertyType() {
        override val originalClassName: ClassName
            get() = Set::class.asClassName()

        override val originalTypeName: TypeName
            get() = Set::class.asTypeName().parameterizedBy(innerType.originalTypeName)

        override val pathClassName: ClassName
            get() = SetPath::class.asClassName()

        override val pathTypeName: TypeName
            get() = SetPath::class.asClassName().parameterizedBy(innerType.originalTypeName, innerType.pathTypeName)

        override fun PropertySpec.Builder.define(name:String) =
            initializer("createSet(\"$name\", ${innerType.originalClassName}::class.java, ${innerType.pathClassName}::class.java, null)")
    }

    class MapCollection(
        val keyType: QPropertyType,
        val valueType: QPropertyType
    ) : QPropertyType() {
        override val originalClassName: ClassName
            get() = Map::class.asClassName()

        override val originalTypeName: TypeName
            get() = Map::class.asTypeName().parameterizedBy(keyType.originalTypeName, valueType.originalTypeName)

        override val pathClassName: ClassName
            get() = MapPath::class.asClassName()

        override val pathTypeName: TypeName
            get() = MapPath::class.asTypeName().parameterizedBy(keyType.originalTypeName, valueType.originalTypeName, valueType.pathTypeName)

        override fun PropertySpec.Builder.define(name:String) =
            initializer("createMap(\"$name\", ${keyType.originalClassName}::class.java, ${valueType.originalClassName}::class.java, ${valueType.pathClassName}::class.java)")
    }

    class Unknown(
        private val innerClassName: ClassName,
        private val innerTypeName: TypeName
    ) : QPropertyType() {
        override val originalClassName: ClassName
            get() = innerClassName

        override val originalTypeName: TypeName
            get() = innerTypeName

        override val pathClassName: ClassName
            get() = SimplePath::class.asClassName()

        override val pathTypeName: TypeName
            get() = SimplePath::class.asTypeName().parameterizedBy(innerTypeName)

        override fun PropertySpec.Builder.define(name:String) =
            initializer("createSimple(\"$name\", ${originalClassName}::class.java)")

    }

    class EnumReference(
        val enumClassName: ClassName
    ) : QPropertyType() {
        override val originalClassName: ClassName
            get() = enumClassName

        override val originalTypeName: TypeName
            get() = enumClassName

        override val pathClassName: ClassName
            get() = EnumPath::class.asClassName()

        override val pathTypeName: TypeName
            get() = EnumPath::class.asTypeName().parameterizedBy(enumClassName)

        override fun PropertySpec.Builder.define(name:String) =
            initializer("createEnum(\"${name}\", ${enumClassName}::class.java)")
    }

    class ObjectReference(
        val entityClassName: ClassName,
        val queryClassName: ClassName,
        val typeArgs: List<TypeName>
    ) : QPropertyType() {
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

        override fun PropertySpec.Builder.define(name:String) =
            delegate(
                CodeBlock.builder()
                    .beginControlFlow("lazy")
                    .addStatement("${queryClassName}(forProperty(\"${name}\"))")
                    .endControlFlow()
                    .build()
            )
    }
}

package iceblizz6.querydsl.ksp

import com.querydsl.core.types.Path
import com.querydsl.core.types.PathMetadata
import com.querydsl.core.types.dsl.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import iceblizz6.querydsl.ksp.Naming.toCamelCase

object QueryModelRenderer {
    fun render(model: QueryModel): TypeSpec {
        return TypeSpec.classBuilder(model.className)
            .setEntitySuperclass(model)
            .addSuperProperty(model)
            .addProperties(model)
            .constructorForPath(model)
            .constructorForMetadata(model)
            .constructorForVariable(model)
            .constructorForTypeMetadata(model)
            .addInitializerCompanionObject(model)
            .addInheritedProperties(model)
            .build()
    }

    private fun TypeSpec.Builder.setEntitySuperclass(model: QueryModel): TypeSpec.Builder {
        val constraint: TypeName = if (model.typeParameterCount > 0) {
            val typeParams = (0..<model.typeParameterCount).map { STAR }
            model.originalClassName.parameterizedBy(typeParams)
        } else {
            model.originalClassName
        }
        superclass(
            when (model.type) {
                QueryModelType.ENTITY, QueryModelType.SUPERCLASS -> EntityPathBase::class.asClassName().parameterizedBy(constraint)
                QueryModelType.EMBEDDABLE -> BeanPath::class.asClassName().parameterizedBy(constraint)
            }
        )
        return this
    }

    private fun TypeSpec.Builder.addSuperProperty(model: QueryModel): TypeSpec.Builder {
        model.superclass?.let { superclass ->
            val superProperty = PropertySpec
                .builder("_super", superclass.className)
                .delegate(
                    CodeBlock.builder()
                        .beginControlFlow("lazy")
                        .addStatement("${superclass.className}(this)")
                        .endControlFlow()
                        .build()
                )
                .build()
            addProperty(superProperty)
        }
        return this
    }

    private fun TypeSpec.Builder.addProperties(model: QueryModel): TypeSpec.Builder {
        model.properties
            .map { renderProperty(it) }
            .forEach { addProperty(it) }
        return this
    }

    private fun TypeSpec.Builder.addInheritedProperties(model: QueryModel): TypeSpec.Builder {
        model.superclass?.let { superclass ->
            superclass.properties
                .map { renderInheritedProperty(it) }
                .forEach { addProperty(it) }
            addInheritedProperties(superclass)
        }
        return this
    }

    private fun renderInheritedProperty(property: QProperty): PropertySpec {
        return PropertySpec.builder(property.name, property.type.pathTypeName)
            .getter(
                FunSpec.getterBuilder()
                    .addCode("return _super.${property.name}")
                    .build()
            )
            .build()
    }

    private fun renderProperty(property: QProperty): PropertySpec {
        val name = property.name
        val type = property.type
        return when (type) {
            is QPropertyType.Simple -> property.type.type.render(name)

        }
    }


    private fun TypeSpec.Builder.constructorForPath(model: QueryModel): TypeSpec.Builder {
        if (model.typeParameterCount > 0) {
            val typeParams = (0..<model.typeParameterCount).map { STAR }
            val source = model.originalClassName.parameterizedBy(typeParams)
            val spec = FunSpec.constructorBuilder()
                .addParameter("path", Path::class.asClassName().parameterizedBy(WildcardTypeName.producerOf(source)))
                .callSuperConstructor("path.type, path.metadata")
                .build()
            addFunction(spec)
        } else {
            val source = model.originalClassName
            val spec = FunSpec.constructorBuilder()
                .addParameter("path", Path::class.asClassName().parameterizedBy(WildcardTypeName.producerOf(source)))
                .callSuperConstructor("path.type, path.metadata")
                .build()
            addFunction(spec)
        }
        return this
    }

    private fun TypeSpec.Builder.constructorForMetadata(model: QueryModel): TypeSpec.Builder {
        val source = model.originalClassName
        val spec = FunSpec.constructorBuilder()
            .addParameter("metadata", PathMetadata::class)
            .callSuperConstructor("$source::class.java, metadata")
            .build()
        addFunction(spec)
        return this
    }

    private fun TypeSpec.Builder.constructorForVariable(model: QueryModel): TypeSpec.Builder {
        val spec = FunSpec.constructorBuilder()
            .addParameter("variable", String::class)
            .callSuperConstructor(
                "${model.originalClassName}::class.java",
                "${com.querydsl.core.types.PathMetadataFactory::class.qualifiedName!!}.forVariable(variable)"
            )
            .build()
        addFunction(spec)
        return this
    }

    private fun TypeSpec.Builder.constructorForTypeMetadata(model: QueryModel): TypeSpec.Builder {
        if (model.typeParameterCount > 0) {
            val typeParams = (0..<model.typeParameterCount).map { STAR }
            val source = model.originalClassName.parameterizedBy(typeParams)
            val spec = FunSpec.constructorBuilder()
                .addParameter("type", Class::class.asClassName().parameterizedBy(WildcardTypeName.producerOf(source)))
                .addParameter("metadata", PathMetadata::class)
                .callSuperConstructor("type, metadata")
                .build()
            addFunction(spec)
        } else {
            val source = model.originalClassName
            val spec = FunSpec.constructorBuilder()
                .addParameter("type", Class::class.asClassName().parameterizedBy(WildcardTypeName.producerOf(source)))
                .addParameter("metadata", PathMetadata::class)
                .callSuperConstructor("type, metadata")
                .build()
            addFunction(spec)
        }
        return this
    }

    private fun TypeSpec.Builder.addInitializerCompanionObject(model: QueryModel): TypeSpec.Builder {
        val source = model.originalClassName
        val qSource = model.className
        val name = source.simpleName.toCamelCase()
        val companionObject = TypeSpec.companionObjectBuilder()
            .addProperty(
                PropertySpec.builder(name, qSource)
                    .initializer("$qSource(\"${name}\")")
                    .addAnnotation(JvmField::class)
                    .build()
            )
            .build()
        addType(companionObject)
        return this
    }
}

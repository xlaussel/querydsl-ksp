package iceblizz6.querydsl.ksp

import com.querydsl.core.types.Path
import com.querydsl.core.types.PathMetadata
import com.querydsl.core.types.dsl.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import iceblizz6.querydsl.ksp.Naming.toCamelCase

object QueryModelRenderer {

    fun renderClass(model: QueryModel): TypeSpec {
        return TypeSpec.classBuilder(model.className)
            .setEntitySuperclass(model)
            .addSuperProperty(model)
            .primaryConstructor(model)
            .addEntitySuperInterfaces(model)
            .addProperties(model)
            .constructorForTypeMetadata(model)
            .constructorForPath(model)
            .constructorForMetadata(model)
            .constructorForVariable(model)
            .addInitializerCompanionObject(model)
            .build()
    }

    fun renderInterface(model: QueryModel): TypeSpec {
        return TypeSpec
            .classBuilder(model.interfaceName)
            .apply {
                model.superclass?.run {  addSuperinterface(interfaceName) }
                model.properties.forEach(QProperty::renderAbstract)
            }
            .build()
    }

    private fun TypeSpec.Builder.setEntitySuperclass(model: QueryModel): TypeSpec.Builder = run {
        val constraint: TypeName = if (model.typeParameterCount > 0) {
            val typeParams = List(model.typeParameterCount) { STAR }
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
        addSuperclassConstructorParameter("type,metadata")
        return this
    }

    private fun TypeSpec.Builder.addEntitySuperInterfaces(model: QueryModel) : TypeSpec.Builder = apply {
        addSuperinterface(model.interfaceName)
        model.superclass?.run {
            addSuperinterface(interfaceName,"_super")
        }
    }

    private fun TypeSpec.Builder.addSuperProperty(model: QueryModel): TypeSpec.Builder = apply {
        model.superclass?.let { superclass ->
            val superProperty = PropertySpec
                .builder("_super", ClassName(superclass.className.packageName,superclass.className.simpleName))
                .initializer("_super")
                .build()
            addProperty(superProperty)
        }
    }


    private fun TypeSpec.Builder.addProperties(model: QueryModel): TypeSpec.Builder = apply {
        model.properties.forEach(QProperty::render)
    }

    private fun TypeSpec.Builder.primaryConstructor(model: QueryModel): TypeSpec.Builder = apply {
        val source = model.originalClassName.run {
            if (model.typeParameterCount > 0) parameterizedBy(List(model.typeParameterCount) { STAR })
            else this
        }
        val spec = FunSpec.constructorBuilder()
            .apply {if (model.superclass!=null) addModifiers(KModifier.PRIVATE) }
            .addParameter("type", Class::class.asClassName().parameterizedBy(WildcardTypeName.producerOf(source)))
            .addParameter("metadata", PathMetadata::class)
            .apply {
                model.superclass?.run {
                    addParameter(
                        ParameterSpec
                            .builder("_super", className)
                            .defaultValue("${className}(type,metadata)").build()
                    )
                }
            }
            .build()
        primaryConstructor(spec)
    }

    private fun TypeSpec.Builder.constructorForPath(model: QueryModel): TypeSpec.Builder = apply {
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
    }

    private fun TypeSpec.Builder.constructorForMetadata(model: QueryModel): TypeSpec.Builder = run {
        val source = model.originalClassName
        val spec = FunSpec.constructorBuilder()
            .addParameter("metadata", PathMetadata::class)
            .callSuperConstructor("$source::class.java, metadata")
            .build()
        addFunction(spec)
    }

    private fun TypeSpec.Builder.constructorForVariable(model: QueryModel): TypeSpec.Builder = run {
        val spec = FunSpec.constructorBuilder()
            .addParameter("variable", String::class)
            .callSuperConstructor(
                "${model.originalClassName}::class.java",
                "${com.querydsl.core.types.PathMetadataFactory::class.qualifiedName!!}.forVariable(variable)"
            )
            .build()
        addFunction(spec)
    }


    private fun TypeSpec.Builder.constructorForTypeMetadata(model: QueryModel): TypeSpec.Builder= run {
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
    }

    private fun TypeSpec.Builder.addInitializerCompanionObject(model: QueryModel): TypeSpec.Builder = run {
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
    }
}

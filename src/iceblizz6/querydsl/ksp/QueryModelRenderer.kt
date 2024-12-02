package iceblizz6.querydsl.ksp

import com.querydsl.core.types.Path
import com.querydsl.core.types.PathMetadata
import com.querydsl.core.types.dsl.BeanPath
import com.querydsl.core.types.dsl.EntityPathBase
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import iceblizz6.querydsl.ksp.Naming.toCamelCase

object QueryModelRenderer {

    fun renderClass(model: QueryModel): TypeSpec =
        TypeSpec.classBuilder(model.className).apply {
            setEntitySuperclass(model)
            addSuperProperty(model)
            addPrimaryConstructor(model)
            addEntitySuperInterfaces(model)
            addProperties(model)
            constructorForTypeMetadata(model)
            constructorForPath(model)
            constructorForMetadata(model)
            constructorForVariable(model)
            addInitializerCompanionObject(model)
        }.build()

    fun renderInterface(model: QueryModel): TypeSpec {
        return TypeSpec
            .interfaceBuilder(model.interfaceName)
            .apply {
                model.superclass?.run { addSuperinterface(interfaceName) }
                model.properties.forEach { addProperty(it.renderAbstract()) }
            }
            .build()
    }

    private val QueryModel.constraint
        get() =
            if (typeParameterCount > 0)
                originalClassName.parameterizedBy(List(typeParameterCount) { STAR })
            else
                originalClassName


    private fun TypeSpec.Builder.setEntitySuperclass(model: QueryModel)  {
        superclass(
            when (model.type) {
                QueryModelType.ENTITY, QueryModelType.SUPERCLASS -> EntityPathBase::class.asClassName().parameterizedBy(model.constraint)
                QueryModelType.EMBEDDABLE -> BeanPath::class.asClassName().parameterizedBy(model.constraint)
            }
        )
        addSuperclassConstructorParameter("type,metadata")
    }

    private fun TypeSpec.Builder.addEntitySuperInterfaces(model: QueryModel)  {
        addSuperinterface(model.interfaceName)
        model.superclass?.run {
            addSuperinterface(interfaceName,"_super")
        }
    }

    private fun TypeSpec.Builder.addSuperProperty(model: QueryModel) =
        model.superclass?.let { superclass ->
            val superProperty = PropertySpec
                .builder("_super", ClassName(superclass.className.packageName,superclass.className.simpleName))
                .initializer("_super")
                .build()
            addProperty(superProperty)
        }


    private fun TypeSpec.Builder.addProperties(model: QueryModel) =
        model.properties.forEach {addProperty(it.render())}

    private fun TypeSpec.Builder.addPrimaryConstructor(model: QueryModel) {
        val spec = FunSpec.constructorBuilder()
            .apply {if (model.superclass!=null) addModifiers(KModifier.PRIVATE) }
            .addParameter("type", Class::class.asClassName().parameterizedBy(WildcardTypeName.producerOf(model.constraint)))
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

    private fun TypeSpec.Builder.constructorForPath(model: QueryModel) =
            addFunction(FunSpec.constructorBuilder()
                .addParameter("path", Path::class.asClassName().parameterizedBy(WildcardTypeName.producerOf(model.constraint)))
                .callThisConstructor("path.type, path.metadata")
                .build())

    private fun TypeSpec.Builder.constructorForMetadata(model: QueryModel) {
        val source = model.originalClassName
        val spec = FunSpec.constructorBuilder()
            .addParameter("metadata", PathMetadata::class)
            .callThisConstructor("$source::class.java, metadata")
            .build()
        addFunction(spec)
    }

    private fun TypeSpec.Builder.constructorForVariable(model: QueryModel) {
        val spec = FunSpec.constructorBuilder()
            .addParameter("variable", String::class)
            .callThisConstructor(
                "${model.originalClassName}::class.java",
                "${com.querydsl.core.types.PathMetadataFactory::class.qualifiedName!!}.forVariable(variable)"
            )
            .build()
        addFunction(spec)
    }


    private fun TypeSpec.Builder.constructorForTypeMetadata(model: QueryModel) {
        if (model.superclass==null) return
        val spec = FunSpec.constructorBuilder()
            .addParameter("type", Class::class.asClassName().parameterizedBy(WildcardTypeName.producerOf(model.constraint)))
            .addParameter("metadata", PathMetadata::class)
            .callThisConstructor(
                if (model.superclass==null) "type, metadata"
                else "type, metadata, ${model.superclass!!.className}(type,metadata)")
            .build()
        addFunction(spec)
    }

    private fun TypeSpec.Builder.addInitializerCompanionObject(model: QueryModel) {
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

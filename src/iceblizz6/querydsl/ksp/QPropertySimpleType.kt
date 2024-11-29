package iceblizz6.querydsl.ksp

import com.querydsl.core.types.dsl.ArrayPath
import com.querydsl.core.types.dsl.BooleanPath
import com.querydsl.core.types.dsl.ComparablePath
import com.querydsl.core.types.dsl.DatePath
import com.querydsl.core.types.dsl.DateTimePath
import com.querydsl.core.types.dsl.NumberPath
import com.querydsl.core.types.dsl.SimplePath
import com.querydsl.core.types.dsl.StringPath
import com.querydsl.core.types.dsl.TimePath
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.math.BigDecimal
import java.math.BigInteger
import java.sql.Blob
import java.sql.Clob
import java.sql.NClob
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.Year
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale
import java.util.Calendar
import java.util.Currency
import java.util.TimeZone
import kotlin.reflect.KClass

sealed class QPropertySimpleType :  QPropertyType() {
    override val originalTypeName: TypeName
        get() = originalClassName


    class Array(
        private val collectionType: ClassName,
        private val singleType: ClassName
    ) : QPropertySimpleType() {
        override val originalClassName = collectionType
        override val pathClassName = ArrayPath::class.asClassName()
        override val pathTypeName = ArrayPath::class.asClassName().parameterizedBy(collectionType, singleType)

        override fun PropertySpec.Builder.define(name:String) =
                initializer("createArray(\"$name\", ${collectionType}::class.java)")
    }

    class Simple(override val originalClassName: ClassName) : QPropertySimpleType() {
        override val pathClassName = SimplePath::class.asClassName()
        override val pathTypeName = SimplePath::class.asClassName().parameterizedBy(originalClassName)
        override fun PropertySpec.Builder.define(name:String) =
                initializer("createSimple(\"$name\", ${originalClassName}::class.java)")
    }

    class Comparable(override val originalClassName: ClassName) : QPropertySimpleType() {
        override val pathClassName = ComparablePath::class.asClassName()
        override val pathTypeName = ComparablePath::class.asClassName().parameterizedBy(originalClassName)

        override fun PropertySpec.Builder.define(name:String) =
            initializer("createComparable(\"$name\", ${originalClassName}::class.java)")
    }

    class QNumber(override val originalClassName: ClassName) : QPropertySimpleType() {
        override val pathClassName = NumberPath::class.asClassName()
        override val pathTypeName = NumberPath::class.asClassName().parameterizedBy(originalClassName)

        override fun PropertySpec.Builder.define(name:String) =
            initializer("createNumber(\"$name\", ${originalClassName}::class.javaObjectType)")
    }

    class Date(override val originalClassName: ClassName) : QPropertySimpleType() {
        override val pathClassName = DatePath::class.asClassName()
        override val pathTypeName = DatePath::class.asClassName().parameterizedBy(originalClassName)

        override fun PropertySpec.Builder.define(name:String) =
            initializer("createDate(\"$name\", ${originalClassName}::class.java)")

    }

    class DateTime(override val originalClassName: ClassName) : QPropertySimpleType() {
        override val pathClassName = DateTimePath::class.asClassName()
        override val pathTypeName = DateTimePath::class.asClassName().parameterizedBy(originalClassName)

        override fun PropertySpec.Builder.define(name:String) =
            initializer("createDateTime(\"$name\", ${originalClassName}::class.java)")
    }

    class Time(override val originalClassName: ClassName) : QPropertySimpleType() {
        override val pathClassName = TimePath::class.asClassName()
        override val pathTypeName = TimePath::class.asClassName().parameterizedBy(originalClassName)

        override fun PropertySpec.Builder.define(name:String) =
            initializer("createTime(\"$name\", ${originalClassName}::class.java)")
    }

    class QString : QPropertySimpleType() {
        override val originalClassName = String::class.asClassName()
        override val pathClassName = StringPath::class.asClassName()
        override val pathTypeName = StringPath::class.asTypeName()

        override fun PropertySpec.Builder.define(name:String) =
            initializer("createString(\"$name\")")
    }

    class QBoolean : QPropertySimpleType() {
        override val originalClassName = Boolean::class.asClassName()
        override val pathClassName = BooleanPath::class.asClassName()
        override val pathTypeName = BooleanPath::class.asTypeName()

        override fun PropertySpec.Builder.define(name:String) =
            initializer("createBoolean(\"$name\")")
    }

    object Mapper {
        private val typeMap: Map<ClassName, QPropertySimpleType> = mutableMapOf<KClass<*>, QPropertySimpleType>()
            .apply {
                this[Any::class] = Simple(Any::class.asClassName())
                this[Char::class] = Comparable(Char::class.asClassName())
                this[String::class] = QString()
                this[Boolean::class] = QBoolean()

                this[Byte::class] = QNumber(Byte::class.asClassName())
                this[UByte::class] = QNumber(Byte::class.asClassName())
                this[Short::class] = QNumber(Short::class.asClassName())
                this[UShort::class] = QNumber(Short::class.asClassName())
                this[Int::class] = QNumber(Int::class.asClassName())
                this[UInt::class] = QNumber(Int::class.asClassName())
                this[Long::class] = QNumber(Long::class.asClassName())
                this[ULong::class] = QNumber(Long::class.asClassName())
                this[Float::class] = QNumber(Float::class.asClassName())
                this[Double::class] = QNumber(Double::class.asClassName())

                this[BigInteger::class] = QNumber(BigInteger::class.asClassName())
                this[BigDecimal::class] = QNumber(BigDecimal::class.asClassName())
                this[java.util.UUID::class] = Comparable(java.util.UUID::class.asClassName())

                this[LocalDate::class] = Date(LocalDate::class.asClassName())
                this[ZonedDateTime::class] = DateTime(ZonedDateTime::class.asClassName())
                this[LocalDateTime::class] = DateTime(LocalDateTime::class.asClassName())
                this[LocalTime::class] = Time(LocalTime::class.asClassName())
                this[Locale::class] = Simple(Locale::class.asClassName())
                this[ByteArray::class] = Array(ByteArray::class.asClassName(), Byte::class.asClassName())
                this[CharArray::class] = Array(CharArray::class.asClassName(), Char::class.asClassName())
                this[ZoneId::class] = Simple(ZoneId::class.asClassName())
                this[ZoneOffset::class] = Comparable(ZoneOffset::class.asClassName())
                this[Year::class] = Comparable(Year::class.asClassName())
                this[OffsetDateTime::class] = DateTime(OffsetDateTime::class.asClassName())
                this[Instant::class] = DateTime(Instant::class.asClassName())
                this[java.util.Date::class] = DateTime(java.util.Date::class.asClassName())
                this[Calendar::class] = DateTime(Calendar::class.asClassName())
                this[java.sql.Date::class] = Date(java.sql.Date::class.asClassName())
                this[java.sql.Time::class] = Time(java.sql.Time::class.asClassName())
                this[java.sql.Timestamp::class] = DateTime(java.sql.Timestamp::class.asClassName())
                this[Duration::class] = Comparable(Duration::class.asClassName())
                this[Blob::class] = Simple(Blob::class.asClassName())
                this[Clob::class] = Simple(Clob::class.asClassName())
                this[NClob::class] = Simple(NClob::class.asClassName())
                this[Currency::class] = Simple(Currency::class.asClassName())
                this[TimeZone::class] = Simple(TimeZone::class.asClassName())
                this[java.net.URL::class] = Simple(java.net.URL::class.asClassName())
            }
            .mapKeys { it.key.asClassName() }

        fun get(className: ClassName): QPropertySimpleType? {
            return typeMap[className]
        }
    }
}

package iceblizz6.querydsl.ksp

class QProperty(
    val name: String,
    val type: QPropertyType
) {
    fun render() = type.render(name)
    fun renderAbstract() = type.renderAbstract(name)
}

package iceblizz6.querydsl.ksp

class KspSettings(
    val enable: Boolean,
    val indent: String,
    val prefix: String,
    val suffix: String,
    val interfacePrefix: String,
    val interfaceSuffix: String,
    val packageSuffix: String,
    val excludedPackages: List<String>,
    val excludedClasses: List<String>,
    val includedPackages: List<String>,
    val includedClasses: List<String>,
) {
    constructor(args: KspArgParser) : this(
        enable = args.getBoolean("enable") ?: true,
        indent = args.getString("indent") ?: "    ",
        prefix = args.getString("prefix") ?: "Q",
        suffix = args.getString("suffix") ?: "",
        interfacePrefix = args.getString("interfacePrefix") ?: "IQ",
        interfaceSuffix = args.getString("interfaceSuffix") ?: "",
        packageSuffix = args.getString("packageSuffix") ?: "",
        excludedPackages = args.getCommaSeparatedList("excludedPackages"),
        excludedClasses = args.getCommaSeparatedList("excludedClasses"),
        includedPackages = args.getCommaSeparatedList("includedPackages"),
        includedClasses = args.getCommaSeparatedList("includedClasses")
    )
}

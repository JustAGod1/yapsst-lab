package org.stella.typecheck

enum class StellaExtension(val extensionName: String) {
    UNIT_TYPE("unit-type"),
    PAIRS("pairs"),
    TUPLES("tuples"),
    RECORDS("records"),
    LET_BINDINGS("let-bindings"),
    TYPE_ASCRIPTIONS("type-ascriptions"),
    SUM_TYPES("sum-types"),
    LISTS("lists"),
    VARIANTS("variants"),
    FIX_POINT_COMBINATOR("fix-point-combinator"),

    SEQUENCE("sequencing"),
    REFERENCES("references"),
    PANIC("panic"),
    EXCEPTIONS("exceptions"),
    EXCEPTIONS_TYPE_ANNOTATION("exception-type-annotation"),
    STRUCTURAL_SUBTYPING("structural-subtyping"),
    AMBIGUOUS_TYPE_AS_BOTTOM("ambiguous-type-as-bottom"),
}
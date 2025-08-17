package org.stella.typecheck

enum class StellaExtension(val extensionName: String) {
    UNIT_TYPE("unit-type"),
    NATURAL_LITERALS("natural-literals"),
    PAIRS("pairs"),
    TUPLES("tuples"),
    RECORDS("records"),
    LET_BINDINGS("let-bindings"),
    TYPE_ASCRIPTIONS("type-ascriptions"),
    SUM_TYPES("sum-types"),
    LISTS("lists"),
    VARIANTS("variants"),
    FIX_POINT_COMBINATOR("fix-point-combinator"),
    STRUCTURAL_PATTERNS("structural-patterns"),

    SEQUENCE("sequencing"),
    REFERENCES("references"),
    PANIC("panic"),
    EXCEPTIONS("exceptions"),
    EXCEPTIONS_TYPE_DECLARATION("exception-type-declaration"),
    STRUCTURAL_SUBTYPING("structural-subtyping"),
    AMBIGUOUS_TYPE_AS_BOTTOM("ambiguous-type-as-bottom"),

    TYPE_RECONSTRUCTION("type-reconstruction"),
    UNIVERSAL_TYPES("universal-types"),
    TOP_TYPE("top-type"),
    BOTTOM_TYPE("bottom-type"),
}
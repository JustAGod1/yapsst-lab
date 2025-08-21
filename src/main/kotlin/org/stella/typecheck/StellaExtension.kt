package org.stella.typecheck

enum class StellaExtension(vararg val extensionName: String) {
    UNIT_TYPE("unit-type"),
    NATURAL_LITERALS("natural-literals"),
    TUPLES("tuples", "pairs"),
    RECORDS("records"),
    LET_BINDINGS("let-bindings"),
    LET_PATTERNS("let-patterns"),
    LETREC_BINDINGS("letrec-bindings"),
    NESTED_FUNCTION_DECLARATIONS("nested-function-declarations"),
    NULLARY_VARIANT_LABELS("nullary-variant-labels"),
    TYPE_ASCRIPTIONS("type-ascriptions"),
    SUM_TYPES("sum-types"),
    LISTS("lists"),
    VARIANTS("variants"),
    FIX_POINT_COMBINATOR("fixpoint-combinator"),
    STRUCTURAL_PATTERNS("structural-patterns"),
    MULTIPARAMETER_FUNCTIONS("multiparameter-functions"),
    NULLARY_FUNCTIONS("nullary-functions"),


    SEQUENCE("sequencing"),
    REFERENCES("references"),
    PANIC("panic"),
    TYPE_CAST("type-cast"),
    TRY_CAST_AS("try-cast-as"),
    EXCEPTIONS("exceptions"),
    OPEN_VARIANT_EXCEPTIONS("open-variant-exceptions"),
    EXCEPTIONS_TYPE_DECLARATION("exception-type-declaration", "exception-type-annotation"),
    STRUCTURAL_SUBTYPING("structural-subtyping"),
    AMBIGUOUS_TYPE_AS_BOTTOM("ambiguous-type-as-bottom"),
    PATTERN_ASCRIPTIONS("pattern-ascriptions"),


    TYPE_RECONSTRUCTION("type-reconstruction"),
    UNIVERSAL_TYPES("universal-types"),
    TOP_TYPE("top-type"),
    BOTTOM_TYPE("bottom-type"),
}
package mpp

fun jsFun() {
}

fun experiments() {
    val string: String = ""
    val ref: kotlin.reflect.KCallable<*> = ::jsFun
    ref.name
    // should be unresolved
    ref.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: call" textAttributesKey="WRONG_REFERENCES_ATTRIBUTES">call</error>()
}
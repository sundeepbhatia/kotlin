package mpp

import kotlin.reflect.KCallable

fun some() {
    val string: String = ""
    val any: Any = ""
    val callableRef: KCallable<*> = ::commonFun
    callableRef.name
    // should be unresolved
    callableRef.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: call" textAttributesKey="WRONG_REFERENCES_ATTRIBUTES">call</error>()
}

fun commonFun() {
}

package mpp

import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KCallable
import java.lang.Cloneable

fun foo(x: KAnnotatedElement): Boolean = true

class Foo {
    fun bar(a: Int, b: Int): KCallable<*> { TODO() }
}

fun jvmFun() {
}

fun getKCallable(): KCallable<*> = ::jvmFun

fun main() {
    val ref = ::jvmFun
    val typedRef: KCallable<*> = getKCallable()
    ref.call()
    typedRef.call()
    foo(Foo::bar)
}


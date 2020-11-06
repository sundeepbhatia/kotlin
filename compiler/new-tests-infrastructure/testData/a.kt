// MODULE: common
// !LANGUAGE: -NewInference
// FILE: b.kt

fun foo(): String = Utils.create()

// FILE: Utils.java

public class Utils {
    public static String create() {
        return "hello";
    }
}

// MODULE: main
// DEPENDENCY: common SOURCE
// !API_VERSION: 1.3
// !IGNORE_DATA_FLOW_IN_ASSERT_DIRECTIVE
// !JVM_DEFAULT_MODE: all-compatibility
// FILE: a.kt

fun test() = foo().length

fun test_2() = Utils.create()

fun test_3(s: Any) {
    if (s is String) {
        <!DEBUG_INFO_SMARTCAST!>s<!>.length
    }
    return <!TYPE_MISMATCH("Unit; String")!>""<!>
}

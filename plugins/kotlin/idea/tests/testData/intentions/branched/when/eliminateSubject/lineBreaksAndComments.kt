// PRIORITY: LOW
class Klass<T>

fun test(obj: Any): String {
    return <caret>when (obj) {
        is String -> "string" // return "string"

        // it's an Int
        is Int -> "int"

        // otherwise
        else -> "unknown"
    }
}

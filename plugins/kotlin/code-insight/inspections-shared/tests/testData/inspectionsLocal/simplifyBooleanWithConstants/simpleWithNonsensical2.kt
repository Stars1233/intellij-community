// FIX: Simplify boolean expression
fun bar(): Boolean {
    return false
}

fun foo(y: Boolean) {
    <caret>true && (bar()) && y
}
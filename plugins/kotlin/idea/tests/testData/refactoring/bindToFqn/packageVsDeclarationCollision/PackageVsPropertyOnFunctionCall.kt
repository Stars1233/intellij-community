// BIND_TO foo.MyClass
// BIND_RESULT MyClass()
package foo

val foo: Int = 10

class MyClass

fun usage() {
    <caret>UNRESOLVED()
}
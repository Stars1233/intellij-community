open class A(x: Int) {
    fun m(x: Int, y: Boolean) = 1

    fun d(x: Int) {
        m(1, false,<caret>)
    }
}

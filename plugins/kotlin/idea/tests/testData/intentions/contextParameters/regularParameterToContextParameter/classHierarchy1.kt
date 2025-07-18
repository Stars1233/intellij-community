// COMPILER_ARGUMENTS: -Xcontext-parameters

interface IFace {
    fun foo(s: String)
}

open class OpenClass : IFace {
    override fun foo(s: String) {}
}

class FinalClass : OpenClass(), IFace {
    override fun foo(<caret>s: String) {}
}

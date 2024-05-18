package parser

class UnOp(val op: Char, val expr: Expression): Expression() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UnOp) return false
        return op == other.op && expr == other.expr
    }
}
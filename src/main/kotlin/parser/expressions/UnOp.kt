package parser.expressions

/** Represents a unary operation and named functions with 1 parameter */
class UnOp(val op: Char, val expr: Expression): Expression() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UnOp) return false
        return op == other.op && expr == other.expr
    }
    override fun hashCode(): Int {
        var result = op.hashCode()
        result = 31 * result + expr.hashCode()
        return result
    }
}
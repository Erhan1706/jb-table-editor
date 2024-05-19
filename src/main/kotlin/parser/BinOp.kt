package parser

class BinOp(val op: Char, val l: Expression, val r: Expression): Expression() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BinOp) return false
        return op == other.op && l == other.l && r == other.r
    }
    override fun hashCode(): Int {
        var result = op.hashCode()
        result = 31 * result + l.hashCode()
        result = 31 * result + r.hashCode()
        return result
    }
}
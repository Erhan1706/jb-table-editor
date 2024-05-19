package parser.Expressions

class NumberExpr(val num: Double): Expression() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NumberExpr) return false
        return num == other.num
    }
    override fun hashCode(): Int {
        return num.hashCode()
    }
}
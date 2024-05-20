# Table Editor
A table editor with integrated formula support. All implemented in Kotlin, using Swing as the UI framework

![](/assets/demo.gif)

## Installation & Usage
Clone the repository with: 
~~~ 
git clone https://github.com/Erhan1706/jb-table-editor.git
~~~ 
Navigate to the project directory
~~~
cd jb-table-editor
~~~
Run the project with:
~~~
./gradlew run
~~~
Or just open the project in IntelliJ IDEA and run `main.kt`
### Usage 
- To enter a formula, start typing in a cell and press enter when done
- Pressing 'c' in a selected cell will calculate the formula in that cell and all cells that the formula references
- Can navigate around the cells freely with the arrow keys
- Note that for decimal numbers a dot must be used as the decimal separator (e.g. `3.14` not `3,14`)
- Cells are resizable

## Features
The following syntactic constructs are supported:
- Parentheses: `()`
- Binary operators: `+`, `-`, `*`, `/`, `%`
- Unary operators: `+`, `-`
- Named functions: `pow`, `sqrt`, `fact`, `e`, `max`, `min`
- References to other cells: `A1`, `B2`, `AC3`, etc.

### How the parser algorithm works
1. Detect all the references to other cells in the formula and replace them with their values. If the reference contains 
a formula then this is recursively calculated. 
2. Strip the formula of all whitespaces.
3. Unary minus is replaced with a placeholder `~` to avoid ambiguity with the binary minus operator
4. Identify all named functions and reformat them into a format that can be evaluated by the tokenizer. For example,
   `pow(2+1, 3)` is converted to `(2+1)pow(3)`
5. Convert the formula from infix to prefix notation, 
using a variation of the [shunting yard algorithm](https://en.wikipedia.org/wiki/Shunting-yard_algorithm). 
The actual algorithm used can be found [here](https://www.javatpoint.com/convert-infix-to-prefix-notation).
6. Tokenize the formula in prefix notation, this will result in an array of tokens (e.g. `[+, *, 2, 4, 3]`)
7.  Construct the AST from the tokenized prefix expression using the following structure:
- `Expression` -> The base class for all expressions.
- `NumberExpr(value: Double)` - Represents numeric values.
- `BinOp(operator: Char, left: Expression, right: Expression)`- Represents binary operations and named functions with two arguments.
- `UnOp(operator: Char, operand: Expression)` - Represents unary operations and named functions with one argument.

8.  Traverse and evaluate the AST to compute the final result.

### How to add a new named function
Adding a new function is very simple. For this example, we will add a function called `add` that takes two arguments and
returns their sum.
1. Include the named function in the `isNamedFunction` set:
~~~kotlin
private fun isNamedFunction(s: String): Boolean {
   return setOf("pow", "sqrt", ..., "add").contains(s)
}
~~~
2. Specify the number of arguments the function takes in the `getNumberOfArguments` function:
~~~kotlin
private fun getNumberOfArguments(s: String): Int {
   return when (s) {
       "sqrt" -> 1
       ...
       "add" -> 2
   }
}
~~~
3. Add the priority of the function in the `operatorsPrio` map inside the `infixToPrefixTokenize` (higher number means higher priority):
~~~kotlin
val operatorsPrio: Map<String, Int> = mapOf("+" to 1, "*" to 2, 
   "pow" to 3, "sqrt" to 4, ..., "add" to 3)
~~~
4. Add the corresponding AST node in the `when` statement of the `generateAST` function. Choose a unique character to
represent the function:
~~~kotlin
 } else if (isNamedFunction(cur)) {
     return when (cur) {
         "pow" -> {
            val (left, nextIndex) = generateAST(prefix, index + 1)
            val (right, finalIndex) = generateAST(prefix, nextIndex)
            Pair(BinOp('^', left, right), finalIndex) 
         }
         ...
         "add" -> {
            val (left, nextIndex) = generateAST(prefix, index + 1)
            val (right, finalIndex) = generateAST(prefix, nextIndex)
            Pair(BinOp('a', left, right), finalIndex) 
         }
~~~
5. Define how the node should be evaluated in the `parseAST` function:
~~~kotlin
is BinOp -> when (expr.op) {
    '*' -> parseAST(expr.l) * parseAST(expr.r)
       ...
    'a' -> parseAST(expr.l) + parseAST(expr.r)
~~~

## Future Improvements
- Add a more robust error handling system, which gives more detailed error messages to the user
- Allow the user to dynamically add more rows and columns to the table
- Add support for more complex formulas, such as nested functions
- Allow the user to save and load tables from files
- Add support for named functions with more than two arguments
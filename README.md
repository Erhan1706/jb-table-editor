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
- Only integers are supported, results that are decimal number will be rounded due to this 
- Cells are resizable

## Features
The following syntactic constructs are supported:
- Parentheses: `()`
- Binary operators: `+`, `-`, `*`, `/`, `%`
- Unary operators: `+`, `-`
- Named functions: `pow`, `fact`,`max`, `min`
- References to other cells: `A1`, `B2`, `AC3`, etc.

### How the parser algorithm works
1. Detect all the references to other cells in the formula and replace them with their values. If the reference contains 
a formula then this is recursively calculated. 
2. Tokenize the formula, transforming the original string to an array of tokens (e.g. [NumberToken(1), OperatorToken('+'), NumberToken(3)])
3. Convert the formula from infix to reverse polish notation (postfix) notation, 
using the [shunting yard algorithm](https://en.wikipedia.org/wiki/Shunting-yard_algorithm). This will originate an output stack of the following format [1,3,+] 
4. Traverse the output stack and evaluate the RPN expression. 

### How to add a new named function
Adding a new function is very simple. For this example, we will add a function called `add` that takes two arguments and
returns their sum.
1. Locate the `evalFunction` method in the Parser class:
~~~kotlin
private fun evalFunction(numStack: ArrayDeque<NumberToken>, f: FunctionToken) {
   val r = numStack.removeLast()
   val l = numStack.removeLast()
   val res: NumberToken = when (f.func) {
      "pow"-> NumberToken(l.num.toDouble().pow(r.num.toDouble()).toInt())
      "max"-> NumberToken(max(l.num, r.num))
      "min"-> NumberToken(min(l.num, r.num))
      "fact" -> {
         numStack.add(l)
         NumberToken(factorial(r.num))
      }
      else -> throw IllegalArgumentException("Unknown function: ${f.func}")
   }
   numStack.add(res)
}
~~~
2. Implement the functionality of the named function. Note: make sure to reset the state according to the number of parameters the function needs (e.g. take a look at how `fact` is implemented for 1 parameter functions).
~~~kotlin
    "add" -> NumberToken(l.num + r.num)
~~~


## Future Improvements
- Add a more robust error handling system, which gives more detailed error messages to the user
- Allow the user to dynamically add more rows and columns to the table
- Allow the user to save and load tables from files
- Add support for named functions with a dynamic number of arguments
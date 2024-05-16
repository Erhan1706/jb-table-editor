import ui.MainFrame
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JTextArea
val name: String? = null

fun main(args: Array<String>) {
    println("hello $name")
    var arr = arrayOf(1,2,3)
    val things = mutableListOf("K", "C", "A")
    arr.forEach { thing ->
        print(thing)
    }


    val mainFrame = MainFrame()
}



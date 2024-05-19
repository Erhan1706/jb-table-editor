package ui

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*


class MainFrame: JFrame() {

    private val mainPanel = JPanel()
    private val initialPanel = JPanel()

    init {
        title = "Table Editor"
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()
        setLocationRelativeTo(null)

        mainPanel.layout = BorderLayout()
        initialPanel.layout = FlowLayout(FlowLayout.LEFT)

        renderInitialPanel()
        mainPanel.add(initialPanel, BorderLayout.CENTER)
        add(mainPanel)

        pack()
        isVisible = true
    }

    /** Renders the initial panel that allows the user to specify the dimensions of the matrix table */
    fun renderInitialPanel() {
        val rowsTextField = JTextField("15", 5)
        val colsTextField = JTextField("10", 5)
        val textFieldSize = Dimension(50, 25)
        rowsTextField.preferredSize = textFieldSize
        colsTextField.preferredSize = textFieldSize
        initialPanel.add(JLabel("Number of rows:"))
        initialPanel.add(rowsTextField)
        initialPanel.add(JLabel("Number of columns:"))
        initialPanel.add(colsTextField)

        val buttonPanel = JPanel(BorderLayout())
        val button = JButton("Create Table")
        buttonPanel.add(button, BorderLayout.CENTER)
        button.addActionListener {
            try {
                val rows = rowsTextField.text.toInt()
                val cols = colsTextField.text.toInt()
                renderTable(rows, cols)
                remove(mainPanel)
                revalidate()
                setSize(600, 400)
                repaint()
            } catch (e: NumberFormatException) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number")
            }
        }

        mainPanel.add(buttonPanel, BorderLayout.SOUTH)
    }


    /** Renders the table with the given number of rows and columns */
    fun renderTable(rows: Int, cols: Int) {
        val table = Table(rows, cols)
        add(table, BorderLayout.CENTER)
        add(JScrollPane(table))
        add(JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED))
        table.autoResizeMode = JTable.AUTO_RESIZE_OFF
        table.label.horizontalAlignment = JLabel.CENTER
        add(table.label, BorderLayout.SOUTH)
    }
}
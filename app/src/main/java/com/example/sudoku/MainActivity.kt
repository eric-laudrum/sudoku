package com.example.sudoku

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    // Properties
    private lateinit var gridLayout: GridLayout
    private var cellViews: Array<Array<TextView>>? = null
    private var puzzleBoard: Array<IntArray>? = null
    private var solutionBoard: Array<IntArray>? = null
    private var selectedCell: TextView? = null
    private var selectedRow = -1
    private var selectedCol = -1

    // On Create
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Get grid view
        gridLayout = findViewById(R.id.grid)

        // Status bar padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Use a ViewTreeObserver to wait for the layout to be measured
        gridLayout.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Verify board hasn't already been created
                if (gridLayout.width > 0 && gridLayout.height > 0 && cellViews == null) {

                    // Generate the puzzle and solution
                    val (puzzle, solution) = GameGenerator().generatePuzzle()
                    puzzleBoard = puzzle
                    solutionBoard = solution

                    // Build the UI based on data just created.
                    createBoard()

                    // Remove the listener so this only runs once
                    gridLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        })
    }

    // Functions
    private fun createBoard() {
        val boardSize = 9
        gridLayout.removeAllViews()

        val cells = Array(boardSize) { row ->
            Array(boardSize) { col ->
                val cell = TextView(this).apply {
                    layoutParams = GridLayout.LayoutParams(
                        GridLayout.spec(row, 1f),
                        GridLayout.spec(col, 1f)
                    ).apply {
                        width = 0
                        height = 0
                    }

                    gravity = Gravity.CENTER
                    textSize = 20f

                    // Player input
                    val isEditable = puzzleBoard!![row][col] ==0
                    if(isEditable){
                        setTextColor(Color.BLUE) // tmp - player entered numbers are blue
                        setOnClickListener {
                            selectCell(this, row, col)
                        }
                    } else{
                        setTextColor(Color.BLACK)
                    }

                    // Set text based on the number from the puzzle
                    val number = puzzleBoard?.get(row)?.get(col) ?: 0
                    text = if (number == 0) "" else number.toString()

                    updateCellBorder(this, row, col, false)
                }
                gridLayout.addView(cell)
                cell
            }
        }
        cellViews = cells
        setupNumberButtons()
    }

    // Handle cell selection
    private fun selectCell(cell: TextView, row: Int, col: Int){
        selectedCell?.let{
            updateCellBorder(it, selectedRow, selectedCol, false)
        }

        // Highlight cell
        updateCellBorder(cell, row, col, true)

        // Update state
        selectedCell = cell
        selectedRow = row
        selectedCol = col
    }

    private fun setupNumberButtons(){
        val numberButtonsLayout = findViewById<LinearLayout>(R.id.footer)
        val buttons = (1..9).map{ number ->
            Button(this).apply{
                text = number.toString()
                layoutParams = LinearLayout.LayoutParams(
                    0, // width
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f // weight
                )
                setOnClickListener {
                    onNumberButtonClick(number)
                }
            }
        }
        buttons.forEach { numberButtonsLayout.addView(it) }
    }

    private fun onNumberButtonClick(number: Int) {
        // Check if cell is selected and can be edited
        if (selectedCell == null || selectedRow == -1) return

        // Set the number in the TextView
        selectedCell?.text = number.toString()

        // Validate
        val isCorrect = solutionBoard!![selectedRow][selectedCol] == number
        if (isCorrect) {
            // Set text to blue for user input
            selectedCell?.setTextColor(Color.BLUE)
        } else {
            // Set text to red for incorrect guess
            selectedCell?.setTextColor(Color.RED)
        }
    }

    private fun updateCellBorder(cell: TextView, row: Int, col: Int, isSelected: Boolean){
        val thick = 6
        val thin = 2

        val top = if (row % 3 == 0) thick else thin
        val left = if (col % 3 == 0) thick else thin
        val bottom = if (row == 8) thick else 0
        val right = if (col == 8) thick else 0

        val baseDrawable = if (isSelected) {
            // Set highlighted state
            getDrawable(R.drawable.highlight_cell)?.constantState?.newDrawable()?.mutate() as LayerDrawable
        } else {
            // For the normal state, we can reuse the same technique as before.
            val border = GradientDrawable().apply {
                setColor(Color.WHITE)
                setStroke(thick, Color.BLACK)
            }
            val inset = GradientDrawable().apply { setColor(Color.WHITE) }
            LayerDrawable(arrayOf(border, inset))
        }
        if(isSelected && baseDrawable is LayerDrawable){
            baseDrawable.setLayerInset(1, left, top, right, bottom) // Main inset
            baseDrawable.setLayerInset(2, left + 2, top + 2, right + 2, bottom + 2) // Highlight inset
        } else if (baseDrawable is LayerDrawable) {
            baseDrawable.setLayerInset(1, left, top, right, bottom)
        }
        cell.background = baseDrawable
    }
}
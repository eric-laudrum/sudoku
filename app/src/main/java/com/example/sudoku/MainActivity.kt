package com.example.sudoku

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    // Variables
    private lateinit var gridLayout: GridLayout
    private var cellViews: Array<Array<TextView>>? = null
    private var puzzleBoard: Array<IntArray>? = null
    private var solutionBoard: Array<IntArray>? = null

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
                    setTextColor(Color.BLACK)

                    val number = puzzleBoard?.get(row)?.get(col) ?: 0

                    // Set text based on the number from the puzzle
                    text = if (number == 0) "" else number.toString()

                    // Create cell borders
                    val border = GradientDrawable()
                    border.setColor(Color.WHITE)

                    // Set line thicknesses
                    val thick = 6
                    val thin = 2

                    // Set border thickness for each side
                    val top = if (row % 3 == 0) thick else thin
                    val bottom = if (row == 8) thick else 0
                    val left = if (col % 3 == 0) thick else thin
                    val right = if (col == 8) thick else 0

                    // Create a LayerDrawable to combine strokes correctly
                    val inset = GradientDrawable()
                    inset.setColor(Color.WHITE)

                    val layers = arrayOf(border, inset)
                    val layerList = android.graphics.drawable.LayerDrawable(layers)

                    // Set the insets to reveal the borders underneath
                    layerList.setLayerInset(1, left, top, right, bottom)

                    // Set all borders to thick lines
                    border.setStroke(thick, Color.BLACK)

                    // Apply the final layered drawable as the background
                    this.background = layerList
                }
                gridLayout.addView(cell)
                cell
            }
        }
        cellViews = cells
    }
}
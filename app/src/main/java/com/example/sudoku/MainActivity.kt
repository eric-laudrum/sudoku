package com.example.sudoku

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
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
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape

class MainActivity : AppCompatActivity() {

    // --------------------------------------------------
    // ---------------- Properties
    // --------------------------------------------------
    private lateinit var gridLayout: GridLayout
    private var cellViews: Array<Array<TextView>>? = null
    private var puzzleBoard: Array<IntArray>? = null
    private var solutionBoard: Array<IntArray>? = null
    private var selectedCell: TextView? = null
    private val numberHighlightMap = mutableMapOf<Int, Int>()
    private var selectedRow = -1
    private var selectedCol = -1
    private val badgeTextViews = mutableMapOf<Int, TextView>()
    private val numberButtonContainers = mutableMapOf<Int, FrameLayout>()


    private enum class HighlightMode { ALL, ACTIVE_CELL }
    private var highlightMode = HighlightMode.ALL

    // --------------------------------------------------
    // ---------------- On Create
    // --------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Set toolbar to the action bar
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean{

        menuInflater.inflate(R.menu.settings_menu, menu)

        val highlightToggle = menu.findItem(R.id.action_toggle_highlight)
        highlightToggle.isChecked = (highlightMode == HighlightMode.ALL)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle when a menu item is clicked.
        return when (item.itemId) {
            R.id.action_toggle_highlight -> {
                // Toggle the checked state
                item.isChecked = !item.isChecked

                // Update our mode property based on the new state
                highlightMode = if (item.isChecked) {
                    HighlightMode.ALL
                } else {
                    HighlightMode.ACTIVE_CELL
                }

                // Refresh the entire grid to apply the new highlight style
                refreshAllCellHighlights()

                true // Indicate we've handled the click
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    // --------------------------------------------------
    // ---------------- Functions
    // --------------------------------------------------
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

                    // Apply listener to every cell
                    setOnClickListener {
                        selectCell(this, row, col)
                    }

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
        updateBadgeCounts()
    }
    private fun setupNumberButtons() {
        val numberButtonsLayout = findViewById<LinearLayout>(R.id.footer)
        if (numberButtonsLayout.childCount > 0) return

        for (number in 1..9) {
            // Inflate the custom layout for the button with a badge
            val buttonLayout = layoutInflater.inflate(R.layout.number_with_badge, numberButtonsLayout, false)

            val container = buttonLayout.findViewById<FrameLayout>(R.id.button_container)
            val numberText = buttonLayout.findViewById<TextView>(R.id.number_text)
            val badge = buttonLayout.findViewById<TextView>(R.id.badge_text_view)

            numberButtonContainers[number] = container

            // Store the badge reference
            badgeTextViews[number] = badge

            // Set text for number
            numberText.text = number.toString()

            container.setOnClickListener { onNumberButtonClick(number) }
            container.setOnLongClickListener {
                onNumberButtonLongClick(number)
                true
            }
            numberButtonsLayout.addView(buttonLayout)
        }
    }
    private fun selectCell(cell: TextView, row: Int, col: Int) {

        // Update state to the newly selected cell
        selectedCell = cell
        selectedRow = row
        selectedCol = col

        // Refresh the grid highlights
        refreshAllCellHighlights()
    }
    private fun onNumberButtonClick(number: Int) {
        // Check if cell is selected and can be edited
        if (selectedCell == null || selectedRow == -1) return

        val isCorrect = solutionBoard!![selectedRow][selectedCol] == number
        // Set the number in the TextView
        selectedCell?.text = number.toString()

        // Set text color based on correctness
        selectedCell?.setTextColor(if (isCorrect) Color.BLUE else Color.RED)

        if (isCorrect) {
            // Set text to blue for user input
            updateBadgeCounts()
        }
    }
    private fun updateCellBorder(cell: TextView, row: Int, col: Int, isSelected: Boolean, backgroundColor: Int = Color.WHITE ){
        val thick = 6
        val thin = 2

        val top = if (row % 3 == 0) thick else thin
        val left = if (col % 3 == 0) thick else thin
        val bottom = if (row == 8) thick else 0
        val right = if (col == 8) thick else 0

        val baseDrawable: LayerDrawable

        if (isSelected) {
            // Mutable drawable to modify layers
            baseDrawable = getDrawable(R.drawable.highlight_cell)?.constantState?.newDrawable()?.mutate() as LayerDrawable
            // Set the background color of the first layer
            (baseDrawable.getDrawable(1) as? GradientDrawable)?.setColor(backgroundColor)
        } else {
            // Drawable for normal state
            val border = GradientDrawable().apply {
                setColor(Color.BLACK) // The "grout" color
            }
            val inset = GradientDrawable().apply {
                setColor(backgroundColor) // Cell background color
            }
            baseDrawable = LayerDrawable(arrayOf(border, inset))
        }

        // Apply the insets to set thick/thin lines
        if (isSelected) {
            baseDrawable.setLayerInset(1, left, top, right, bottom) // Main inset
            // Insets for the blue selection stroke
            (baseDrawable.getDrawable(2) as? GradientDrawable)?.let {
                baseDrawable.setLayerInset(2, left + 1, top + 1, right + 1, bottom + 1)
            }
        } else {
            baseDrawable.setLayerInset(1, left, top, right, bottom)
        }

        cell.background = baseDrawable
    }
    private fun onNumberButtonLongClick(number: Int){
        ColorPickerDialog
            .Builder(this)
            .setTitle("Pick Highlight Color")
            .setColorShape(ColorShape.SQAURE)
            .setDefaultColor(Color.WHITE)
            .setColorListener { color, colorHex ->
                applyHighlightToNumber(number, color)
            }
            .show()
    }
    private fun applyHighlightToNumber(number: Int, colorToApply: Int) {
        // Save choice
        if (colorToApply == Color.WHITE) {
            // Remove highlight
            numberHighlightMap.remove(number)
        } else {
            // Save the number-to-color mapping.
            numberHighlightMap[number] = colorToApply
        }

        // Update background of the number buttons
        val buttonContainer = numberButtonContainers[number]
        if(colorToApply == Color.WHITE){
            buttonContainer?.setBackgroundResource(R.drawable.button_background_white)
        } else{
            val drawable = buttonContainer?.background?.mutate()

            if(drawable is android.graphics.drawable.RippleDrawable){
                val backgroundShape = drawable.getDrawable(1)

                if(backgroundShape is GradientDrawable){
                    backgroundShape.setColor(colorToApply)
                }
            }
        }

        if(highlightMode == HighlightMode.ALL){

            // Apply to all 81 cells
            for (r in 0..8) {
                for (c in 0..8) {
                    val cell = cellViews?.get(r)?.get(c)
                    if (cell != null && cell.text.toString() == number.toString()) {
                        val isSelected = (selectedRow == r && selectedCol == c)
                        updateCellBorder(cell, r, c, isSelected, colorToApply)
                    }
                }
            }
        }
    }
    private fun updateBadgeCounts() {
        if (cellViews == null) return

        val counts = IntArray(10) // index 0 is unused, 1-9 for numbers

        // Count every number currently visible on the grid
        for (row in 0..8) {
            for (col in 0..8) {
                val cellText = cellViews!![row][col].text
                if (cellText.isNotEmpty()) {
                    val number = cellText.toString().toInt()
                    counts[number]++
                }
            }
        }

        // Now, update each badge
        for (number in 1..9) {
            val count = counts[number]
            val badge = badgeTextViews[number]

            if (count == 9) {
                // If all 9 instances of a number are found, hide the badge.
                badge?.visibility = View.GONE
            } else {
                // Otherwise, show the badge with the number left to find.
                badge?.visibility = View.VISIBLE
                badge?.text = (9 - count).toString()
            }
        }
    }
    private fun refreshAllCellHighlights() {
        if (cellViews == null) return

        for (r in 0..8) {
            for (c in 0..8) {
                val cell = cellViews!![r][c]
                val cellText = cell.text.toString()

                var colorToApply = Color.WHITE // Default to no highlight

                if (cellText.isNotEmpty()) {
                    val number = cellText.toInt()

                    if (highlightMode == HighlightMode.ALL) {
                        // "All" mode: Get the color for this number, if it exists.
                        colorToApply = numberHighlightMap[number] ?: Color.WHITE
                    } else { // ACTIVE_CELL mode
                        // Only apply color if this cell's number matches the selected cell's number.
                        val selectedCellText = selectedCell?.text?.toString()
                        if (selectedCellText?.isNotEmpty() == true && number == selectedCellText.toInt()) {
                            colorToApply = numberHighlightMap[number] ?: Color.WHITE
                        }
                    }
                }

                val isSelected = (selectedRow == r && selectedCol == c)
                updateCellBorder(cell, r, c, isSelected, colorToApply)
            }
        }
    }
}
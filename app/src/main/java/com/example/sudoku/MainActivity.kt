package com.example.sudoku

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.google.android.material.slider.Slider
import java.io.BufferedReader
import java.io.InputStreamReader
import android.widget.Button

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
    private var singleHighlightColor: Int = Color.YELLOW // Default single highlight color
    private var selectedRow = -1
    private var selectedCol = -1
    private var activeNumber = -1
    private val badgeTextViews = mutableMapOf<Int, TextView>()
    private val numberButtonContainers = mutableMapOf<Int, FrameLayout>()
    private var difficultyLevel = 0
    private lateinit var completionMessages: MutableList<String>
    private var currentCompletionMessageIndex = 0
    private lateinit var completionTextView: TextView
    private var defaultBadgeColor: Int = Color.RED // Default badge color
    private lateinit var errorCountTextView: TextView
    private var isErrorCountingEnabled = false
    private var errorCount = 0

    private enum class HighlightMode { ALL, ACTIVE_CELL }
    private var highlightMode = HighlightMode.ACTIVE_CELL
    private var isHighlightingEnabled = true

    // --------------------------------------------------
    // ---------------- On Create
    // --------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        completionTextView = findViewById(R.id.completion_text)
        errorCountTextView = findViewById(R.id.error_count_text)

        // Load and apply all saved preferences
        loadAndApplyBackgroundColor()
        loadSingleHighlightColor()
        loadDifficulty()
        loadCompletionMessages()
        loadDefaultBadgeColor()
        loadErrorCountingState()

        errorCountTextView.visibility = if (isErrorCountingEnabled) View.VISIBLE else View.GONE

        // Set toolbar to the action bar
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Get grid view
        gridLayout = findViewById<GridLayout>(R.id.grid)

        // Setup eraser button
        val eraserButton = findViewById<Button>(R.id.eraser_button)
        eraserButton.setOnClickListener { onEraseButtonClick() }

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
                    startNewGame()
                    // Remove the listener so this only runs once
                    gridLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean{

        menuInflater.inflate(R.menu.settings_menu, menu)

        // Set initial state of the radio button group based on the current mode
        val modeToSelect = if (highlightMode == HighlightMode.ALL) {
            R.id.action_highlight_all_numbers
        } else {
            R.id.action_highlight_active_cell_numbers
        }
        menu.findItem(modeToSelect)?.isChecked = true

        menu.findItem(R.id.action_count_errors)?.isChecked = isErrorCountingEnabled

        // Return true to display the menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_new_game -> {
                showNewGameConfirmation()
                return true
            }

            // Highlight All Numbers
            R.id.action_highlight_all_numbers -> {
                item.isChecked = true // The group ensures the other is unchecked.
                highlightMode = HighlightMode.ALL
                updateNumberButtonHighlights()
                refreshAllCellHighlights() // Redraw the grid with the new mode.
                return true
            }

            // Highlight Active Number
            R.id.action_highlight_active_cell_numbers -> {
                item.isChecked = true
                highlightMode = HighlightMode.ACTIVE_CELL
                updateNumberButtonHighlights()
                refreshAllCellHighlights() // Redraw the grid with the new mode.
                return true
            }

            R.id.action_count_errors -> {
                item.isChecked = !item.isChecked
                isErrorCountingEnabled = item.isChecked
                saveErrorCountingState(isErrorCountingEnabled)
                if (isErrorCountingEnabled) {
                    errorCount = 0
                    updateErrorCountDisplay()
                    errorCountTextView.visibility = View.VISIBLE
                } else {
                    errorCountTextView.visibility = View.GONE
                }
                return true
            }

            R.id.action_set_single_highlight_color -> {
                showSingleHighlightColorPicker()
                return true
            }

            // Change Background Color
            R.id.action_change_background_color -> {
                showBackgroundColorPicker()
                return true
            }

            R.id.action_set_badge_color -> {
                showDefaultBadgeColorPicker()
                return true
            }

            R.id.action_set_difficulty -> {
                showDifficultySlider()
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun loadCompletionMessages() {
        try {
            val inputStream = resources.openRawResource(R.raw.completion_messages)
            val reader = BufferedReader(InputStreamReader(inputStream))
            completionMessages = reader.readLines().toMutableList()
            completionMessages.shuffle()
            currentCompletionMessageIndex = 0
        } catch (e: Exception) {
            completionMessages = mutableListOf("Congratulations!")
        }
    }

    private fun showCompletionMessage() {
        if (::completionMessages.isInitialized.not() || completionMessages.isEmpty()) {
            return
        }

        val message = completionMessages[currentCompletionMessageIndex]

        currentCompletionMessageIndex++
        if (currentCompletionMessageIndex >= completionMessages.size) {
            completionMessages.shuffle()
            currentCompletionMessageIndex = 0
        }

        completionTextView.text = message
        completionTextView.visibility = View.VISIBLE
    }

    private fun checkGameCompletion() {
        if (cellViews == null || solutionBoard == null) return

        for (r in 0..8) {
            for (c in 0..8) {
                val cell = cellViews!![r][c]
                if (cell.text.isNullOrEmpty() || cell.text.toString().toInt() != solutionBoard!![r][c]) {
                    return // Found an empty or incorrect cell, so the game isn't over.
                }
            }
        }

        // If this is reached, the board is complete and correct.
        showCompletionMessage()
    }

    private fun showNewGameConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Start New Game?")
            .setMessage("Are you sure you want to start a new game?")
            .setPositiveButton("Yes") { _, _ ->
                startNewGame()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showDifficultySlider() {
        val slider = Slider(this).apply {
            valueFrom = 0f
            valueTo = 10f
            stepSize = 1f
            value = difficultyLevel.toFloat()
        }

        AlertDialog.Builder(this)
            .setTitle("Set Difficulty")
            .setView(slider)
            .setPositiveButton("OK") { _, _ ->
                val newDifficulty = slider.value.toInt()
                difficultyLevel = newDifficulty
                saveDifficulty(newDifficulty)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveDifficulty(level: Int) {
        val prefs = getSharedPreferences("SudokuPrefs", MODE_PRIVATE)
        prefs.edit { putInt("difficulty_level", level) }
    }

    private fun loadDifficulty() {
        val prefs = getSharedPreferences("SudokuPrefs", MODE_PRIVATE)
        difficultyLevel = prefs.getInt("difficulty_level", 0)
    }

    private fun showBackgroundColorPicker() {
        val prefs = getSharedPreferences("SudokuPrefs", MODE_PRIVATE)
        val savedColor = prefs.getInt("background_color", Color.WHITE)

        ColorPickerDialog.Builder(this)
            .setTitle("Choose Background Color")
            .setColorShape(ColorShape.SQAURE)
            .setDefaultColor(savedColor)
            .setColorListener { selectedColor, _ ->
                val gameBackground = findViewById<View>(R.id.main)
                gameBackground.setBackgroundColor(selectedColor)
                saveBackgroundColorPreference(selectedColor)
            }
            .show()
    }

    private fun saveBackgroundColorPreference(color: Int) {
        val prefs = getSharedPreferences("SudokuPrefs", MODE_PRIVATE)
        prefs.edit { putInt("background_color", color) }
    }

    private fun loadAndApplyBackgroundColor() {
        val prefs = getSharedPreferences("SudokuPrefs", MODE_PRIVATE)
        val backgroundColor = prefs.getInt("background_color", Color.WHITE) // Default to white
        findViewById<View>(R.id.main).setBackgroundColor(backgroundColor)
    }

    private fun showSingleHighlightColorPicker() {
        ColorPickerDialog.Builder(this)
            .setTitle("Choose Highlight Color")
            .setColorShape(ColorShape.SQAURE)
            .setDefaultColor(singleHighlightColor)
            .setColorListener { selectedColor, _ ->
                // Set the new color
                singleHighlightColor = selectedColor
                saveSingleHighlightColor(selectedColor)

                // When a single color is chosen, switch to ACTIVE_CELL mode
                highlightMode = HighlightMode.ACTIVE_CELL
                invalidateOptionsMenu() // This updates the menu to show the correct checked item

                // Update the UI to reflect the change
                updateNumberButtonHighlights()
                refreshAllCellHighlights()
            }
            .show()
    }

    private fun saveSingleHighlightColor(color: Int) {
        val prefs = getSharedPreferences("SudokuPrefs", MODE_PRIVATE)
        prefs.edit { putInt("single_highlight_color", color) }
    }

    private fun loadSingleHighlightColor() {
        val prefs = getSharedPreferences("SudokuPrefs", MODE_PRIVATE)
        singleHighlightColor = prefs.getInt("single_highlight_color", Color.YELLOW)
    }

    private fun showDefaultBadgeColorPicker() {
        ColorPickerDialog.Builder(this)
            .setTitle("Choose Default Badge Color")
            .setColorShape(ColorShape.SQAURE)
            .setDefaultColor(defaultBadgeColor)
            .setColorListener { selectedColor, _ ->
                defaultBadgeColor = selectedColor
                saveDefaultBadgeColor(selectedColor)
                updateNumberButtonHighlights()
            }
            .show()
    }

    private fun saveDefaultBadgeColor(color: Int) {
        val prefs = getSharedPreferences("SudokuPrefs", MODE_PRIVATE)
        prefs.edit { putInt("default_badge_color", color) }
    }

    private fun loadDefaultBadgeColor() {
        val prefs = getSharedPreferences("SudokuPrefs", MODE_PRIVATE)
        defaultBadgeColor = prefs.getInt("default_badge_color", Color.RED)
    }

    private fun startNewGame() {
        completionTextView.visibility = View.GONE
        if (isErrorCountingEnabled) {
            errorCount = 0
            updateErrorCountDisplay()
        }
        // Generate the puzzle and solution
        val (puzzle, solution) = GameGenerator().generatePuzzle(difficultyLevel)
        puzzleBoard = puzzle
        solutionBoard = solution

        // Build the UI based on data just created.
        createBoard()
    }

    private fun updateErrorCountDisplay() {
        errorCountTextView.text = "Errors: $errorCount"
    }

    private fun saveErrorCountingState(isEnabled: Boolean) {
        val prefs = getSharedPreferences("SudokuPrefs", MODE_PRIVATE)
        prefs.edit { putBoolean("error_counting_enabled", isEnabled) }
    }

    private fun loadErrorCountingState() {
        val prefs = getSharedPreferences("SudokuPrefs", MODE_PRIVATE)
        isErrorCountingEnabled = prefs.getBoolean("error_counting_enabled", false)
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
        updateNumberButtonHighlights()
    }
    private fun setupNumberButtons() {
        // Target the correct container from the new layout
        val numberButtonsLayout = findViewById<LinearLayout>(R.id.number_pad_container)
        if (numberButtonsLayout.childCount > 0) return


        // Calculate size in DP for the button
        val desiredHeightInDp = 75
        val desiredHeightInPixels = (desiredHeightInDp * resources.displayMetrics.density).toInt()

        // Make button taller
        numberButtonsLayout.layoutParams.height = desiredHeightInPixels

        val buttonParams = LinearLayout.LayoutParams(
            0, // Width = 0dp
            LinearLayout.LayoutParams.MATCH_PARENT, // Height = MATCH_PARENT
            1f // Weight = 1
        )
        for (number in 1..9) {
            val buttonLayout = layoutInflater.inflate(R.layout.number_with_badge, numberButtonsLayout, false)
            buttonLayout.layoutParams = buttonParams

            // Your existing code to find views and set listeners
            val container = buttonLayout.findViewById<FrameLayout>(R.id.button_container)
            val numberText = buttonLayout.findViewById<TextView>(R.id.number_text)
            val badge = buttonLayout.findViewById<TextView>(R.id.badge_text_view)
            val background = badge.background as? GradientDrawable
            background?.setColor(defaultBadgeColor)
            numberButtonContainers[number] = container
            badgeTextViews[number] = badge
            numberText.text = number.toString()
            container.setOnClickListener { onNumberButtonClick(number) }
            container.setOnLongClickListener {
                onNumberButtonLongClick(number)
                true
            }
            numberButtonsLayout.addView(buttonLayout)
        }
    }

    private fun onEraseButtonClick() {
        if (selectedCell == null || selectedRow == -1) return

        // Check if the cell is part of the original puzzle
        val isOriginalNumber = puzzleBoard!![selectedRow][selectedCol] != 0
        if (isOriginalNumber) {
            return // Don't erase original numbers
        }

        selectedCell?.text = ""
        activeNumber = -1
        updateBadgeCounts()
        updateNumberButtonHighlights()
        refreshAllCellHighlights()
    }

    private fun selectCell(cell: TextView, row: Int, col: Int) {
        // Update state to the newly selected cell
        selectedCell = cell
        selectedRow = row
        selectedCol = col

        // If the selected cell has a number, update the active number for highlighting.
        // If the cell is empty, reset the active number to remove highlights.
        val cellText = cell.text.toString()
        activeNumber = if (cellText.isNotEmpty()) {
            cellText.toInt()
        } else {
            -1
        }

        // Refresh the grid and button highlights
        updateNumberButtonHighlights()
        refreshAllCellHighlights()
    }

    private fun onNumberButtonClick(number: Int) {
        // Check if cell is selected and can be edited
        if (selectedCell == null || selectedRow == -1) return

        if (puzzleBoard!![selectedRow][selectedCol] != 0) {
            return
        }

        val isCorrect = solutionBoard!![selectedRow][selectedCol] == number
        if (!isCorrect && isErrorCountingEnabled) {
            errorCount++
            updateErrorCountDisplay()
        }
        // Set the number in the TextView
        selectedCell?.text = number.toString()

        // Update active number and refresh highlights
        activeNumber = number
        updateNumberButtonHighlights()

        // Set text color based on correctness
        selectedCell?.setTextColor(if (isCorrect) Color.BLUE else Color.RED)

        updateBadgeCounts()

        if (isCorrect) {
            checkGameCompletion()
        }
        refreshAllCellHighlights()
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
        val defaultColor = numberHighlightMap[number] ?: Color.WHITE
        ColorPickerDialog
            .Builder(this)
            .setTitle("Pick Highlight Color")
            .setColorShape(ColorShape.SQAURE)
            .setDefaultColor(defaultColor)
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

        // When a color is set for a specific number, switch to ALL mode and make it active
        highlightMode = HighlightMode.ALL
        activeNumber = number // Make the number active to highlight it immediately
        invalidateOptionsMenu()

        // Update background of the number buttons and the grid
        updateNumberButtonHighlights()
        refreshAllCellHighlights()
    }
    private fun updateNumberButtonHighlights() {
        for ((number, container) in numberButtonContainers) {
            val numberSpecificColor = numberHighlightMap[number]

            // Determine highlight color for the button background
            val buttonHighlightColor = when (highlightMode) {
                HighlightMode.ALL -> numberSpecificColor
                HighlightMode.ACTIVE_CELL -> if (number == activeNumber) {
                    numberSpecificColor ?: singleHighlightColor // Use specific color if available, else single highlight color
                } else {
                    null
                }
            }

            // Update button background
            if (buttonHighlightColor == null || buttonHighlightColor == Color.WHITE) {
                container.setBackgroundResource(R.drawable.button_background_white)
            } else {
                val drawable = container.background?.mutate()
                if (drawable is RippleDrawable) {
                    (drawable.getDrawable(0) as? GradientDrawable)?.setColor(buttonHighlightColor)
                }
            }

            // Update badge background
            val badge = badgeTextViews[number]
            val badgeBackground = badge?.background as? GradientDrawable
            val badgeColorToShow = numberSpecificColor ?: defaultBadgeColor
            badgeBackground?.setColor(badgeColorToShow)
        }
    }
    private fun updateBadgeCounts() {
        if (cellViews == null || solutionBoard == null) return

        val counts = IntArray(10) // index 0 is unused, 1-9 for numbers

        // Count every correct number on the grid
        for (row in 0..8) {
            for (col in 0..8) {
                val cellText = cellViews!![row][col].text
                if (cellText.isNotEmpty()) {
                    val number = cellText.toString().toInt()
                    if (number == solutionBoard!![row][col]) {
                        counts[number]++
                    }
                }
            }
        }

        // Update badges and button states
        for (number in 1..9) {
            val count = counts[number]
            val badge = badgeTextViews[number]
            val buttonContainer = numberButtonContainers[number]

            if (count == 9) {
                badge?.visibility = View.GONE
                buttonContainer?.isEnabled = false
                buttonContainer?.alpha = 0.5f // Make it look disabled
            } else {
                badge?.visibility = View.VISIBLE
                badge?.text = (9 - count).toString()
                buttonContainer?.isEnabled = true
                buttonContainer?.alpha = 1.0f // Make it look enabled
            }
        }
    }
    private fun refreshAllCellHighlights() {
        if (cellViews == null) return

        for (r in 0..8) {
            for (c in 0..8) {
                val cell = cellViews!![r][c]
                val cellText = cell.text.toString()

                var colorToApply = Color.WHITE // Default: no highlight

                if (isHighlightingEnabled && cellText.isNotEmpty()) {
                    val numberInCell = cellText.toInt()

                    colorToApply = when (highlightMode) {
                        HighlightMode.ALL -> {
                            numberHighlightMap[numberInCell] ?: Color.WHITE
                        }
                        HighlightMode.ACTIVE_CELL -> {
                            if (activeNumber != -1 && numberInCell == activeNumber) {
                                numberHighlightMap[activeNumber] ?: singleHighlightColor
                            } else {
                                Color.WHITE
                            }
                        }
                    }
                }

                val isSelected = (selectedRow == r && selectedCol == c)
                updateCellBorder(cell, r, c, isSelected, colorToApply)
            }
        }
    }
}

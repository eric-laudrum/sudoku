package com.example.sudoku

import kotlin.math.min
import kotlin.random.Random

class GameGenerator {

    fun generatePuzzle(difficulty: Int): Pair<Array<IntArray>, Array<IntArray>> {
        val board = Array(9) { IntArray(9) }
        solve(board) // This creates a complete, valid Sudoku board.

        // Make a copy of the solved board to serve as the solution.
        val solution = board.map { it.clone() }.toTypedArray()

        // Now, remove numbers from the `board` to create the puzzle.
        removeNumbers(board, difficulty)

        return Pair(board, solution)
    }

    private fun solve(board: Array<IntArray>): Boolean {
        for (row in 0..8) {
            for (col in 0..8) {
                if (board[row][col] == 0) {
                    val numbers = (1..9).shuffled(Random)
                    for (num in numbers) {
                        if (isValid(board, row, col, num)) {
                            board[row][col] = num
                            if (solve(board)) {
                                return true
                            } else {
                                board[row][col] = 0 // backtrack
                            }
                        }
                    }
                    return false
                }
            }
        }
        return true
    }

    private fun isValid(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        for (i in 0..8) {
            if (board[row][i] == num || board[i][col] == num) return false
        }
        val startRow = row - row % 3
        val startCol = col - col % 3
        for (i in 0..2) {
            for (j in 0..2) {
                if (board[i + startRow][j + startCol] == num) return false
            }
        }
        return true
    }

    private fun removeNumbers(board: Array<IntArray>, difficulty: Int) {
        // 1. Determine the number of cells to remove based on difficulty.
        val cellsToRemove = 30 + (difficulty * 2)

        // 2. Determine the max number of any single digit to remove.
        //    This adds another layer of difficulty control.
        val maxPerDigit = when (difficulty) {
            in 0..2 -> 5
            in 3..5 -> 6
            in 6..8 -> 7
            else -> 8
        }

        val removedCount = IntArray(10) // 0 is unused, 1-9 for digits
        var totalRemoved = 0

        // Get all cell positions and shuffle them.
        val positions = (0..80).toMutableList().shuffled(Random)

        for (pos in positions) {
            if (totalRemoved >= cellsToRemove) break

            val row = pos / 9
            val col = pos % 9
            val num = board[row][col]

            if (num != 0 && removedCount[num] < maxPerDigit) {
                // Temporarily remove the number
                board[row][col] = 0

                // Check if the puzzle is still solvable with a unique solution.
                // For simplicity, we'll skip this check in this implementation.
                // In a real-world app, you'd need a robust solver here.

                // If the puzzle remains valid (in a real-world scenario),
                // then finalize the removal.
                removedCount[num]++
                totalRemoved++
            }
        }
    }
}

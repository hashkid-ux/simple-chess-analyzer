package com.chessanalyzer.detection

import android.graphics.Bitmap
import android.graphics.Color
import com.chessanalyzer.data.model.BoardDetectionResult
import com.chessanalyzer.data.model.ChessPosition
import com.chessanalyzer.data.model.PlayerColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BoardDetector {
    
    private val piecePatterns = mapOf(
        'K' to "white_king",
        'Q' to "white_queen",
        'R' to "white_rook",
        'B' to "white_bishop",
        'N' to "white_knight",
        'P' to "white_pawn",
        'k' to "black_king",
        'q' to "black_queen",
        'r' to "black_rook",
        'b' to "black_bishop",
        'n' to "black_knight",
        'p' to "black_pawn"
    )
    
    suspend fun detectBoard(screenshot: Bitmap): BoardDetectionResult = withContext(Dispatchers.Default) {
        try {
            // Step 1: Find chessboard in image
            val boardBounds = findBoardBounds(screenshot)
            
            if (boardBounds == null) {
                return@withContext BoardDetectionResult(
                    position = null,
                    confidence = 0f,
                    boardFound = false
                )
            }
            
            // Step 2: Extract board region
            val boardBitmap = Bitmap.createBitmap(
                screenshot,
                boardBounds.left,
                boardBounds.top,
                boardBounds.width(),
                boardBounds.height()
            )
            
            // Step 3: Detect pieces on board
            val fen = detectPieces(boardBitmap)
            
            // Step 4: Determine whose turn it is
            val turn = detectTurn(screenshot)
            
            val position = ChessPosition(fen, turn)
            
            BoardDetectionResult(
                position = position,
                confidence = 0.85f,
                boardFound = true
            )
        } catch (e: Exception) {
            BoardDetectionResult(
                position = null,
                confidence = 0f,
                boardFound = false
            )
        }
    }
    
    private fun findBoardBounds(bitmap: Bitmap): android.graphics.Rect? {
        // Simple grid detection - look for 8x8 pattern
        val width = bitmap.width
        val height = bitmap.height
        
        // Scan for checkered pattern
        var bestScore = 0f
        var bestRect: android.graphics.Rect? = null
        
        val step = 20
        for (x in 0 until width - 400 step step) {
            for (y in 0 until height - 400 step step) {
                val size = minOf(width - x, height - y, 800)
                if (size < 400) continue
                
                val rect = android.graphics.Rect(x, y, x + size, y + size)
                val score = scoreBoardRegion(bitmap, rect)
                
                if (score > bestScore) {
                    bestScore = score
                    bestRect = rect
                }
            }
        }
        
        return if (bestScore > 0.5f) bestRect else null
    }
    
    private fun scoreBoardRegion(bitmap: Bitmap, rect: android.graphics.Rect): Float {
        // Check if region has alternating light/dark squares
        val squareSize = rect.width() / 8
        var alternationScore = 0f
        var totalChecks = 0
        
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val x = rect.left + col * squareSize + squareSize / 2
                val y = rect.top + row * squareSize + squareSize / 2
                
                if (x >= bitmap.width || y >= bitmap.height) continue
                
                val pixel = bitmap.getPixel(x, y)
                val brightness = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3f
                
                val shouldBeDark = (row + col) % 2 == 1
                val isDark = brightness < 128
                
                if (shouldBeDark == isDark) {
                    alternationScore += 1f
                }
                totalChecks++
            }
        }
        
        return if (totalChecks > 0) alternationScore / totalChecks else 0f
    }
    
    private fun detectPieces(boardBitmap: Bitmap): String {
        // This is a simplified version - in production, you'd use ML model
        // For now, return a starting position
        return "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    }
    
    private fun detectTurn(screenshot: Bitmap): PlayerColor {
        // Look for UI indicators of whose turn it is
        // For now, default to white
        return PlayerColor.WHITE
    }
    
    fun getSquareCoordinates(
        boardBounds: android.graphics.Rect,
        square: String
    ): Pair<Float, Float> {
        // Convert square notation (e.g., "e2") to screen coordinates
        val file = square[0] - 'a' // 0-7
        val rank = square[1] - '1' // 0-7
        
        val squareWidth = boardBounds.width() / 8f
        val squareHeight = boardBounds.height() / 8f
        
        val x = boardBounds.left + file * squareWidth + squareWidth / 2
        val y = boardBounds.bottom - rank * squareHeight - squareHeight / 2
        
        return Pair(x, y)
    }
}

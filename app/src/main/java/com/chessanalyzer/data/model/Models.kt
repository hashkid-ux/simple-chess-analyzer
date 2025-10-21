package com.chessanalyzer.data.model

data class ChessPosition(
    val fen: String,
    val turn: PlayerColor
)

enum class PlayerColor {
    WHITE, BLACK
}

data class BestMove(
    val move: String,
    val fromSquare: String,
    val toSquare: String,
    val evaluation: Float,
    val depth: Int
)

data class EngineResponse(
    val success: Boolean,
    val bestmove: String?,
    val continuation: String?,
    val evaluation: Float?,
    val mate: Int?
)

enum class AnalysisSpeed(val maxDepth: Int, val timeLimit: Long) {
    BULLET(5, 1000L),      // 1 second max
    BLITZ(8, 3000L),       // 3 seconds max
    RAPID(12, 8000L),      // 8 seconds max
    CLASSICAL(15, 15000L)  // 15 seconds max
}

data class BoardDetectionResult(
    val position: ChessPosition?,
    val confidence: Float,
    val boardFound: Boolean
)

data class ArrowOverlay(
    val fromX: Float,
    val fromY: Float,
    val toX: Float,
    val toY: Float,
    val move: String
)

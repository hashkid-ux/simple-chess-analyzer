package com.chessanalyzer.data.repository

import com.chessanalyzer.data.api.ChessEngineApi
import com.chessanalyzer.data.model.BestMove
import com.chessanalyzer.data.model.EngineResponse
import kotlinx.coroutines.withTimeout

class ChessEngineRepository {
    
    private val api = ChessEngineApi.create()
    
    suspend fun getBestMove(fen: String, depth: Int, timeoutMs: Long): Result<BestMove> {
        return try {
            withTimeout(timeoutMs) {
                val response = api.analyzeFEN(fen, depth)
                
                if (response.isSuccessful && response.body() != null) {
                    val engineResponse = response.body()!!
                    
                    if (engineResponse.success && engineResponse.bestmove != null) {
                        val move = parseBestMove(engineResponse)
                        Result.success(move)
                    } else {
                        Result.failure(Exception("Engine returned no move"))
                    }
                } else {
                    Result.failure(Exception("API error: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun parseBestMove(response: EngineResponse): BestMove {
        val move = response.bestmove ?: ""
        
        // Parse UCI move format (e.g., "e2e4")
        val fromSquare = if (move.length >= 4) move.substring(0, 2) else ""
        val toSquare = if (move.length >= 4) move.substring(2, 4) else ""
        
        return BestMove(
            move = move,
            fromSquare = fromSquare,
            toSquare = toSquare,
            evaluation = response.evaluation ?: 0f,
            depth = 0
        )
    }
}

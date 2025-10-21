package com.chessanalyzer.service

import android.content.Context
import android.graphics.*
import android.view.View
import com.chessanalyzer.data.model.BestMove

class ArrowOverlayView(context: Context) : View(context) {
    
    private val arrowPaint = Paint().apply {
        color = Color.argb(200, 255, 215, 0) // Semi-transparent gold
        strokeWidth = 12f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    
    private val arrowHeadPaint = Paint().apply {
        color = Color.argb(200, 255, 215, 0)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private var currentArrow: ArrowData? = null
    
    data class ArrowData(
        val fromX: Float,
        val fromY: Float,
        val toX: Float,
        val toY: Float
    )
    
    fun drawArrow(bestMove: BestMove) {
        // You'll need to calculate screen coordinates based on detected board
        // For now, this is a placeholder
        currentArrow = ArrowData(0f, 0f, 0f, 0f)
        invalidate()
    }
    
    fun clearArrow() {
        currentArrow = null
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        currentArrow?.let { arrow ->
            // Draw arrow line
            canvas.drawLine(
                arrow.fromX, arrow.fromY,
                arrow.toX, arrow.toY,
                arrowPaint
            )
            
            // Draw arrow head
            val angle = Math.atan2(
                (arrow.toY - arrow.fromY).toDouble(),
                (arrow.toX - arrow.fromX).toDouble()
            )
            
            val arrowHeadLength = 40f
            val arrowHeadAngle = Math.PI / 6
            
            val path = Path().apply {
                moveTo(arrow.toX, arrow.toY)
                lineTo(
                    (arrow.toX - arrowHeadLength * Math.cos(angle - arrowHeadAngle)).toFloat(),
                    (arrow.toY - arrowHeadLength * Math.sin(angle - arrowHeadAngle)).toFloat()
                )
                lineTo(
                    (arrow.toX - arrowHeadLength * Math.cos(angle + arrowHeadAngle)).toFloat(),
                    (arrow.toY - arrowHeadLength * Math.sin(angle + arrowHeadAngle)).toFloat()
                )
                close()
            }
            
            canvas.drawPath(path, arrowHeadPaint)
        }
    }

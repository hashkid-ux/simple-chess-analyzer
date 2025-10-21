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

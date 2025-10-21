package com.chessanalyzer.service

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.chessanalyzer.ChessAnalyzerApp
import com.chessanalyzer.R
import com.chessanalyzer.databinding.OverlayControlBinding
import com.chessanalyzer.ui.MainActivity
import kotlinx.coroutines.*

class OverlayService : Service() {
    
    private var windowManager: WindowManager? = null
    private var overlayView: OverlayControlBinding? = null
    private var arrowView: ArrowOverlayView? = null
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var detectionJob: Job? = null
    
    private val app get() = application as ChessAnalyzerApp
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startOverlay()
            ACTION_STOP -> stopOverlay()
        }
        return START_STICKY
    }
    
    private fun startOverlay() {
        if (overlayView != null) return
        
        // Create control overlay
        overlayView = OverlayControlBinding.inflate(LayoutInflater.from(this))
        val params = createOverlayParams()
        windowManager?.addView(overlayView?.root, params)
        
        // Create arrow overlay
        arrowView = ArrowOverlayView(this)
        val arrowParams = createArrowOverlayParams()
        windowManager?.addView(arrowView, arrowParams)
        
        setupOverlayControls()
        startDetection()
    }
    
    private fun stopOverlay() {
        detectionJob?.cancel()
        
        overlayView?.root?.let { windowManager?.removeView(it) }
        overlayView = null
        
        arrowView?.let { windowManager?.removeView(it) }
        arrowView = null
        
        stopSelf()
    }
    
    private fun setupOverlayControls() {
        overlayView?.apply {
            btnStop.setOnClickListener {
                stopOverlay()
            }
            
            speedSpinner.setOnItemSelectedListener { _, _, position, _ ->
                val speeds = arrayOf("Bullet", "Blitz", "Rapid", "Classical")
                updateAnalysisSpeed(speeds[position])
            }
            
            btnPause.setOnClickListener {
                toggleDetection()
            }
        }
    }
    
    private fun startDetection() {
        detectionJob?.cancel()
        detectionJob = serviceScope.launch {
            while (isActive) {
                try {
                    performAnalysis()
                    delay(2000) // Check every 2 seconds
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    private suspend fun performAnalysis() {
        val screenshot = captureScreen() ?: return
        
        val detector = com.chessanalyzer.detection.BoardDetector()
        val result = detector.detectBoard(screenshot)
        
        if (result.boardFound && result.position != null) {
            val speed = app.preferenceManager.getAnalysisSpeed()
            val startTime = System.currentTimeMillis()
            
            val moveResult = app.engineRepository.getBestMove(
                fen = result.position.fen,
                depth = speed.maxDepth,
                timeoutMs = speed.timeLimit
            )
            
            val totalTime = System.currentTimeMillis() - startTime
            
            moveResult.onSuccess { bestMove ->
                withContext(Dispatchers.Main) {
                    updateOverlayWithMove(bestMove, totalTime)
                }
            }
        }
    }
    
    private fun updateOverlayWithMove(bestMove: com.chessanalyzer.data.model.BestMove, timeMs: Long) {
        overlayView?.apply {
            tvBestMove.text = "Best: ${bestMove.move}"
            tvEvaluation.text = "Eval: ${bestMove.evaluation}"
            tvTime.text = "${timeMs}ms"
        }
        
        // Draw arrow on board
        arrowView?.drawArrow(bestMove)
    }
    
    private fun captureScreen(): android.graphics.Bitmap? {
        // This requires MediaProjection API
        // Simplified version - you'll need to implement screen capture
        return null
    }
    
    private fun toggleDetection() {
        if (detectionJob?.isActive == true) {
            detectionJob?.cancel()
            overlayView?.btnPause?.text = "Resume"
        } else {
            startDetection()
            overlayView?.btnPause?.text = "Pause"
        }
    }
    
    private fun updateAnalysisSpeed(speed: String) {
        val analysisSpeed = when (speed) {
            "Bullet" -> com.chessanalyzer.data.model.AnalysisSpeed.BULLET
            "Blitz" -> com.chessanalyzer.data.model.AnalysisSpeed.BLITZ
            "Rapid" -> com.chessanalyzer.data.model.AnalysisSpeed.RAPID
            else -> com.chessanalyzer.data.model.AnalysisSpeed.CLASSICAL
        }
        app.preferenceManager.setAnalysisSpeed(analysisSpeed)
    }
    
    private fun createOverlayParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }
    }
    
    private fun createArrowOverlayParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Chess Analyzer")
            .setContentText("Analyzing chess games...")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Chess Analyzer Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopOverlay()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        private const val CHANNEL_ID = "chess_analyzer_channel"
        private const val NOTIFICATION_ID = 1001
    }
}

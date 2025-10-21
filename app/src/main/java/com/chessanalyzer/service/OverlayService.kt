package com.chessanalyzer.service

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.AdapterView
import androidx.core.app.NotificationCompat
import com.chessanalyzer.ChessAnalyzerApp
import com.chessanalyzer.R
import com.chessanalyzer.databinding.OverlayControlBinding
import com.chessanalyzer.detection.BoardDetector
import com.chessanalyzer.ui.MainActivity
import com.chessanalyzer.utils.ScreenCaptureHelper
import kotlinx.coroutines.*

class OverlayService : Service() {
    
    private var windowManager: WindowManager? = null
    private var overlayView: OverlayControlBinding? = null
    private var arrowView: ArrowOverlayView? = null
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var detectionJob: Job? = null
    
    private val app get() = application as ChessAnalyzerApp
    private val boardDetector = BoardDetector()
    
    private var screenCapture: ScreenCaptureHelper? = null
    private var lastCapturedBitmap: Bitmap? = null
    private var isAnalyzing = false
    private var boardBounds: android.graphics.Rect? = null
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val data: Intent? = intent.getParcelableExtra(EXTRA_DATA)
                if (data != null) {
                    startOverlay(resultCode, data)
                }
            }
            ACTION_STOP -> stopOverlay()
        }
        return START_STICKY
    }
    
    private fun startOverlay(resultCode: Int, data: Intent) {
        if (overlayView != null) return
        
        // Setup screen capture
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        
        screenCapture = ScreenCaptureHelper(this, mediaProjection)
        screenCapture?.startCapture { bitmap ->
            lastCapturedBitmap = bitmap
        }
        
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
        isAnalyzing = false
        detectionJob?.cancel()
        
        screenCapture?.stopCapture()
        screenCapture = null
        
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
            
            speedSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    val speeds = arrayOf("Bullet", "Blitz", "Rapid", "Classical")
                    updateAnalysisSpeed(speeds[position])
                }
                
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            
            btnPause.setOnClickListener {
                toggleDetection()
            }
        }
    }
    
    private fun startDetection() {
        isAnalyzing = true
        detectionJob?.cancel()
        detectionJob = serviceScope.launch {
            while (isActive && isAnalyzing) {
                try {
                    performAnalysis()
                    delay(2000) // Check every 2 seconds
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        overlayView?.tvBestMove?.text = "Error: ${e.message}"
                    }
                }
            }
        }
    }
    
    private suspend fun performAnalysis() = withContext(Dispatchers.Default) {
        val screenshot = lastCapturedBitmap ?: return@withContext
        
        // Detect board and position
        val result = boardDetector.detectBoard(screenshot)
        
        if (!result.boardFound || result.position == null) {
            withContext(Dispatchers.Main) {
                overlayView?.tvBestMove?.text = "No board detected"
            }
            return@withContext
        }
        
        // Get analysis settings
        val speed = app.preferenceManager.getAnalysisSpeed()
        val startTime = System.currentTimeMillis()
        
        // Get best move from engine
        val moveResult = app.engineRepository.getBestMove(
            fen = result.position.fen,
            depth = speed.maxDepth,
            timeoutMs = speed.timeLimit
        )
        
        val totalTime = System.currentTimeMillis() - startTime
        
        moveResult.onSuccess { bestMove ->
            withContext(Dispatchers.Main) {
                updateOverlayWithMove(bestMove, totalTime)
                
                // Calculate arrow coordinates if board was found
                boardBounds?.let { bounds ->
                    val fenParser = com.chessanalyzer.detection.FENParser()
                    val uciMove = fenParser.parseUCIMove(bestMove.move)
                    
                    if (uciMove != null) {
                        val fromCoords = boardDetector.getSquareCoordinates(bounds, uciMove.from)
                        val toCoords = boardDetector.getSquareCoordinates(bounds, uciMove.to)
                        
                        val arrowData = ArrowOverlayView.ArrowData(
                            fromCoords.first,
                            fromCoords.second,
                            toCoords.first,
                            toCoords.second
                        )
                        
                        arrowView?.currentArrow = arrowData
                        arrowView?.invalidate()
                    }
                }
            }
        }.onFailure { error ->
            withContext(Dispatchers.Main) {
                overlayView?.tvBestMove?.text = "Engine error"
            }
        }
    }
    
    private fun updateOverlayWithMove(bestMove: com.chessanalyzer.data.model.BestMove, timeMs: Long) {
        overlayView?.apply {
            tvBestMove.text = "Best: ${bestMove.move}"
            
            if (app.preferenceManager.getShowEvaluation()) {
                tvEvaluation.text = String.format("Eval: %.2f", bestMove.evaluation)
            } else {
                tvEvaluation.text = ""
            }
            
            tvTime.text = "${timeMs}ms"
        }
    }
    
    private fun toggleDetection() {
        isAnalyzing = !isAnalyzing
        
        if (isAnalyzing) {
            startDetection()
            overlayView?.btnPause?.text = "Pause"
        } else {
            detectionJob?.cancel()
            overlayView?.btnPause?.text = "Resume"
            arrowView?.clearArrow()
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
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 20
            y = 100
        }
    }
    
    private fun createArrowOverlayParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
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
            .setContentText("Analyzing chess games in real-time")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Chess Analyzer Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows chess analysis status"
            }
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
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_DATA = "EXTRA_DATA"
        private const val CHANNEL_ID = "chess_analyzer_channel"
        private const val NOTIFICATION_ID = 1001
    }
}

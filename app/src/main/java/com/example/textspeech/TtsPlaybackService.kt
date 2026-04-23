package com.example.textspeech

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import java.util.Locale

class TtsPlaybackService : MediaSessionService(), TextToSpeech.OnInitListener {

    companion object {
        const val ACTION_PLAY     = "com.example.textspeech.ACTION_PLAY"
        const val ACTION_PAUSE    = "com.example.textspeech.ACTION_PAUSE"
        const val ACTION_STOP     = "com.example.textspeech.ACTION_STOP"
        const val ACTION_NEXT     = "com.example.textspeech.ACTION_NEXT"
        const val ACTION_PREV     = "com.example.textspeech.ACTION_PREV"
        const val ACTION_SEEK     = "com.example.textspeech.ACTION_SEEK"
        const val ACTION_SET_SPEED= "com.example.textspeech.ACTION_SET_SPEED"
        const val EXTRA_TEXT      = "TEXT_TO_READ"
        const val EXTRA_SPEED     = "PLAYBACK_SPEED"
        const val EXTRA_INDEX     = "SEEK_INDEX"

        const val BROADCAST_INDEX = "com.example.textspeech.BROADCAST_INDEX"
        const val BROADCAST_PLAYING = "com.example.textspeech.BROADCAST_PLAYING"
        const val EXTRA_CURRENT_INDEX = "CURRENT_INDEX"
        const val EXTRA_IS_PLAYING = "IS_PLAYING"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "tts_playback_channel"
    }

    private var ttsEngine: TextToSpeech? = null
    private var isTtsReady = false
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private var wakeLock: PowerManager.WakeLock? = null

    // Playback state
    private var chunks: List<String> = emptyList()
    private var currentIndex: Int = 0
    private var isPlaying: Boolean = false
    private var playbackSpeed: Float = 1.0f

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()
        ttsEngine = TextToSpeech(this, this)

        createNotificationChannel()
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TextSpeech:TtsWakeLock").apply {
            setReferenceCounted(false)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "TTS Playback"
            val descriptionText = "Controls for text-to-speech playback"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundStatus() {
        val notification = getNotification("Reading article...")
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            }
        )
        wakeLock?.acquire(10 * 60 * 1000L /*10 minutes*/)
    }

    private fun stopForegroundStatus() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    private fun getNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TTS Reader")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = ttsEngine?.setLanguage(Locale.CHINESE)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TtsPlaybackService", "Chinese Language not supported or missing data.")
            } else {
                isTtsReady = true
                ttsEngine?.setSpeechRate(playbackSpeed)

                // One-at-a-time playback: when a chunk finishes, advance to the next
                ttsEngine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        broadcastIndex()
                    }

                    override fun onDone(utteranceId: String?) {
                        if (isPlaying) {
                            currentIndex++
                            if (currentIndex < chunks.size) {
                                speakCurrentChunk()
                            } else {
                                isPlaying = false
                                stopForegroundStatus()
                                broadcastIndex()
                            }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        Log.e("TtsPlaybackService", "TTS error on: $utteranceId")
                    }
                })

                // If text was queued before TTS was ready, start now
                if (chunks.isNotEmpty() && isPlaying) {
                    speakCurrentChunk()
                }
            }
        } else {
            Log.e("TtsPlaybackService", "TTS initialization failed.")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {

            ACTION_PLAY -> {
                val text = intent.getStringExtra(EXTRA_TEXT)
                if (!text.isNullOrEmpty()) {
                    // New article — reset state
                    ttsEngine?.stop()
                    chunks = text.split("\n").filter { it.trim().isNotEmpty() }
                    currentIndex = 0
                }
                isPlaying = true
                startForegroundStatus()
                if (isTtsReady) speakCurrentChunk()
            }

            ACTION_PAUSE -> {
                isPlaying = false
                ttsEngine?.stop()
                stopForegroundStatus()
            }

            ACTION_STOP -> {
                isPlaying = false
                currentIndex = 0
                ttsEngine?.stop()
                stopForegroundStatus()
            }

            ACTION_NEXT -> {
                ttsEngine?.stop()
                if (currentIndex < chunks.size - 1) currentIndex++
                if (isPlaying) speakCurrentChunk()
                broadcastIndex()
            }

            ACTION_PREV -> {
                ttsEngine?.stop()
                if (currentIndex > 0) currentIndex--
                if (isPlaying) speakCurrentChunk()
                broadcastIndex()
            }

            ACTION_SEEK -> {
                val targetIndex = intent.getIntExtra(EXTRA_INDEX, 0)
                if (targetIndex in chunks.indices) {
                    ttsEngine?.stop()
                    currentIndex = targetIndex
                    isPlaying = true
                    startForegroundStatus()
                    speakCurrentChunk()
                    broadcastIndex()
                }
            }

            ACTION_SET_SPEED -> {
                val speed = intent.getFloatExtra(EXTRA_SPEED, 1.0f)
                playbackSpeed = speed
                ttsEngine?.setSpeechRate(speed)
            }

            else -> {
                // Legacy fallback: plain text passed without an action
                val text = intent?.getStringExtra(EXTRA_TEXT)
                if (!text.isNullOrEmpty()) {
                    chunks = text.split("\n").filter { it.trim().isNotEmpty() }
                    currentIndex = 0
                    isPlaying = true
                    startForegroundStatus()
                    if (isTtsReady) speakCurrentChunk()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /** Speak only the current chunk (one sentence/paragraph at a time). */
    private fun speakCurrentChunk() {
        if (currentIndex >= chunks.size) {
            isPlaying = false
            broadcastIndex()
            return
        }
        val chunk = chunks[currentIndex]
        ttsEngine?.speak(chunk, TextToSpeech.QUEUE_FLUSH, null, "chunk_$currentIndex")
    }

    /** Broadcast the current playback index to the UI. */
    private fun broadcastIndex() {
        val broadcast = Intent(BROADCAST_INDEX).apply {
            putExtra(EXTRA_CURRENT_INDEX, currentIndex)
            putExtra(EXTRA_IS_PLAYING, isPlaying)
            setPackage(packageName)
        }
        sendBroadcast(broadcast)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        ttsEngine?.stop()
        ttsEngine?.shutdown()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}

package jp.uguisu.aikotoba.mmlt

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.txt_nifty.sketch.flmml.FlMML

class BackgroundService : Service() {
    val binder: Binder = ServiceBinder()

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStart(intent: Intent, startId: Int) {
        val flmml = FlMML.staticInstanceIfCreated
        if (flmml == null || !flmml.isPlaying) stopSelf()
    }

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.v("BackgroundService", "onDestroy()")
        FlMML.staticInstance.setListener(null)
    }

    //VerifyErrorを避けるために囲う
    private object ForegroundUtil {
        private const val TITLE = "Playing"
        private const val CHANNEL_ID = TITLE
        private const val NOTIFICATION_ID = 1

        @TargetApi(Build.VERSION_CODES.ECLAIR)
        fun start(service: BackgroundService) {
            val context = service.applicationContext

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(context)
            }

            service.startForeground(NOTIFICATION_ID, createNotification(context))
        }

        @TargetApi(Build.VERSION_CODES.ECLAIR)
        fun stop(service: BackgroundService) {
            service.stopForeground(true)
        }

        private fun createNotification(context: Context): Notification {
            val title = FlMML.staticInstance.metaTitle
            val text = title.ifEmpty { "unknown title" }

            val activityIntent = Intent(context, MainActivity::class.java)
            val pendingIntent =
                PendingIntent.getActivity(context, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE)

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                createHoneycombNotification(context, text, pendingIntent)
            } else {
                createEclairNotification(context, text, pendingIntent)
            }
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        private fun createHoneycombNotification(
            context: Context,
            text: String,
            pendingIntent: PendingIntent
        ): Notification {
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, CHANNEL_ID)
            } else {
                Notification.Builder(context)
            }

            // 通知について
            // 実行時にこちらからは権限を求めない
            // Android 13 以降で通知を見るには、手動で通知 ON に設定する必要があると思われる
            builder.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(TITLE)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setOngoing(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            }
            return builder.notification
        }

        @TargetApi(Build.VERSION_CODES.ECLAIR)
        private fun createEclairNotification(
            context: Context,
            text: String,
            pendingIntent: PendingIntent
        ): Notification {
            return try {
                val setLatestEventInfo = Notification::class.java.getMethod(
                    "setLatestEventInfo",
                    Context::class.java,
                    CharSequence::class.java,
                    CharSequence::class.java,
                    PendingIntent::class.java
                )

                val notification = Notification()
                notification.icon = R.drawable.ic_notification
                // notification.setLatestEventInfo(context, TITLE, text, pendingIntent);
                setLatestEventInfo.invoke(notification, context, TITLE, text, pendingIntent)
                notification
            } catch (e: Exception) {
                Log.v("BackgroundService", "failed to generate notification")
                Notification()
            }
        }

        @TargetApi(Build.VERSION_CODES.O)
        private fun createNotificationChannel(context: Context) {
            val notificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                CHANNEL_ID,
                TITLE, NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    internal inner class ServiceBinder : Binder() {
        private val useStartForeground = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR

        fun startPlaying() {
            startService(Intent(applicationContext, BackgroundService::class.java))
            if (useStartForeground) {
                ForegroundUtil.start(this@BackgroundService)
            } else {
                invokeSetForeground(true)
            }
        }

        fun stopPlaying() {
            if (useStartForeground) {
                ForegroundUtil.stop(this@BackgroundService)
            } else {
                invokeSetForeground(false)
            }
            stopSelf()
        }

        private fun invokeSetForeground(x: Boolean) {
            try {
                val setForeground = BackgroundService::class.java.getMethod(
                    "setForeground",
                    Boolean::class.javaPrimitiveType
                )
                // BackgroundService.this.setForeground(x);
                setForeground.invoke(this@BackgroundService, x)
            } catch (e: Exception) {
                Log.v("BackgroundService", "failed to invoke setForeground")
            }
        }

        fun activityClosed() {
            FlMML.staticInstance.setListener(object : FlMML.Listener() {
                override fun onComplete() {
                    stopPlaying()
                }
            })
        }
    }
}
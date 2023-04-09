package jp.uguisu.aikotoba.mmlt;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.txt_nifty.sketch.flmml.FlMML;

import java.lang.reflect.Method;

public class BackgroundService extends Service {

    Binder binder = new ServiceBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        FlMML flmml = FlMML.getStaticInstanceIfCreated();
        if (flmml == null || !flmml.isPlaying()) stopSelf();
    }

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.v("BackgroundService", "onDestroy()");
        FlMML.getStaticInstance().setListener(null);
    }

    //VerifyErrorを避けるために囲う
    private static class ForegroundUtil {

        private static String TITLE = "Playing";
        private static String CHANNEL_ID = TITLE;
        private static int NOTIFICATION_ID = 1;

        @TargetApi(Build.VERSION_CODES.ECLAIR)
        public static void start(BackgroundService service) {
            Context context = service.getApplicationContext();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                createNotificationChannel(context);

            service.startForeground(NOTIFICATION_ID, createNotification(context));
        }

        @TargetApi(Build.VERSION_CODES.ECLAIR)
        static void stop(BackgroundService service) {
            service.stopForeground(true);
        }

        private static Notification createNotification(Context context) {
            String title = FlMML.getStaticInstance().getMetaTitle();
            String text = title.length() == 0 ? "unknown title" : title;

            Intent activityIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE);

            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
                    createHoneycombNotification(context, text, pendingIntent) :
                    createEclairNotification(context, text, pendingIntent);
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        private static Notification createHoneycombNotification(Context context, String text, PendingIntent pendingIntent) {
            Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                    new Notification.Builder(context, CHANNEL_ID) :
                    new Notification.Builder(context);

            // 通知について
            // 実行時にこちらからは権限を求めない
            // Android 13 以降で通知を見るには、手動で通知 ON に設定する必要があると思われる
            builder.setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(TITLE)
                    .setContentText(text)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
            }

            return builder.getNotification();
        }

        @TargetApi(Build.VERSION_CODES.ECLAIR)
        private static Notification createEclairNotification(Context context, String text, PendingIntent pendingIntent) {
            try {
                Method setLatestEventInfo = Notification.class.getMethod("setLatestEventInfo",
                        Context.class, CharSequence.class, CharSequence.class, PendingIntent.class);

                Notification notification = new Notification();
                notification.icon = R.drawable.ic_notification;
                // notification.setLatestEventInfo(context, TITLE, text, pendingIntent);
                setLatestEventInfo.invoke(notification, context, TITLE, text, pendingIntent);
                return notification;
            } catch (Exception e) {
                Log.v("BackgroundService", "failed to generate notification");
                return new Notification();
            }
        }

        @TargetApi(Build.VERSION_CODES.O)
        private static void createNotificationChannel(Context context) {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager == null) return;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    TITLE, NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
    }

    class ServiceBinder extends Binder {
        private final boolean useStartForeground = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR;

        void startPlaying() {
            startService(new Intent(getApplicationContext(), BackgroundService.class));
            if (useStartForeground)
                ForegroundUtil.start(BackgroundService.this);
            else
                invokeSetForeground(true);
        }

        void stopPlaying() {
            if (useStartForeground)
                ForegroundUtil.stop(BackgroundService.this);
            else
                invokeSetForeground(false);
            stopSelf();
        }

        private void invokeSetForeground(boolean x) {
            try {
                Method setForeground = BackgroundService.class.getMethod("setForeground", boolean.class);
                // BackgroundService.this.setForeground(x);
                setForeground.invoke(BackgroundService.this, x);
            } catch (Exception e) {
                Log.v("BackgroundService", "failed to invoke setForeground");
            }
        }

        void activityClosed() {
            FlMML.getStaticInstance().setListener(new FlMML.Listener() {
                @Override
                public void onComplete() {
                    stopPlaying();
                }
            });
        }
    }
}

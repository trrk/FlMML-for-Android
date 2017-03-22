package jp.uguisu.aikotoba.mmlt;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import com.txt_nifty.sketch.flmml.FlMML;

public class BackgroundService extends Service {

    Binder binder = new ServiceBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        if (!FlMML.getStaticInstance().isPlaying()) stopSelf();
    }

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        FlMML.getStaticInstance().setListener(null);
    }

    //VerifyErrorを避ける用
    private static class NotificationUtil {
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public static void start(BackgroundService service) {
            Context context = service.getApplication();
            String title = FlMML.getStaticInstance().getMetaTitle();
            String desc = (title.length() == 0 ? "unknown title" : title);

            Intent activityIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, activityIntent, 0);

            service.startForeground(1, new Notification.Builder(context)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Playing")
                    .setContentText(desc)
                    .setContentIntent(pendingIntent)
                    .getNotification());
        }

        @TargetApi(Build.VERSION_CODES.ECLAIR)
        static void stop(BackgroundService service) {
            service.stopForeground(true);
        }
    }

    class ServiceBinder extends Binder {
        void startPlaying() {
            startService(new Intent(getApplicationContext(), BackgroundService.class));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                NotificationUtil.start(BackgroundService.this);
            }
            // (HONEYCOMB以前)
            //Notification notification = new Notification();
            //notification.icon = R.drawable.ic_notification;
            //notification.setLatestEventInfo(getApplicationContext(), "Playing", meta, pendingIntent);
        }

        void stopPlaying() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                NotificationUtil.stop(BackgroundService.this);
            }
            stopSelf();
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

package br.com.logdi.alertacarregamento

import android.app.*
import android.content.*
import android.os.Build

object AlertScheduler {
    fun scheduleNext(context: Context) {
        val prefs = context.getSharedPreferences("alerta", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", false)) return
        val now = System.currentTimeMillis()
        val next = ((now / 3_600_000L) + 1) * 3_600_000L
        val intent = Intent(context, AlertReceiver::class.java)
        val pi = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarm = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= 23) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
        else alarm.setExact(AlarmManager.RTC_WAKEUP, next, pi)
    }
}

class AlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = context.getSharedPreferences("alerta", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", false)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channelId = "alerta_carregamento"
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(channelId, "Alertas de carregamento", NotificationManager.IMPORTANCE_HIGH))
        val open = PendingIntent.getActivity(context, 2, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("ATENÇÃO - FOTOS DE CARREGAMENTO")
            .setContentText("Lembrete: registrar e enviar as fotos dos carregamentos.")
            .setStyle(Notification.BigTextStyle().bigText(prefs.getString("message", "Não esqueça de registrar as fotos dos carregamentos.")))
            .setContentIntent(open).setAutoCancel(true).build()
        manager.notify(1001, notification)
        AlertScheduler.scheduleNext(context)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) AlertScheduler.scheduleNext(context)
    }
}

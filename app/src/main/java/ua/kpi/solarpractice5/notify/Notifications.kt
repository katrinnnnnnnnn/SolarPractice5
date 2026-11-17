package ua.kpi.solarpractice5.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ua.kpi.solarpractice5.MainActivity

object Alerts {
    const val ACTION_ACK = "ua.kpi.solarpractice5.ACK"
    private const val CHANNEL = "solar_alerts"
    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Моніторинг станції", NotificationManager.IMPORTANCE_HIGH))
    }
    fun show(context: Context, id: Int, title: String, detail: String) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        create(context)
        val open = PendingIntent.getActivity(context, id, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val ack = PendingIntent.getBroadcast(context, id, Intent(context, AlertActionReceiver::class.java).apply { action = ACTION_ACK; putExtra("id", id) }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notice = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title).setContentText(detail)
            .setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open).addAction(android.R.drawable.ic_input_add, "Підтвердити", ack)
            .build()
        NotificationManagerCompat.from(context).notify(id, notice)
    }
}
class AlertActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Alerts.ACTION_ACK) return
        val id = intent.getIntExtra("id", 0)
        context.getSharedPreferences("solar_alerts", Context.MODE_PRIVATE)
            .edit().putInt("ack_count", context.getSharedPreferences("solar_alerts", Context.MODE_PRIVATE).getInt("ack_count",0)+1).apply()
        NotificationManagerCompat.from(context).cancel(id)
    }
}

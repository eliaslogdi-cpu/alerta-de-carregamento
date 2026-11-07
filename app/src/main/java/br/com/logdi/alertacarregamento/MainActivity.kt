package br.com.logdi.alertacarregamento

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("alerta", MODE_PRIVATE) }
    private val defaultMessage = "ATENÇÃO, EQUIPE! 📸\nNão esqueçam de realizar o registro fotográfico de todos os carregamentos e encaminhar as fotos no grupo correspondente.\n\nO registro é obrigatório para comprovar as condições da carga e a qualidade da operação."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 36, 28, 24) }
        layout.addView(TextView(this).apply { text = "ALERTA DE CARREGAMENTO"; textSize = 22f; setTextColor(0xFF176B45.toInt()) })
        layout.addView(TextView(this).apply { text = "Receba um lembrete no início de cada hora para registrar e enviar as fotos dos carregamentos."; textSize = 15f; setPadding(0,16,0,20) })
        val message = EditText(this).apply { minLines = 5; setText(prefs.getString("message", defaultMessage)); hint = "Mensagem do alerta" }
        layout.addView(TextView(this).apply { text = "Mensagem do alerta:" })
        layout.addView(message)
        val start = Button(this).apply { text = "ATIVAR ALERTAS DE HORA EM HORA" }
        layout.addView(start)
        val stop = Button(this).apply { text = "DESATIVAR ALERTAS" }
        layout.addView(stop)
        val whatsapp = Button(this).apply { text = "ABRIR WHATSAPP" }
        layout.addView(whatsapp)
        layout.addView(TextView(this).apply { text = "O aplicativo emite uma notificação. O envio ao grupo do WhatsApp deve ser confirmado manualmente."; textSize = 12f; setPadding(0,18,0,0) })
        setContentView(layout)

        start.setOnClickListener {
            prefs.edit().putString("message", message.text.toString()).putBoolean("enabled", true).apply()
            if (Build.VERSION.SDK_INT >= 31 && !getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) {
                try {
                    val settingsIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:$packageName"))
                    startActivity(settingsIntent)
                    Toast.makeText(this, "Ative 'Alarmes e lembretes' para este aplicativo e volte.", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    prefs.edit().putBoolean("enabled", false).apply()
                    Toast.makeText(this, "Não foi possível abrir a permissão de alarmes. Verifique as configurações do aplicativo.", Toast.LENGTH_LONG).show()
                }
            } else {
                AlertScheduler.scheduleNext(this)
                Toast.makeText(this, "Alertas ativados para a próxima hora cheia.", Toast.LENGTH_LONG).show()
            }
        }
        stop.setOnClickListener {
            prefs.edit().putBoolean("enabled", false).apply()
            val pending = PendingIntent.getBroadcast(this, 1, Intent(this, AlertReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            getSystemService(AlarmManager::class.java).cancel(pending)
            Toast.makeText(this, "Alertas desativados.", Toast.LENGTH_SHORT).show()
        }
        whatsapp.setOnClickListener {
            val launch = packageManager.getLaunchIntentForPackage("com.whatsapp")
            if (launch != null) startActivity(launch) else startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://whatsapp.com")))
        }
    }

    override fun onResume() {
        super.onResume()
        if (prefs.getBoolean("enabled", false)) {
            val alarm = getSystemService(AlarmManager::class.java)
            if (Build.VERSION.SDK_INT < 31 || alarm.canScheduleExactAlarms()) {
                AlertScheduler.scheduleNext(this)
            }
        }
    }
}

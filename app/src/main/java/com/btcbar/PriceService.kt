package com.btcbar

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

class PriceService : Service() {

    private val channelId = "btc_price"
    private val urls = listOf(
        "https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT",
        "https://data-api.binance.vision/api/v3/ticker/price?symbol=BTCUSDT"
    )
    private var executor: ScheduledExecutorService? = null
    private var interval = 1L
    private var last = 0.0
    private var color = Color.parseColor("#2E7D32")
    private lateinit var nm: NotificationManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(channelId, "Prezzo BTC", NotificationManager.IMPORTANCE_LOW)
        ch.setShowBadge(false)
        nm.createNotificationChannel(ch)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        interval = getSharedPreferences("s", MODE_PRIVATE).getInt("interval", 1).coerceIn(1, 3600).toLong()
        val n = build(null)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
        executor?.shutdownNow()
        executor = Executors.newSingleThreadScheduledExecutor().also {
            it.scheduleWithFixedDelay({
                try { tick() } catch (e: Exception) { /* riprova al prossimo ciclo */ }
            }, 0, interval, TimeUnit.SECONDS)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        executor?.shutdownNow()
        executor = null
        super.onDestroy()
    }

    private fun tick() {
        val p = fetch() ?: return
        if (p > last) color = Color.parseColor("#2E7D32")
        else if (p < last) color = Color.parseColor("#C62828")
        last = p
        nm.notify(1, build(p))
    }

    private fun fetch(): Double? {
        for (u in urls) {
            try {
                val c = URL(u).openConnection() as HttpURLConnection
                c.connectTimeout = 3000
                c.readTimeout = 3000
                val body = c.inputStream.bufferedReader().use { it.readText() }
                c.disconnect()
                return JSONObject(body).getString("price").toDouble()
            } catch (e: Exception) { /* prova il prossimo endpoint */ }
        }
        return null
    }

    private fun build(price: Double?): Notification {
        val title = if (price == null) "BTC: caricamento..."
        else "BTC  \$" + String.format(Locale.US, "%,.2f", price)
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, channelId)
            .setSmallIcon(iconFor(price))
            .setContentTitle(title)
            .setContentText("Binance - aggiornamento ogni " + interval + " s")
            .setColor(color)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(open)
            .build()
    }

    // Icona a due righe: es. 67432 -> "67" sopra, "432" sotto
    private fun iconFor(price: Double?): Icon {
        var top = "BTC"
        var bottom = "..."
        if (price != null) {
            val s = price.roundToLong().toString()
            if (s.length > 3) { top = s.dropLast(3); bottom = s.takeLast(3) } else { bottom = s }
        }
        val size = 96
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        fun draw(t: String, baseline: Float) {
            paint.textSize = 52f
            val w = paint.measureText(t)
            if (w > size - 2) paint.textSize = 52f * (size - 2) / w
            canvas.drawText(t, size / 2f, baseline, paint)
        }
        draw(top, 44f)
        draw(bottom, 90f)
        return Icon.createWithBitmap(bmp)
    }
}

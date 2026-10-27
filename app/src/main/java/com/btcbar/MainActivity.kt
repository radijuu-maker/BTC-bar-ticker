package com.btcbar

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }
        root.addView(TextView(this).apply {
            text = "Prezzo BTC nella barra di stato (dati Binance).\nAggiorna ogni quanti secondi? (1 - 3600)"
            textSize = 16f
        })
        input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(getSharedPreferences("s", MODE_PRIVATE).getInt("interval", 1).toString())
        }
        root.addView(input)
        root.addView(Button(this).apply {
            text = "Avvia / Applica"
            setOnClickListener { begin() }
        })
        root.addView(Button(this).apply {
            text = "Ferma"
            setOnClickListener {
                setOn(false)
                stopService(Intent(this@MainActivity, PriceService::class.java))
            }
        })
        setContentView(root)
    }

    private fun begin() {
        val sec = (input.text.toString().toIntOrNull() ?: 1).coerceIn(1, 3600)
        input.setText(sec.toString())
        getSharedPreferences("s", MODE_PRIVATE).edit().putInt("interval", sec).apply()
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        } else launch()
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, res: IntArray) {
        super.onRequestPermissionsResult(code, perms, res)
        if (res.firstOrNull() == PackageManager.PERMISSION_GRANTED) launch()
    }

    private fun launch() {
        setOn(true)
        startForegroundService(Intent(this, PriceService::class.java))
    }

    private fun setOn(v: Boolean) {
        getSharedPreferences("s", MODE_PRIVATE).edit().putBoolean("on", v).apply()
    }
}

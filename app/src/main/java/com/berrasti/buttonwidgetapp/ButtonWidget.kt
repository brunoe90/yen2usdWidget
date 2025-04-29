package com.berrasti.buttonwidgetapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.StrictMode
import android.util.Log
import android.widget.RemoteViews
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ButtonWidget : AppWidgetProvider() {

    companion object {
        var currentInput: String = ""
        var yenToUsdRate: Double = 150.0
        var isYenToUsd: Boolean = true  // NUEVA VARIABLE: modo actual
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        fetchExchangeRate(context)

        for (appWidgetId in appWidgetIds) {
            setupWidget(context, appWidgetManager, appWidgetId)
        }

        scheduleDailyUpdate(context) // <-- NUEVO

    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val action = intent.action
        if (action != null && action.startsWith("ACTION_")) {
            val actionValue = action.removePrefix("ACTION_")
            when (actionValue) {
                "CLEAR" -> currentInput = ""
                "BACK" -> if (currentInput.isNotEmpty()) currentInput = currentInput.dropLast(1)
                "CONVERT" -> {
                    // CAMBIAMOS EL MODO al tocar el botón "="
                    isYenToUsd = !isYenToUsd
                }
                "DEBUG" -> {
                    fetchExchangeRate(context)

                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val componentName = ComponentName(context, ButtonWidget::class.java)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

                    for (id in appWidgetIds) {
                        setupWidget(context, appWidgetManager, id)
                    }
                }
                else -> {
                    if (actionValue == "." && currentInput.contains(".")) {
                        // No agregar doble punto
                    } else {
                        currentInput += actionValue
                    }
                }
            }

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ButtonWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (id in appWidgetIds) {
                setupWidget(context, appWidgetManager, id)
            }
        }
    }

    fun setupWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        val inputToShow = if (currentInput.isEmpty()) "0" else currentInput
        val amount = currentInput.toDoubleOrNull() ?: 0.0

        if (isYenToUsd) {
            // JPY → USD
            views.setTextViewText(R.id.textYen, "$inputToShow")
            val converted = amount * yenToUsdRate
            views.setTextViewText(R.id.textUsd, String.format("%.2f", converted))

            // Banderas

            views.setImageViewResource(R.id.imgUSA,  R.drawable.flag_usa)
            views.setImageViewResource(R.id.imgJapan, R.drawable.flag_japan)

        } else {
            // USD → JPY
            views.setTextViewText(R.id.textYen, "$inputToShow")
            val converted = amount / yenToUsdRate   // CORRECTO: dividir ahora
            views.setTextViewText(R.id.textUsd, String.format("%.2f", converted))

            // Banderas invertidas
            views.setImageViewResource(R.id.imgUSA, R.drawable.flag_japan)
            views.setImageViewResource(R.id.imgJapan, R.drawable.flag_usa)

        }
        val buttons = listOf(
            Pair(R.id.btn0, "0"), Pair(R.id.btn1, "1"), Pair(R.id.btn2, "2"),
            Pair(R.id.btn3, "3"), Pair(R.id.btn4, "4"), Pair(R.id.btn5, "5"),
            Pair(R.id.btn6, "6"), Pair(R.id.btn7, "7"), Pair(R.id.btn8, "8"),
            Pair(R.id.btn9, "9"),
            Pair(R.id.btnDot, "."),
            Pair(R.id.btnClear, "CLEAR"),
            Pair(R.id.btnBack, "BACK"),
            Pair(R.id.btnConvert, "CONVERT"),
            Pair(R.id.btnDebug, "DEBUG") // <-- AGREGADO
        )

        for ((id, actionValue) in buttons) {
            val intent = Intent(context, ButtonWidget::class.java).apply {
                action = "ACTION_$actionValue"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(id, pendingIntent)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun fetchExchangeRate(context: Context) {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        try {
            val url = URL("https://open.er-api.com/v6/latest/JPY")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode == 200) {
                val inputStream = connection.inputStream
                val json = inputStream.bufferedReader().use { it.readText() }

                val jsonObject = JSONObject(json)

                if (jsonObject.getString("result") == "success") {
                    val usdRate = jsonObject.getJSONObject("rates").getDouble("USD")
                    ButtonWidget.yenToUsdRate = usdRate
                    Log.d("ButtonWidget", "Tasa de cambio actualizada: 1 JPY = $usdRate USD")
                } else {
                    Log.e("ButtonWidget", "Error en respuesta JSON: no success")
                    // No cambiamos el valor si la respuesta no es success
                }
            } else {
                Log.e("ButtonWidget", "Error HTTP: ${connection.responseCode}")
                // No cambiamos el valor si no es 200 OK
            }

        } catch (e: Exception) {
            Log.e("ButtonWidget", "Error al obtener tasa de cambio", e)
            // No cambiamos el valor si hay cualquier excepción
        }
    }

    private fun scheduleDailyUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(context, UpdateWidgetReceiver::class.java)
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // Cancelar alarma previa (por si existe)
        alarmManager.cancel(pendingIntent)
        Log.d("ButtonWidget", "Alarma programada para actualización diaria")
        // Disparar a las 9 AM todos los días
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(java.util.Calendar.HOUR_OF_DAY, 9)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)

            // Si ya pasó la hora de hoy, programarlo para mañana
            if (before(java.util.Calendar.getInstance())) {
                add(java.util.Calendar.DATE, 1)
            }
        }

        alarmManager.setRepeating(
            android.app.AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            android.app.AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}
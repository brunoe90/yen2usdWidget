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
        var yenToUsdRate: Double = 143.75
        var isYenToUsd: Boolean = true  // NUEVA VARIABLE: modo actual
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        fetchExchangeRate(context)

        for (appWidgetId in appWidgetIds) {
            setupWidget(context, appWidgetManager, appWidgetId)
        }
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

    private fun setupWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        val inputToShow = if (currentInput.isEmpty()) "0" else currentInput
        val amount = currentInput.toDoubleOrNull() ?: 0.0

        if (!isYenToUsd) {
            // JPY → USD
            views.setTextViewText(R.id.textYen, "$inputToShow")
            val converted = amount * yenToUsdRate
            views.setTextViewText(R.id.textUsd, String.format("%.2f", converted))

            // Banderas
            views.setImageViewResource(R.id.imgUSA, R.drawable.flag_japan)
            views.setImageViewResource(R.id.imgJapan, R.drawable.flag_usa)

        } else {
            // USD → JPY
            views.setTextViewText(R.id.textYen, "$inputToShow")
            val converted = amount / yenToUsdRate   // CORRECTO: dividir ahora
            views.setTextViewText(R.id.textUsd, String.format("%.2f", converted))

            // Banderas invertidas
            views.setImageViewResource(R.id.imgUSA,  R.drawable.flag_usa)
            views.setImageViewResource(R.id.imgJapan, R.drawable.flag_japan)

        }
        val buttons = listOf(
            Pair(R.id.btn0, "0"), Pair(R.id.btn1, "1"), Pair(R.id.btn2, "2"),
            Pair(R.id.btn3, "3"), Pair(R.id.btn4, "4"), Pair(R.id.btn5, "5"),
            Pair(R.id.btn6, "6"), Pair(R.id.btn7, "7"), Pair(R.id.btn8, "8"),
            Pair(R.id.btn9, "9"),
            Pair(R.id.btnDot, "."),
            Pair(R.id.btnClear, "CLEAR"),
            Pair(R.id.btnBack, "BACK"),
            Pair(R.id.btnConvert, "CONVERT")
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
            val url = URL("https://api.exchangerate.host/latest?base=JPY&symbols=USD")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            val inputStream = connection.inputStream
            val json = inputStream.bufferedReader().use { it.readText() }

            val jsonObject = JSONObject(json)
            val usdRate = jsonObject.getJSONObject("rates").getDouble("USD")

            yenToUsdRate = usdRate

        } catch (e: Exception) {
            Log.e("ButtonWidget", "Error al obtener tasa de cambio", e)
        }
    }
}
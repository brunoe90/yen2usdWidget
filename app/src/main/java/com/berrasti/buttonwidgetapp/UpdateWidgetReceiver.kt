package com.berrasti.buttonwidgetapp

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.StrictMode
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class UpdateWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, ButtonWidget::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        // Descargar la tasa actualizada
        fetchExchangeRate(context)

        // Actualizar los widgets
        for (id in appWidgetIds) {
            ButtonWidget().setupWidget(context, appWidgetManager, id)
        }
    }

    private fun fetchExchangeRate(context: Context) {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        try {
            val url = URL("https://open.er-api.com/v6/latest/JPY")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            val inputStream = connection.inputStream
            val json = inputStream.bufferedReader().use { it.readText() }

            val jsonObject = JSONObject(json)
            val usdRate = jsonObject.getJSONObject("rates").getDouble("USD")

            ButtonWidget.yenToUsdRate = usdRate

            Log.d("UpdateWidgetReceiver", "Nueva tasa descargada: 1 JPY = $usdRate USD")

        } catch (e: Exception) {
            Log.e("UpdateWidgetReceiver", "Error al descargar tasa de cambio", e)
        }
    }
}
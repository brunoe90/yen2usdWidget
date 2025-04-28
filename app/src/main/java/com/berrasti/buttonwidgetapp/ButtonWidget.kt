package com.berrasti.buttonwidgetapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class ButtonWidget : AppWidgetProvider() {

    companion object {
        var currentInput: String = ""
        private const val yenToUsdRate = 133.5 // Fijo, después lo mejoramos si querés
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
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
                "CONVERT" -> { /* Solo recalcula */ }
                else -> {
                    // Agregar número o punto decimal, validando que solo haya un "."
                    if (actionValue == "." && currentInput.contains(".")) {
                        // No agregar otro punto si ya existe uno
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

        // Actualizar los textos
        val inputToShow = if (currentInput.isEmpty()) "0" else currentInput
        views.setTextViewText(R.id.textYen, "$inputToShow YEN")

        val amount = currentInput.toDoubleOrNull() ?: 0.0
        val converted = amount / yenToUsdRate
        views.setTextViewText(R.id.textUsd, String.format("%.2f USD", converted))

        // Setear acciones en los botones
        val buttons = listOf(
            Pair(R.id.btn0, "0"), Pair(R.id.btn1, "1"), Pair(R.id.btn2, "2"),
            Pair(R.id.btn3, "3"), Pair(R.id.btn4, "4"), Pair(R.id.btn5, "5"),
            Pair(R.id.btn6, "6"), Pair(R.id.btn7, "7"), Pair(R.id.btn8, "8"),
            Pair(R.id.btn9, "9"),
            Pair(R.id.btnDot, "."),   // <-- AGREGADO
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
}
package com.printer.playingcards2

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView

class UpdateDialog(
    context: Context,
    private val currentVersion: String,
    private val latestVersion: String,
    private val onUpdate: () -> Unit
) : Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val root = LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#CC000000"))
            gravity = android.view.Gravity.CENTER
            orientation = LinearLayout.VERTICAL
        }

        val card = CardView(context).apply {
            val width = if (isLandscape) {
                (context.resources.displayMetrics.widthPixels * 0.7).toInt()
            } else {
                (context.resources.displayMetrics.widthPixels * 0.85).toInt()
            }
            layoutParams = LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            radius = 32f
            cardElevation = 20f
            setCardBackgroundColor(Color.parseColor("#1E1E2E"))
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 48, 40, 48)
        }

        val icon = TextView(context).apply {
            text = "⬆️"
            textSize = 64f
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val title = TextView(context).apply {
            text = "Доступно обновление!"
            textSize = 26f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 24 }
        }

        val version = TextView(context).apply {
            text = "Текущая: v$currentVersion → Новая: v$latestVersion"
            textSize = 18f
            setTextColor(Color.parseColor("#FFD700"))
            gravity = android.view.Gravity.CENTER
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 32 }
        }

        val line = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            ).apply { bottomMargin = 32 }
            setBackgroundColor(Color.parseColor("#40FFFFFF"))
        }

        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val later = Button(context).apply {
            text = "Позже"
            textSize = 18f
            setTextColor(Color.WHITE)
            setBackgroundResource(R.drawable.button_rounded_gray)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 20 }
            setOnClickListener { dismiss() }
        }

        val update = Button(context).apply {
            text = "Обновить"
            textSize = 18f
            setTextColor(Color.WHITE)
            setBackgroundResource(R.drawable.button_rounded)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                dismiss()
                onUpdate()
            }
        }

        buttons.addView(later)
        buttons.addView(update)

        content.addView(icon)
        content.addView(title)
        content.addView(version)
        content.addView(line)
        content.addView(buttons)

        card.addView(content)
        root.addView(card)
        setContentView(root)

        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}
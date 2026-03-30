package com.printer.playingcards2

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.BounceInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView

class CardDetailDialog(
    context: Context,
    private val card: Card,
    private val onAnimationActivated: (Card) -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_card_detail)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        setupViews()
        startEntranceAnimation()
    }

    private fun setupViews() {
        try {
            val detailImage = findViewById<ShapeableImageView>(R.id.detailCardImage)
            detailImage?.setImageResource(card.photoResId)

            if (card.name == "Красный дьявол") {
                detailImage?.isClickable = true
                detailImage?.isFocusable = true
                detailImage?.setOnClickListener {
                    activateAnimation()
                }
                detailImage?.setStrokeWidth(4.0f)
                detailImage?.setStrokeColorResource(android.R.color.holo_red_dark)
                startPulseAnimation(detailImage)
            }

            findViewById<TextView>(R.id.detailCardName)?.text = card.name

            val rarityColor = ContextCompat.getColor(context, card.rarity.colorResId)
            val rarityIndicator = findViewById<View>(R.id.rarityIndicator)
            rarityIndicator?.setBackgroundColor(rarityColor)

            ObjectAnimator.ofFloat(rarityIndicator, View.SCALE_X, 0f, 1f).apply {
                duration = 500
                interpolator = AnticipateOvershootInterpolator()
                start()
            }

            findViewById<TextView>(R.id.rarityValue)?.text = card.rarity.displayName
            findViewById<TextView>(R.id.rarityValue)?.setTextColor(rarityColor)

            findViewById<TextView>(R.id.descriptionValue)?.text = card.description
            findViewById<TextView>(R.id.featureValue)?.text = card.specialFeature

            animateStatBar(R.id.healthBar, card.health, 200, "#E74C3C")
            animateStatBar(R.id.attackBar, card.attack, 150, "#E67E22")
            animateStatBar(R.id.defenseBar, card.defense, 150, "#3498DB")

            findViewById<TextView>(R.id.healthValue)?.text = card.health.toString()
            findViewById<TextView>(R.id.attackValue)?.text = card.attack.toString()
            findViewById<TextView>(R.id.defenseValue)?.text = card.defense.toString()

            findViewById<MaterialCardView>(R.id.closeButton)?.setOnClickListener {
                animateExit {
                    dismiss()
                }
            }

            // Анимация появления для чудика в диалоге
            val lottiePreview = findViewById<LottieAnimationView>(R.id.lottiePreview)
            if (lottiePreview != null && card.name == "Красный дьявол") {
                lottiePreview.visibility = View.GONE
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startPulseAnimation(view: View) {
        ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.05f, 1f).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
        ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.05f, 1f).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    private fun activateAnimation() {
        if (card.animationActivated) {
            Toast.makeText(context, "🔥 Чудик уже активирован! 🔥", Toast.LENGTH_SHORT).show()
            return
        }

        val lottiePreview = findViewById<LottieAnimationView>(R.id.lottiePreview)
        lottiePreview?.visibility = View.VISIBLE
        lottiePreview?.playAnimation()

        Toast.makeText(context, "🔥 Чудик активирован! 🔥", Toast.LENGTH_SHORT).show()
        onAnimationActivated(card)

        lottiePreview?.postDelayed({
            animateExit {
                dismiss()
            }
        }, 1500)
    }

    private fun animateStatBar(barId: Int, value: Int, maxValue: Int, colorHex: String) {
        val bar = findViewById<View>(barId) ?: return
        val maxWidth = 200
        bar.layoutParams.width = 0
        bar.requestLayout()

        val targetWidth = (value.toFloat() / maxValue.toFloat() * maxWidth).toInt()
        ObjectAnimator.ofInt(bar, "width", 0, targetWidth).apply {
            duration = 1000
            interpolator = BounceInterpolator()
            addUpdateListener {
                bar.layoutParams.width = it.animatedValue as Int
                bar.requestLayout()
            }
            start()
        }
    }

    private fun startEntranceAnimation() {
        findViewById<View>(android.R.id.content)?.apply {
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(BounceInterpolator())
                .start()
        }
    }

    private fun animateExit(onEnd: () -> Unit) {
        findViewById<View>(android.R.id.content)?.animate()
            ?.alpha(0f)
            ?.scaleX(0.8f)
            ?.scaleY(0.8f)
            ?.setDuration(300)
            ?.withEndAction { onEnd() }
            ?.start()
    }
}
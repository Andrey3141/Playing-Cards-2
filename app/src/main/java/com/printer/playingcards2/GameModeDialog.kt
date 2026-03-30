package com.printer.playingcards2

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.google.android.material.card.MaterialCardView

class GameModeDialog(context: Context) : Dialog(context) {

    private lateinit var offlineButton: MaterialCardView
    private lateinit var onlineButton: MaterialCardView
    private lateinit var cancelButton: MaterialCardView
    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var dialogCard: CardView
    private lateinit var offlineArrow: TextView
    private lateinit var onlineLock: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(R.layout.dialog_game_mode)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        initViews()
        setupButtons()
        startEntranceAnimation()

        // Устанавливаем флаг, чтобы диалог не закрывался при повороте
        setCancelable(true)
        setCanceledOnTouchOutside(true)
    }

    private fun initViews() {
        dialogCard = findViewById(R.id.dialogCard)
        offlineButton = findViewById(R.id.offlineButton)
        onlineButton = findViewById(R.id.onlineButton)
        cancelButton = findViewById(R.id.cancelButton)
        titleText = findViewById(R.id.titleText)
        subtitleText = findViewById(R.id.subtitleText)
        offlineArrow = findViewById(R.id.offlineArrow)
        onlineLock = findViewById(R.id.onlineLock)
    }

    private fun startEntranceAnimation() {
        dialogCard.alpha = 0f
        dialogCard.scaleX = 0.7f
        dialogCard.scaleY = 0.7f

        dialogCard.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .setInterpolator(AnticipateOvershootInterpolator(1.2f))
            .start()

        titleText.alpha = 0f
        titleText.translationY = -50f
        titleText.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setStartDelay(100)
            .setInterpolator(BounceInterpolator())
            .start()

        subtitleText.alpha = 0f
        subtitleText.translationY = -30f
        subtitleText.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setStartDelay(200)
            .setInterpolator(DecelerateInterpolator())
            .start()

        animateButtonWithDelay(offlineButton, 300, 300, 50f)
        animateButtonWithDelay(onlineButton, 400, 400, 100f)

        offlineArrow.alpha = 0f
        offlineArrow.translationX = -20f
        offlineArrow.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(300)
            .setStartDelay(500)
            .start()

        onlineLock.alpha = 0f
        onlineLock.scaleX = 0f
        onlineLock.scaleY = 0f
        onlineLock.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .setStartDelay(550)
            .setInterpolator(BounceInterpolator())
            .start()

        cancelButton.alpha = 0f
        cancelButton.translationY = 30f
        cancelButton.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setStartDelay(600)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun animateButtonWithDelay(button: MaterialCardView, duration: Long, delay: Long, translationY: Float) {
        button.alpha = 0f
        button.translationY = translationY
        button.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(duration)
            .setStartDelay(delay)
            .setInterpolator(BounceInterpolator())
            .start()
    }

    private fun setupButtons() {
        setupHoverAnimation(offlineButton)
        setupHoverAnimation(onlineButton)
        setupHoverAnimation(cancelButton)

        offlineButton.setOnClickListener {
            animateButtonClick(offlineButton) {
                val intent = Intent(context, GameActivity::class.java)
                context.startActivity(intent)
                dismiss()
            }
        }

        onlineButton.setOnClickListener {
            animateButtonClick(onlineButton) {
                animateShake(onlineButton) {
                    Toast.makeText(context, "🎮 Онлайн режим будет доступен в следующем обновлении!", Toast.LENGTH_LONG).show()
                    dismiss()
                }
            }
        }

        cancelButton.setOnClickListener {
            animateButtonClick(cancelButton) {
                animateExit {
                    dismiss()
                }
            }
        }
    }

    private fun setupHoverAnimation(button: MaterialCardView) {
        button.setOnHoverListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_HOVER_ENTER -> {
                    button.animate()
                        .scaleX(1.02f)
                        .scaleY(1.02f)
                        .setDuration(200)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
                android.view.MotionEvent.ACTION_HOVER_EXIT -> {
                    button.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start()
                }
            }
            false
        }
    }

    private fun animateButtonClick(button: MaterialCardView, action: () -> Unit) {
        val scaleDown = ObjectAnimator.ofPropertyValuesHolder(
            button,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 0.95f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.95f)
        )
        scaleDown.duration = 100

        val scaleUp = ObjectAnimator.ofPropertyValuesHolder(
            button,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f)
        )
        scaleUp.duration = 200
        scaleUp.interpolator = BounceInterpolator()

        scaleDown.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                scaleUp.start()
                scaleUp.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        action()
                    }
                })
            }
        })

        scaleDown.start()
    }

    private fun animateShake(button: MaterialCardView, action: () -> Unit) {
        val shakeAnim = ObjectAnimator.ofPropertyValuesHolder(
            button,
            PropertyValuesHolder.ofFloat(View.ROTATION, -5f, 5f, -3f, 3f, -1f, 1f, 0f)
        )
        shakeAnim.duration = 500
        shakeAnim.interpolator = BounceInterpolator()
        shakeAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                action()
            }
        })
        shakeAnim.start()
    }

    private fun animateExit(onEnd: () -> Unit) {
        dialogCard.animate()
            .alpha(0f)
            .scaleX(0.7f)
            .scaleY(0.7f)
            .setDuration(300)
            .withEndAction {
                onEnd()
            }
            .start()

        titleText.animate()
            .alpha(0f)
            .translationY(-30f)
            .setDuration(250)
            .start()

        subtitleText.animate()
            .alpha(0f)
            .setDuration(200)
            .start()
    }
}
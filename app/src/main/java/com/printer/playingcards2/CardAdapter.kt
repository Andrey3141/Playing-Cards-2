package com.printer.playingcards2

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView

class CardAdapter(
    private val cards: List<Card>,
    private val onItemClick: (Card) -> Unit,
    private val onItemLongClick: (Card) -> Unit,
    private val onAnimationStateChanged: (Card) -> Unit
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardImage: ShapeableImageView = itemView.findViewById(R.id.cardImage)
        private val cardName: TextView = itemView.findViewById(R.id.cardName)
        private val rarityIndicator: View = itemView.findViewById(R.id.rarityIndicator)
        private val rarityBadge: MaterialCardView = itemView.findViewById(R.id.rarityBadge)
        private val rarityIcon: TextView = itemView.findViewById(R.id.rarityIcon)
        private val lockOverlay: View = itemView.findViewById(R.id.lockOverlay)
        private val lockIcon: ImageView = itemView.findViewById(R.id.lockIcon)
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        private val lottieAnimation: com.airbnb.lottie.LottieAnimationView = itemView.findViewById(R.id.lottieAnimation)

        fun bind(
            card: Card,
            onItemClick: (Card) -> Unit,
            onItemLongClick: (Card) -> Unit,
            onAnimationStateChanged: (Card) -> Unit
        ) {
            // Устанавливаем изображение
            cardImage.setImageResource(card.photoResId)
            cardName.text = card.name

            // Устанавливаем цвет редкости
            val color = itemView.context.getColor(card.rarity.colorResId)
            rarityIndicator.setBackgroundColor(color)
            rarityBadge.setCardBackgroundColor(color)
            rarityIcon.text = getRarityIcon(card.rarity)

            // Обработка недоступных карт
            if (!card.isUnlocked) {
                lockOverlay.visibility = View.VISIBLE
                lockIcon.visibility = View.VISIBLE
            } else {
                lockOverlay.visibility = View.GONE
                lockIcon.visibility = View.GONE
            }

            // Показываем анимацию чудика (только для Красного дьявола)
            if (card.name == "Красный дьявол" && card.animationActivated) {
                lottieAnimation.visibility = View.VISIBLE
                lottieAnimation.playAnimation()
            } else {
                lottieAnimation.visibility = View.GONE
                lottieAnimation.cancelAnimation()
            }

            // Обработка нажатий
            itemView.setOnClickListener {
                if (card.isUnlocked) {
                    animateCardClick {
                        onItemClick(card)
                    }
                }
            }

            itemView.setOnLongClickListener {
                if (card.isUnlocked) {
                    animateLongClick {
                        onItemLongClick(card)
                    }
                    true
                } else {
                    false
                }
            }
        }

        private fun animateCardClick(onEnd: () -> Unit) {
            ObjectAnimator.ofPropertyValuesHolder(
                cardView,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.95f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.95f)
            ).apply {
                duration = 100
                doOnEnd {
                    ObjectAnimator.ofPropertyValuesHolder(
                        cardView,
                        PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f),
                        PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f)
                    ).apply {
                        duration = 200
                        interpolator = OvershootInterpolator()
                        doOnEnd {
                            onEnd()
                        }
                        start()
                    }
                }
                start()
            }
        }

        private fun animateLongClick(onEnd: () -> Unit) {
            ObjectAnimator.ofPropertyValuesHolder(
                cardView,
                PropertyValuesHolder.ofFloat(View.ROTATION, -3f, 3f, -2f, 2f, -1f, 1f, 0f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.98f, 1f, 0.99f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.98f, 1f, 0.99f, 1f)
            ).apply {
                duration = 400
                doOnEnd {
                    onEnd()
                }
                start()
            }
        }

        private fun getRarityIcon(rarity: Rarity): String {
            return when (rarity) {
                Rarity.COMMON -> "C"
                Rarity.RARE -> "R"
                Rarity.SUPER_RARE -> "SR"
                Rarity.EPIC -> "E"
                Rarity.MYTHIC -> "M"
                Rarity.LEGENDARY -> "L"
                Rarity.CUSTOM -> "C"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(cards[position], onItemClick, onItemLongClick, onAnimationStateChanged)
        // Убрана анимация появления карточек - они просто отображаются
    }

    override fun getItemCount() = cards.size

    fun activateCardAnimation(cardId: Int) {
        val position = cards.indexOfFirst { it.id == cardId }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }
}
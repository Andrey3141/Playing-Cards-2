package com.printer.playingcards2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView

class GameCardAdapter(
    private val cards: List<GameCard>,
    private val isPlayer: Boolean,
    private val onCardClick: (GameCard) -> Unit,
    private val allSideCards: List<GameCard> = cards
) : RecyclerView.Adapter<GameCardAdapter.GameCardViewHolder>() {

    class GameCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        private val cardImage: ShapeableImageView = itemView.findViewById(R.id.cardImage)
        private val cardName: TextView = itemView.findViewById(R.id.cardName)
        private val healthText: TextView = itemView.findViewById(R.id.healthText)
        private val attackText: TextView = itemView.findViewById(R.id.attackText)
        private val defenseText: TextView = itemView.findViewById(R.id.defenseText)
        private val healthBar: View = itemView.findViewById(R.id.healthBar)

        private val icon1: ImageView = itemView.findViewById(R.id.icon1)
        private val icon2: ImageView = itemView.findViewById(R.id.icon2)
        private val icon3: ImageView = itemView.findViewById(R.id.icon3)

        private val stunOverlay: View = itemView.findViewById(R.id.stunOverlay)
        private val stunIcon: ImageView = itemView.findViewById(R.id.stunIcon)

        fun bind(card: GameCard, isPlayer: Boolean, allSideCards: List<GameCard>, onCardClick: (GameCard) -> Unit) {
            cardImage.setImageResource(card.originalCard.photoResId)
            cardName.text = card.originalCard.name
            healthText.text = "${card.currentHealth}"
            attackText.text = "${card.currentAttack}"

            val healthPercent = card.currentHealth.toFloat() / card.originalCard.health.toFloat()
            val maxWidth = if (itemView.context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 25 else 35
            healthBar.layoutParams.width = (healthPercent * maxWidth).toInt()
            healthBar.requestLayout()

            defenseText.text = "${card.currentDefense}"

            // Отображение оглушения
            if (card.isStunned) {
                stunOverlay.visibility = View.VISIBLE
                stunIcon.visibility = View.VISIBLE
                cardView.alpha = 0.6f
            } else {
                stunOverlay.visibility = View.GONE
                stunIcon.visibility = View.GONE
                cardView.alpha = 1f
            }

            // Иконки особенностей
            val icons = CardSpecialEffect.getSpecialIcons(card, allSideCards)
            icon1.visibility = View.GONE
            icon2.visibility = View.GONE
            icon3.visibility = View.GONE

            fun showToast(description: String) {
                Toast.makeText(itemView.context, description, Toast.LENGTH_SHORT).show()
            }

            when (icons.size) {
                1 -> {
                    icon1.visibility = View.VISIBLE
                    icon1.setImageResource(icons[0].iconResId)
                    icon1.setOnClickListener { showToast(icons[0].description) }
                }
                2 -> {
                    icon1.visibility = View.VISIBLE
                    icon2.visibility = View.VISIBLE
                    icon1.setImageResource(icons[0].iconResId)
                    icon2.setImageResource(icons[1].iconResId)
                    icon1.setOnClickListener { showToast(icons[0].description) }
                    icon2.setOnClickListener { showToast(icons[1].description) }
                }
                3 -> {
                    icon1.visibility = View.VISIBLE
                    icon2.visibility = View.VISIBLE
                    icon3.visibility = View.VISIBLE
                    icon1.setImageResource(icons[0].iconResId)
                    icon2.setImageResource(icons[1].iconResId)
                    icon3.setImageResource(icons[2].iconResId)
                    icon1.setOnClickListener { showToast(icons[0].description) }
                    icon2.setOnClickListener { showToast(icons[1].description) }
                    icon3.setOnClickListener { showToast(icons[2].description) }
                }
            }

            // Логика клика:
            // - Если карта принадлежит ИГРОКУ (isPlayer = true) и она оглушена → НЕЛЬЗЯ выбрать
            // - Если карта принадлежит ВРАГУ (isPlayer = false) → МОЖНО атаковать, даже если оглушена
            itemView.setOnClickListener {
                if (isPlayer && card.isStunned) {
                    // Своя оглушенная карта — нельзя выбрать
                    Toast.makeText(itemView.context, "${card.originalCard.name} оглушен и не может атаковать!", Toast.LENGTH_SHORT).show()
                } else {
                    // Вражескую карту (даже оглушенную) или свою неоглушенную — можно кликнуть
                    onCardClick(card)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameCardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_game_card, parent, false)
        return GameCardViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameCardViewHolder, position: Int) {
        holder.bind(cards[position], isPlayer, allSideCards, onCardClick)
    }

    override fun getItemCount() = cards.size
}
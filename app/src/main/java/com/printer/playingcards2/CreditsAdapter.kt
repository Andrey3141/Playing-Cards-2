package com.printer.playingcards2

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class CreditsAdapter(
    private val credits: List<CreditItem>
) : RecyclerView.Adapter<CreditsAdapter.CreditViewHolder>() {

    class CreditViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.creditCard)
        private val icon: ImageView = itemView.findViewById(R.id.creditIcon)
        private val title: TextView = itemView.findViewById(R.id.creditTitle)
        private val description: TextView = itemView.findViewById(R.id.creditDescription)
        private val arrowIcon: ImageView = itemView.findViewById(R.id.arrowIcon)

        fun bind(credit: CreditItem) {
            icon.setImageResource(credit.iconResId)
            title.text = credit.role
            description.text = credit.name

            // Если есть ссылка, делаем кликабельным и показываем стрелку
            if (!credit.link.isNullOrEmpty()) {
                cardView.isClickable = true
                cardView.isFocusable = true
                arrowIcon.visibility = View.VISIBLE
                cardView.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(credit.link))
                    itemView.context.startActivity(intent)
                }
            } else {
                cardView.isClickable = false
                arrowIcon.visibility = View.GONE
                cardView.setOnClickListener(null)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreditViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_credit, parent, false)
        return CreditViewHolder(view)
    }

    override fun onBindViewHolder(holder: CreditViewHolder, position: Int) {
        holder.bind(credits[position])
    }

    override fun getItemCount() = credits.size
}
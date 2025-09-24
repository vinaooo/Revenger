package com.vinaooo.revenger.ui.menu

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import com.vinaooo.revenger.R

/**
 * Material You Expressive Menu Card View Sprint 1: Base component for individual menu actions
 *
 * Each menu action (Reset, Save, Load, etc.) will be represented by this card Designed to be
 * responsive and follow Material You Expressive guidelines
 */
class MenuCardView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        MaterialCardView(context, attrs, defStyleAttr) {

    private lateinit var iconImageView: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var subtitleTextView: TextView

    init {
        // Sprint 1: Basic setup
        initializeView()
        setupAttributes(attrs)
    }

    private fun initializeView() {
        // Inflate the card layout (will be created next)
        LayoutInflater.from(context).inflate(R.layout.menu_card_view, this, true)

        // Sprint 1: Basic Material Card properties
        cardElevation = resources.getDimension(R.dimen.menu_card_elevation)
        radius = resources.getDimension(R.dimen.menu_card_corner_radius)

        // Find views (will be enhanced in Sprint 3)
        iconImageView = findViewById(R.id.menu_card_icon)
        titleTextView = findViewById(R.id.menu_card_title)
        subtitleTextView = findViewById(R.id.menu_card_subtitle)
    }

    private fun setupAttributes(attrs: AttributeSet?) {
        // Sprint 1: Basic attribute handling
        // Will be expanded with custom attributes in Sprint 3
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.MenuCardView, 0, 0)

            // Set icon if provided
            val iconRes = typedArray.getResourceId(R.styleable.MenuCardView_cardIcon, 0)
            if (iconRes != 0) {
                setIcon(iconRes)
            }

            // Set title if provided
            val title = typedArray.getString(R.styleable.MenuCardView_cardTitle)
            title?.let { setTitle(it) }

            // Set subtitle if provided
            val subtitle = typedArray.getString(R.styleable.MenuCardView_cardSubtitle)
            subtitle?.let { setSubtitle(it) }

            typedArray.recycle()
        }
    }

    /** Set the icon for this menu card */
    fun setIcon(iconRes: Int) {
        iconImageView.setImageResource(iconRes)
    }

    /** Set the title text for this menu card */
    fun setTitle(title: String) {
        titleTextView.text = title
    }

    /** Set the subtitle text for this menu card */
    fun setSubtitle(subtitle: String) {
        subtitleTextView.text = subtitle
        subtitleTextView.visibility = if (subtitle.isNotEmpty()) VISIBLE else GONE
    }

    /** Enable/disable the card (Sprint 1: basic state) */
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1.0f else 0.6f
    }
}

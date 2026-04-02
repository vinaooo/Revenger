package com.vinaooo.revenger.ui.retromenu3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.R
import com.vinaooo.revenger.utils.ViewUtils
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

class CoreVariablesFragment : MenuFragmentBase() {

    private lateinit var viewModel: GameActivityViewModel

    // Containers
    private lateinit var variablesContainer: LinearLayout
    private lateinit var coreVariablesList: LinearLayout

    // Title
    private lateinit var coreVariablesTitle: TextView

    // Back button parts
    private lateinit var backVariable: RetroCardView
    private lateinit var selectionArrowBack: TextView
    private lateinit var backTitle: TextView

    // Lists to keep track of dynamic items to update their visual
    private val cardViews = mutableListOf<RetroCardView>()
    private val arrowViews = mutableListOf<TextView>()
    private val titleViews = mutableListOf<TextView>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.core_variables, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyLayoutProportions(view)

        viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]

        ViewUtils.forceZeroElevationRecursively(view)

        setupViews(view)
        setupClickListeners()

        viewModel.navigationController?.registerFragment(this, getMenuItems().size)
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    private fun setupViews(view: View) {
        variablesContainer = view.findViewById(R.id.variables_container)
        coreVariablesList = view.findViewById(R.id.core_variables_list)
        coreVariablesTitle = view.findViewById(R.id.core_variables_title)

        backVariable = view.findViewById(R.id.variable_back)
        selectionArrowBack = view.findViewById(R.id.selection_arrow_back)
        backTitle = view.findViewById(R.id.back_title)

        backVariable.setUseBackgroundColor(false)

        ViewUtils.applySelectedFontToViews(
            requireContext(),
            coreVariablesTitle,
            selectionArrowBack,
            backTitle
        )

        loadVariables()

        // Apply capitalization
        val capitalizationStyle = resources.getInteger(com.vinaooo.revenger.R.integer.rm_text_capitalization)
        val titleText = coreVariablesTitle.text.toString()
        val capitalizedTitle = when (capitalizationStyle) {
            1 -> if (titleText.isNotEmpty()) titleText.substring(0, 1).uppercase() + titleText.substring(1) else titleText
            2 -> titleText.uppercase()
            else -> titleText
        }
        if (capitalizedTitle != titleText) {
            coreVariablesTitle.text = capitalizedTitle
        }
        
        updateSelectionVisualInternal()
    }

    private fun loadVariables() {
        cardViews.clear()
        arrowViews.clear()
        titleViews.clear()
        
        // Remove prior dynamic items if any (keeping back button)
        val childCount = coreVariablesList.childCount
        // We injected them above the back button, but we should be careful.
        // Actually, the back button is at the bottom, so let's just clear cardViews list, then re-inflate.
        
        val vars = com.vinaooo.revenger.RevengerApplication.appConfig.getVariables()?.split(",") ?: emptyList()
        val inflater = LayoutInflater.from(requireContext())
        var insertIndex = 0

        for (variable in vars) {
            val trimmed = variable.trim()
            if (trimmed.isNotEmpty()) {
                val itemView = inflater.inflate(R.layout.item_core_variable, coreVariablesList, false) as RetroCardView
                itemView.setUseBackgroundColor(false)

                val arrow = itemView.findViewById<TextView>(R.id.selection_arrow)
                val title = itemView.findViewById<TextView>(R.id.item_title)

                title.text = trimmed

                ViewUtils.applySelectedFontToViews(requireContext(), arrow, title)

                coreVariablesList.addView(itemView, insertIndex++)

                cardViews.add(itemView)
                arrowViews.add(arrow)
                titleViews.add(title)
            }
        }
        
        // Back item is static
        cardViews.add(backVariable)
        arrowViews.add(selectionArrowBack)
        titleViews.add(backTitle)
    }

    private fun setupClickListeners() {
        cardViews.forEachIndexed { index, card ->
            card.setOnClickListener {
                setSelectedIndex(index)
                handleItemClick(index)
            }
        }
    }

    override fun onMenuItemSelected(item: MenuItem) {}
    override fun performNavigateUp() {
        navigateUpCircular(getMenuItems().size)
    }

    override fun performNavigateDown() {
        navigateDownCircular(getMenuItems().size)
    }

    override fun performConfirm() {
        handleItemClick(getCurrentSelectedIndex())
    }

    override fun performBack(): Boolean {
        // Return false to let NavigationEventProcessor handle the back navigation
        // Don't call navigateBack() here as it causes infinite recursion
        return false
    }

    private fun handleItemClick(index: Int) {
        // Last index is back button
        if (index == cardViews.size - 1) {
            viewModel.navigationController?.navigateBack()
        } else {
            // handle variable click
        }
    }

    override fun updateSelectionVisualInternal() {
        val selectedIndex = getCurrentSelectedIndex()
        val context = requireContext()

        titleViews.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            title.setTextColor(
                if (isSelected) androidx.core.content.ContextCompat.getColor(context, R.color.rm_selected_color)
                else androidx.core.content.ContextCompat.getColor(context, R.color.rm_normal_color)
            )
        }

        arrowViews.forEachIndexed { index, arrow ->
            val isSelected = index == selectedIndex
            arrow.visibility = if (isSelected) android.view.View.VISIBLE else android.view.View.GONE
            arrow.setTextColor(
                if (isSelected) androidx.core.content.ContextCompat.getColor(context, R.color.rm_selected_color)
                else androidx.core.content.ContextCompat.getColor(context, R.color.rm_normal_color)
            )
        }

        // Handle auto-scroll to make the selected item visible
        if (cardViews.isNotEmpty() && selectedIndex in cardViews.indices) {
            val selectedCard = cardViews[selectedIndex]
            view?.findViewById<android.widget.ScrollView>(R.id.core_variables_scroll)?.let { scrollView ->
                scrollView.post {
                    val scrollY = selectedCard.top - (scrollView.height - selectedCard.height) / 2
                    scrollView.smoothScrollTo(0, scrollY.coerceAtLeast(0))
                }
            }
        }
    }

    override fun getMenuItems(): List<MenuItem> {
        return cardViews.mapIndexed { index, view ->
            MenuItem(id = "item_$index", title = titleViews[index].text.toString(), action = com.vinaooo.revenger.ui.retromenu3.MenuAction.NONE)
        }
    }
}

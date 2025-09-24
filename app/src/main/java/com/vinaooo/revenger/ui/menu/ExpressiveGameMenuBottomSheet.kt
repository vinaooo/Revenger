package com.vinaooo.revenger.ui.menu

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vinaooo.revenger.R

/**
 * Material You Expressive Bottom Sheet Menu Sprint 2: Functional implementation with full
 * AlertDialog compatibility
 *
 * This component replaces the AlertDialog menu with a modern Material You Expressive interface
 * while maintaining all existing functionality.
 */
class ExpressiveGameMenuBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val TAG = "ExpressiveGameMenu"

        fun newInstance(): ExpressiveGameMenuBottomSheet {
            return ExpressiveGameMenuBottomSheet()
        }
    }

    // Menu action callbacks - same interface as original AlertDialog
    interface MenuActionListener {
        fun onResetClicked()
        fun onSaveStateClicked()
        fun onLoadStateClicked()
        fun onMuteClicked()
        fun onFastForwardClicked()
        fun onMenuDismissed()
    }

    private var actionListener: MenuActionListener? = null
    private lateinit var actionsRecyclerView: RecyclerView
    private lateinit var closeButton: Button

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return try {
            Log.d(TAG, "onCreateView: Inflating menu_expressive_functional layout")
            val view = inflater.inflate(R.layout.menu_expressive_functional, container, false)
            Log.d(TAG, "onCreateView: Layout inflated successfully")
            view
        } catch (e: Exception) {
            Log.e(TAG, "onCreateView: Error inflating layout", e)
            throw e
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Starting view setup")

        try {
            // Sprint 2: Setup functional components
            setupViews(view)
            setupRecyclerView()
            setupCloseButton()
            Log.d(TAG, "onViewCreated: View setup completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "onViewCreated: Error setting up views", e)
        }
    }

    /** Setup view references */
    private fun setupViews(view: View) {
        actionsRecyclerView = view.findViewById(R.id.menu_actions_recycler)
        closeButton = view.findViewById<Button>(R.id.menu_close_button)
    }

    /** Setup RecyclerView with actions */
    private fun setupRecyclerView() {
        // Create default actions matching AlertDialog functionality
        val actions = MenuActionsAdapter.createDefaultActions()

        // Setup adapter with click handling
        val adapter = MenuActionsAdapter(actions) { action -> handleActionClick(action) }

        // Setup grid layout (2 columns for compact screens)
        val spanCount = getSpanCount()
        val layoutManager = GridLayoutManager(requireContext(), spanCount)

        actionsRecyclerView.layoutManager = layoutManager
        actionsRecyclerView.adapter = adapter
    }

    /** Handle menu action clicks - same logic as original AlertDialog */
    private fun handleActionClick(action: MenuActionsAdapter.MenuAction) {
        when (action.id) {
            MenuActionsAdapter.MenuActionId.RESET -> {
                actionListener?.onResetClicked()
            }
            MenuActionsAdapter.MenuActionId.SAVE_STATE -> {
                actionListener?.onSaveStateClicked()
            }
            MenuActionsAdapter.MenuActionId.LOAD_STATE -> {
                actionListener?.onLoadStateClicked()
            }
            MenuActionsAdapter.MenuActionId.MUTE -> {
                actionListener?.onMuteClicked()
            }
            MenuActionsAdapter.MenuActionId.FAST_FORWARD -> {
                actionListener?.onFastForwardClicked()
            }
        }
        // Auto-dismiss after action (matching AlertDialog behavior)
        dismiss()
    }

    /** Setup close button */
    private fun setupCloseButton() {
        closeButton.setOnClickListener { dismiss() }
    }

    /** Get span count based on screen size Sprint 2: Basic responsive logic */
    private fun getSpanCount(): Int {
        val screenWidth = resources.displayMetrics.widthPixels
        val dp = resources.displayMetrics.density

        return when {
            screenWidth / dp >= 840 -> 5 // Expanded: horizontal layout
            screenWidth / dp >= 600 -> 3 // Medium: 3 columns
            else -> 2 // Compact: 2 columns
        }
    }

    fun setMenuActionListener(listener: MenuActionListener) {
        actionListener = listener
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        actionListener?.onMenuDismissed()
    }
}

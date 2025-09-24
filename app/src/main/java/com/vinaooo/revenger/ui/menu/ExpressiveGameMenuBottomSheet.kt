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
 * Material Design 3 Expressive Bottom Sheet Fragment for game menu Uses Material 3 color system
 * with dynamic theming support
 */
class ExpressiveGameMenuBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val TAG = "ExpressiveGameMenu"

        fun newInstance(): ExpressiveGameMenuBottomSheet {
            return ExpressiveGameMenuBottomSheet()
        }
    }

    interface MenuActionListener {
        fun onSaveState()
        fun onLoadState()
        fun onReset()
        fun onSettings()
        fun onScreenshot()
        fun onScanGamepad()
        fun onExit()
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

    // Remove the custom theme to avoid Material 3 attribute resolution issues
    // override fun getTheme(): Int {
    //     return R.style.Theme_Revenger_BottomSheetDialog
    // }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return try {
            Log.d(TAG, "onCreateView: Inflating menu_expressive_material3 layout")
            val view = inflater.inflate(R.layout.menu_expressive_material3, container, false)
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
            setupViews(view)
            setupRecyclerView()
            setupCloseButton()
            Log.d(TAG, "onViewCreated: View setup completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "onViewCreated: Error setting up views", e)
        }
    }

    private fun setupViews(view: View) {
        actionsRecyclerView = view.findViewById(R.id.menu_actions_recycler)
        closeButton = view.findViewById<Button>(R.id.menu_close_button)
    }

    private fun setupRecyclerView() {
        val actions = MenuActionsAdapter.createDefaultActions()
        val adapter = MenuActionsAdapter(actions) { action -> handleActionClick(action) }

        val spanCount = getSpanCount()
        val layoutManager = GridLayoutManager(requireContext(), spanCount)

        actionsRecyclerView.layoutManager = layoutManager
        actionsRecyclerView.adapter = adapter
    }

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
        dismiss()
    }

    private fun setupCloseButton() {
        closeButton.setOnClickListener { dismiss() }
    }

    private fun getSpanCount(): Int {
        val screenWidth = resources.displayMetrics.widthPixels
        val dp = resources.displayMetrics.density
        return when {
            screenWidth / dp >= 840 -> 5
            screenWidth / dp >= 600 -> 3
            else -> 2
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

package com.vinaooo.revenger.ui.menu

import android.os.Bundle
import androidx.fragment.app.DialogFragment

/**
 * Placeholder stub for GameMenuBottomSheet. The full BottomSheet implementation was removed; keep a
 * minimal stub to avoid breaking references during cleanup. This class intentionally avoids any
 * BottomSheet-specific APIs or theme references.
 */
class GameMenuBottomSheet : DialogFragment(), MenuItemClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No-op: BottomSheet behaviour removed. Keep class for compatibility.
    }

    override fun onMenuItemClick(item: GameMenuItem) {
        // No-op implementation. Real handling is performed by FloatingGameMenu.
    }

    companion object {
        const val TAG = "GameMenuBottomSheet"
    }
}

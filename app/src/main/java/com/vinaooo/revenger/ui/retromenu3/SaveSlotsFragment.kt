package com.vinaooo.revenger.ui.retromenu3

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.caverock.androidsvg.SVG
import com.vinaooo.revenger.R
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.models.SaveSlotData
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.utils.ViewUtils
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

class SaveSlotsFragment : Fragment() {

    private lateinit var viewModel: GameActivityViewModel
    private lateinit var saveStateManager: SaveStateManager

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_save_slots, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]
        saveStateManager = SaveStateManager.getInstance(requireContext())

        val grid = view.findViewById<GridLayout>(R.id.grid_slots)
        grid.removeAllViews()

        // Apply retro font to fragment title
        val titleView = view.findViewById<TextView>(R.id.save_slots_title)
        titleView?.let { ViewUtils.applySelectedFontToViews(requireContext(), it) }

        // Populate slots from manager
        for (i in 1..9) {
            val slot = saveStateManager.getSlot(i) ?: SaveSlotData.empty(i)
            val itemView = layoutInflater.inflate(R.layout.item_save_slot, grid, false)

            val img = itemView.findViewById<ImageView>(R.id.iv_slot_image)
            val name = itemView.findViewById<TextView>(R.id.tv_slot_name)
            val ts = itemView.findViewById<TextView>(R.id.tv_slot_ts)
            val normalContainer = itemView.findViewById<View>(R.id.normal_container)
            val emptyContainer = itemView.findViewById<View>(R.id.empty_container)
            val emptyIcon = itemView.findViewById<ImageView>(R.id.iv_empty_icon)
            val emptyLabel = itemView.findViewById<TextView>(R.id.tv_empty_label)

            // Apply retro font to slot texts
            ViewUtils.applySelectedFontToViews(requireContext(), name, ts)

            if (slot.screenshotFile != null && slot.screenshotFile.exists()) {
                // show normal content
                normalContainer.visibility = View.VISIBLE
                emptyContainer.visibility = View.GONE
                val bmp = BitmapFactory.decodeFile(slot.screenshotFile.absolutePath)
                img.setImageBitmap(bmp)
                name.text = slot.getDisplayName()
                ts.text = slot.getFormattedTimestamp()
            } else {
                // show empty centered icon + text inside slot
                normalContainer.visibility = View.GONE
                emptyContainer.visibility = View.VISIBLE
                // Try to render high-quality SVG from assets to avoid blurry upscaling of the PNG
                try {
                    val input = requireContext().assets.open("videogame_asset_120.svg")
                    val svg = SVG.getFromInputStream(input)
                    val sizePx = resources.getDimensionPixelSize(R.dimen.rm_slot_empty_icon_size)
                    var bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    svg.setDocumentWidth(sizePx.toFloat())
                    svg.setDocumentHeight(sizePx.toFloat())
                    svg.renderToCanvas(canvas)
                    // crop transparent padding from bitmap to reduce whitespace around icon
                    val cropped = cropTransparent(bmp)
                    if (cropped != bmp) {
                        bmp.recycle()
                        bmp = cropped
                    }
                    emptyIcon.setImageBitmap(bmp)
                    emptyIcon.imageTintList = ColorStateList.valueOf(Color.WHITE)
                } catch (e: Exception) {
                    emptyIcon.setImageResource(R.drawable.videogame_asset_24)
                    emptyIcon.imageTintList = ColorStateList.valueOf(Color.WHITE)
                }
                emptyLabel.text = "empty"
                ViewUtils.applySelectedFontToViews(requireContext(), emptyLabel)
            }

            // Ensure RetroCardView visuals match RetroMenu3
            (itemView as? RetroCardView)?.apply {
                setUseBackgroundColor(true)
                setState(RetroCardView.State.NORMAL)
            }

            itemView.setOnClickListener { onSlotClicked(slot) }

            val params = GridLayout.LayoutParams()
            val w = resources.getDimensionPixelSize(R.dimen.rm_slot_item_width)
            params.width = w
            params.height = LayoutParams.WRAP_CONTENT
            params.setGravity(Gravity.CENTER)
            // place item in correct column/row for consistent centering
            val index = i - 1
            params.columnSpec = GridLayout.spec(index % 3)
            params.rowSpec = GridLayout.spec(index / 3)
            grid.addView(itemView, params)
        }
    }

    private fun cropTransparent(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        var top = -1
        var left = -1
        var right = -1
        var bottom = -1

        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val alpha = (pixels[y * w + x] ushr 24) and 0xff
                if (alpha > 8) {
                    top = if (top == -1) y else top
                    bottom = y
                    left = if (left == -1) x else Math.min(left, x)
                    right = if (right == -1) x else Math.max(right, x)
                }
            }
        }

        if (top == -1 || left == -1) return src

        val rect = Rect(left, top, right + 1, bottom + 1)
        return Bitmap.createBitmap(src, rect.left, rect.top, rect.width(), rect.height())
    }

    private fun onSlotClicked(slot: SaveSlotData) {
        if (slot.isEmpty) {
            showNamingDialog(slot.slotNumber)
        } else {
            showOverwriteConfirmation(slot)
        }
    }

    private fun showNamingDialog(slotNumber: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_retro_naming, null)
        val input = dialogView.findViewById<EditText>(R.id.dialog_input)
        val title = dialogView.findViewById<TextView>(R.id.dialog_title)
        val ok = dialogView.findViewById<Button>(R.id.dialog_ok)
        val cancel = dialogView.findViewById<Button>(R.id.dialog_cancel)

        input.setText("Slot $slotNumber")
        input.selectAll()

        ViewUtils.applySelectedFontToViews(requireContext(), title, input)
        FontUtils.applyTextCapitalization(requireContext(), title)

        val dlg = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        ok.setOnClickListener {
            val name = input.text.toString().ifBlank { "Slot $slotNumber" }
            dlg.dismiss()
            performSave(slotNumber, name)
        }

        cancel.setOnClickListener { dlg.dismiss() }

        dlg.show()
    }

    private fun showOverwriteConfirmation(slot: SaveSlotData) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_retro_confirm, null)
        val msg = dialogView.findViewById<TextView>(R.id.dialog_message)
        val ok = dialogView.findViewById<Button>(R.id.dialog_confirm)
        val cancel = dialogView.findViewById<Button>(R.id.dialog_cancel)

        msg.text = "Deseja substituir o save '${slot.name}'?"
        ViewUtils.applySelectedFontToViews(requireContext(), msg)

        val dlg = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        ok.setOnClickListener {
            dlg.dismiss()
            showNamingDialog(slot.slotNumber)
        }

        cancel.setOnClickListener { dlg.dismiss() }

        dlg.show()
    }

    private fun performSave(slotNumber: Int, name: String) {
        val retroView = viewModel.retroView
        if (retroView == null) {
            android.util.Log.e("SaveSlotsFragment", "RetroView is null, cannot save")
            Toast.makeText(requireContext(), "Cannot save: no retro view", Toast.LENGTH_SHORT)
                    .show()
            return
        }

        try {
            val stateBytes = retroView.view.serializeState()
            val screenshot = viewModel.getCachedScreenshot()
            val romName = resources.getString(R.string.conf_rom)

            val success =
                    saveStateManager.saveToSlot(slotNumber, stateBytes, screenshot, name, romName)

            if (success) {
                android.util.Log.d("SaveSlotsFragment", "Save successful to slot $slotNumber")
                // Pop this fragment and refresh parent ProgressFragment
                parentFragmentManager.popBackStack()
                // If parent is ProgressFragment, trigger refresh
                val parent = parentFragment
                try {
                    (parent as? ProgressFragment)?.refreshMenuItems()
                } catch (e: Exception) {
                    // ignore
                }
            } else {
                android.util.Log.e("SaveSlotsFragment", "Save failed to slot $slotNumber")
                Toast.makeText(requireContext(), "Save failed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("SaveSlotsFragment", "Error saving state", e)
            Toast.makeText(requireContext(), "Error saving: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
        }
    }

    companion object {
        fun newInstance(): SaveSlotsFragment = SaveSlotsFragment()
    }
}

package com.vinaooo.revenger.models

import android.graphics.Bitmap

/**
 * Groups the data written by
 * [com.vinaooo.revenger.managers.SaveStateManager.saveToSlot]: the serialized state plus its
 * optional screenshot/preview images and display metadata.
 *
 * Kept as a single parameter object instead of five separate parameters so `saveToSlot` stays
 * under the project's `LongParameterList` threshold.
 *
 * @property stateBytes Serialized state data from RetroView
 * @property screenshot Bitmap of the game screen (will be saved as WebP)
 * @property preview Full-screen bitmap with black bars for load preview overlay (optional)
 * @property name User-defined name (defaults to "Slot X")
 * @property romName Name of the current ROM
 */
@Suppress("ArrayInDataClass")
data class SaveSlotPayload(
        val stateBytes: ByteArray,
        val screenshot: Bitmap? = null,
        val preview: Bitmap? = null,
        val name: String? = null,
        val romName: String = ""
)

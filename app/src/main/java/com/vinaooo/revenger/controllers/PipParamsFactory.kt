package com.vinaooo.revenger.controllers

import android.annotation.TargetApi
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.FrameLayout
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.R
import com.vinaooo.revenger.repositories.PipConfigRepository
import com.vinaooo.revenger.utils.PipAspectRatioResolver

/**
 * Builds the [PictureInPictureParams.Builder] used both to enter PiP and to keep its params
 * current (aspect ratio, source-rect hint, and the Quick Save / Save and Exit [RemoteAction]s).
 * Extracted out of [PipController] to keep that class within detekt's `TooManyFunctions`
 * threshold.
 */
class PipParamsFactory(
        private val host: PipHost,
        private val retroviewContainer: FrameLayout,
        private val appConfig: AppConfig
) {
        @TargetApi(Build.VERSION_CODES.O)
        fun newBuilder(): PictureInPictureParams.Builder {
                val activity = host.activity
                val builder = PictureInPictureParams.Builder()

                // Otimizar a janela PIP para usar a exata proporção da plataforma
                val platformId = appConfig.getPlatformId()
                val pipProfile = PipConfigRepository.getProfile(platformId)

                val resolvedRatio = PipAspectRatioResolver.resolve(
                        pipProfile.ratioW, pipProfile.ratioH, retroviewContainer.width, retroviewContainer.height
                )
                if (resolvedRatio != null) {
                        builder.setAspectRatio(resolvedRatio)
                }

                // Source rect hint: where the PiP window animates from/to. Without it the
                // enter/exit animation cross-fades from a wrong rectangle and reads as a black flash.
                val sourceRect = Rect()
                if (retroviewContainer.getGlobalVisibleRect(sourceRect) && !sourceRect.isEmpty) {
                        builder.setSourceRectHint(sourceRect)
                }

                // Adicionar botões (RemoteActions) ao PiP
                val quickSaveIntent = PendingIntent.getBroadcast(
                        activity, 0, Intent(PipController.ACTION_PIP_QUICK_SAVE).apply { setPackage(activity.packageName) },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val quickSaveAction = RemoteAction(
                        Icon.createWithResource(activity, R.drawable.ic_save_24),
                        "Quick Save",
                        "Quick Save",
                        quickSaveIntent
                )

                val saveIntent = PendingIntent.getBroadcast(
                        activity, 1, Intent(PipController.ACTION_PIP_SAVE).apply { setPackage(activity.packageName) },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val saveAction = RemoteAction(
                        Icon.createWithResource(activity, R.drawable.ic_empty_slot),
                        "Save and Exit",
                        "Save and Exit",
                        saveIntent
                )

                builder.setActions(listOf(quickSaveAction, saveAction))
                return builder
        }
}

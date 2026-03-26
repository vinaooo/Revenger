        private fun updatePictureInPictureParams() {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        // DO NOT update bounds while already in PIP mode, it messes up the view
                        if (isInPictureInPictureMode) return

                        val builder = android.app.PictureInPictureParams.Builder()
                                .setAspectRatio(getGameAspectRatio())
                                .setAutoEnterEnabled(true)
                                
                        getGameBounds()?.let { builder.setSourceRectHint(it) }
                        
                        setPictureInPictureParams(builder.build())
                }
        }

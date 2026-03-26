        private fun getGameBounds(): android.graphics.Rect? {
                val retroView = viewModel.retroView?.view ?: return null
                if (retroView.width == 0 || retroView.height == 0) return null
                val rect = android.graphics.Rect()
                val location = IntArray(2)
                retroView.getLocationInWindow(location)
                rect.set(location[0], location[1], location[0] + retroView.width, location[1] + retroView.height)
                return if (rect.isEmpty) null else rect
        }

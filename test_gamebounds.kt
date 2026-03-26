        private fun enterPiPMode() {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        // DO NOT set source rect hint before entering PIP because we might 
                        // have incorrect layout? No, before PIP is fine.

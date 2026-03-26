package test
import android.app.Activity
import android.content.res.Configuration

class TestActivity : Activity() {
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration?) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }
}

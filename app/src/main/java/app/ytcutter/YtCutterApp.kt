package app.ytcutter

import android.app.Application
import app.ytcutter.core.data.PoToken

class YtCutterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        PoToken.warmUp(this)
    }
}
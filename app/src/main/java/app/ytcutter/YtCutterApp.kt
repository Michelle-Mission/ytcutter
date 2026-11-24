package app.ytcutter

import android.app.Application

class YtCutterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}
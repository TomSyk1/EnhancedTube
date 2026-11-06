package com.tomsyk1.enhancedtube

import android.app.Application
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe

class EnhancedTubeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NewPipe.init(AppDownloader(OkHttpClient()))
        Settings.init(this)
        History.init(this)
    }
}

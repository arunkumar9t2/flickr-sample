package `in`.arunkumarsampath.flickerapp

import `in`.arunkumarsampath.flickerapp.di.DependencyInjector
import android.app.Application

class FlickerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        DependencyInjector.setup(application = this)
    }
}
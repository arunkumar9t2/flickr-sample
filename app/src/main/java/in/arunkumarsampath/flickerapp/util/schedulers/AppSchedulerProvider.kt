package `in`.arunkumarsampath.flickerapp.util.schedulers

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class AppSchedulerProvider : SchedulerProvider {
    override val io get() = Schedulers.io()
    override val pool get() = Schedulers.computation()
    override val ui get() = AndroidSchedulers.mainThread()
}
package `in`.arunkumarsampath.flickerapp.util.schedulers

import io.reactivex.schedulers.Schedulers

class TestSchedulerProvider : SchedulerProvider {

    override val io get() = Schedulers.trampoline()
    override val pool get() = Schedulers.trampoline()
    override val ui get() = Schedulers.trampoline()
}
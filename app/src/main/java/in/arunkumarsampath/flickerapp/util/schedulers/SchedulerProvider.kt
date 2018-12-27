package `in`.arunkumarsampath.flickerapp.util.schedulers

import io.reactivex.Scheduler

interface SchedulerProvider {
    val io: Scheduler
    val pool: Scheduler
    val ui: Scheduler
}
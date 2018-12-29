package `in`.arunkumarsampath.flickerapp.util

import io.reactivex.FlowableTransformer

/**
 * Sealed type to denote Loading content and error easily in Rx stream.
 */
sealed class Result<T> {
    data class Success<T>(val data: T) : Result<T>()
    class Loading<T> : Result<T>()
    data class Failure<T>(val throwable: Throwable) : Result<T>()

    companion object {

        fun <T> applyToFlowable(): FlowableTransformer<T, Result<T>> {
            return FlowableTransformer { upstream ->
                upstream.map { Result.Success(it) as Result<T> }
                    .onErrorReturn { Result.Failure(it) }
                    .startWith(Result.Loading())
            }
        }
    }
}
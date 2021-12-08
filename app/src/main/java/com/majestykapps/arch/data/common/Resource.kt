package com.majestykapps.arch.data.common

/**
 * A generic class that holds a value with its loading status.
 *
 * @param <T>
</T> */
sealed class Resource<out T> {

    class Success<T>(val data: T) : Resource<T>()
    class Failure<T>(val error: Throwable) : Resource<T>()
    class Loading<T> : Resource<T>()

}
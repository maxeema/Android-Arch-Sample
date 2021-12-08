package com.majestykapps.arch.util

class NetworkUtils {
    companion object {
        private val netErrorTypes = arrayOf<Class<out Throwable>>(
            retrofit2.HttpException::class.java,
            java.net.UnknownHostException::class.java,
            java.net.ConnectException::class.java,
            java.net.SocketTimeoutException::class.java
        )

        @JvmStatic
        fun isConnectionError(error: Any?): Boolean = netErrorTypes.contains(error?.javaClass)
    }
}
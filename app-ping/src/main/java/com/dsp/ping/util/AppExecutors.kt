package com.dsp.ping.util

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object AppExecutors {

    private val diskIo: ExecutorService = Executors.newSingleThreadExecutor()

    fun diskIo(): ExecutorService = diskIo
}

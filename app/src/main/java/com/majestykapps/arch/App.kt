package com.majestykapps.arch

import android.app.Application
import android.os.StrictMode
import com.majestykapps.arch.data.repository.TasksRepositoryImpl
import com.majestykapps.arch.data.source.local.TasksDao
import com.majestykapps.arch.data.source.local.TasksLocalDataSource
import com.majestykapps.arch.data.source.local.ToDoDatabase
import com.majestykapps.arch.domain.repository.TasksRepository
import timber.log.Timber

class App : Application() {

    private lateinit var tasksDao: TasksDao
    private lateinit var localDataSource: TasksLocalDataSource
    lateinit var tasksRepository: TasksRepository

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            initStrictMode()
        }

        tasksDao = ToDoDatabase.getInstance(applicationContext).taskDao()
        localDataSource = TasksLocalDataSource.getInstance(tasksDao)
        tasksRepository = TasksRepositoryImpl.getInstance(localDataSource)
    }


    private fun initStrictMode() {
        val threadPolicy = StrictMode.ThreadPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build()
        StrictMode.setThreadPolicy(threadPolicy)

        val vmPolicy = StrictMode.VmPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build()
        StrictMode.setVmPolicy(vmPolicy)
    }
}
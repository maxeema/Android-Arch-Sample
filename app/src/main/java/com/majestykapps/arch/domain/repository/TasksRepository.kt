package com.majestykapps.arch.domain.repository

import com.majestykapps.arch.common.Repository
import com.majestykapps.arch.data.common.Resource
import com.majestykapps.arch.domain.entity.Task
import io.reactivex.Observable

interface TasksRepository : Repository {
    /**
     * Returns an [Observable] that will emit the resource when the entire Tasks change
     */
    fun loadTasks() : Observable<Resource<List<Task>>>

    /**
     * Returns an [Observable] that will emit the resource when the specified Task changes
     */
    fun getTask(id: String): Observable<Resource<Task>>
}
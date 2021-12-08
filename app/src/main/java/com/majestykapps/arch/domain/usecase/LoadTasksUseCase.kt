package com.majestykapps.arch.domain.usecase

import com.majestykapps.arch.data.common.Resource
import com.majestykapps.arch.domain.entity.Task
import com.majestykapps.arch.domain.repository.TasksRepository
import io.reactivex.Observable

/**
 * Due to the simplicity of this example, this use case is somewhat redundant. Typically you'd
 * combine data from multiple repositories and transform them for the View Model in a Use Case
 */
interface LoadTasksUseCase {
    fun loadAll(): Observable<Resource<List<Task>>>

    fun refresh()
}

class LoadTasks(private val repository: TasksRepository) : LoadTasksUseCase {

    override fun loadAll() = repository.loadTasks()

    override fun refresh() = repository.refresh()

}
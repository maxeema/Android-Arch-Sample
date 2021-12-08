package com.majestykapps.arch.data.repository

import androidx.annotation.VisibleForTesting
import com.majestykapps.arch.common.SchedulerProvider
import com.majestykapps.arch.common.ToDoSchedulerProvider
import com.majestykapps.arch.data.common.Resource
import com.majestykapps.arch.data.source.TasksDataSource
import com.majestykapps.arch.data.source.remote.TasksRemoteDataSource
import com.majestykapps.arch.domain.entity.Task
import com.majestykapps.arch.domain.repository.TasksRepository
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

/**
 * Concrete implementation to load tasks from the data sources into a cache.
 *
 * For simplicity, this implements a dumb synchronisation between locally persisted data and data
 * obtained from the server, by using the remote data source only if the local database doesn't
 * exist or is empty.
 */
class TasksRepositoryImpl private constructor(
    private val tasksLocalDataSource: TasksDataSource,
    private val tasksRemoteDataSource: TasksDataSource,
    private val schedulerProvider: SchedulerProvider
) : TasksRepository {

    @VisibleForTesting
    private val tasksSubject: Subject<Resource<List<Task>>> = BehaviorSubject.create()

    /**
     * Map of cached tasks using their id as the key
     */
    @VisibleForTesting
    private val _cachedTasks: LinkedHashMap<String, Task> = LinkedHashMap()

    /**
     * When true indicates cached data should not be used
     */
    @VisibleForTesting
    private var _isCacheDirty = false
    val isCacheDirty : Boolean
        get() = _isCacheDirty

    override fun loadTasks() = tasksSubject.apply {
        Timber.tag(TAG).i("loadTasks: _isCacheDirty = $_isCacheDirty")
        // First check to see if there are cached tasks
        if (!_isCacheDirty && _cachedTasks.isNotEmpty()) {
            this.onNext(Resource.Success(ArrayList(_cachedTasks.values)))
            return@apply
        }

        if (_isCacheDirty) {
            remoteObservable
                .subscribeOn(schedulerProvider.io)
                .observeOn(schedulerProvider.main)
                .subscribe()
        }

        if (!localObservableSubscribed) {
            localObservable
                .subscribeOn(schedulerProvider.io)
                .observeOn(schedulerProvider.main)
                .subscribe(this)
            localObservableSubscribed = true;
        }
    }

    override fun getTask(id: String): Observable<Resource<Task>> {
        Timber.tag(TAG).i("getTask: id = $id, _isCacheDirty = $_isCacheDirty")
        //
        if (!_isCacheDirty && _cachedTasks.containsKey(id)) {
            return Observable.just(Resource.Success(_cachedTasks[id]!!))
        }
        val observable = if (_isCacheDirty) {
            tasksRemoteDataSource.getTask(id)
                .onErrorReturn { throwable: Throwable ->
                    if (_cachedTasks.containsKey(id))
                        Resource.Success(_cachedTasks[id]!!)
                    else
                        Resource.Failure(throwable)
                }
        } else {
            tasksLocalDataSource.getTask(id)
                .onErrorResumeNext(tasksRemoteDataSource.getTask(id))
        }

        return observable
            .subscribeOn(schedulerProvider.io)
            .observeOn(schedulerProvider.main)
    }

    override fun refresh() {
        Timber.tag(TAG).i("refresh() called")
        _isCacheDirty = true
        loadTasks()
    }

    private val remoteObservable : Observable<Resource<List<Task>>> by lazy {
        tasksRemoteDataSource.getTasks()
            .doOnNext { resource ->
                Timber.tag(TAG).d("getAndCacheRemoteTasks: emitted $resource")
                when (resource) {
                    is Resource.Success ->
                        resource.data.let { tasks ->
                            cache(tasks)
                            saveToDb(tasks)
                            _isCacheDirty = false
                        }
                    is Resource.Failure ->
                        tasksSubject.onNext(Resource.Failure(resource.error))
                    else -> {
                        // ignored
                    }
                }
            }
    }

    private var localObservableSubscribed = false;
    private val localObservable : Observable<Resource<List<Task>>> by lazy {
        tasksLocalDataSource.getTasks()
            .doOnNext { resource ->
                Timber.tag(TAG).d("getAndCacheLocalTasks: emitted $resource")
                when (resource ) {
                    is Resource.Success -> {
                        cache(resource.data)
                    }
                    else -> {
                        // ignored
                    }
                }
            }
    }

    private fun cache(tasks: List<Task>?) {
        Timber.tag(TAG).d("cache: $tasks")
        tasks?.apply {
            _cachedTasks.clear()
            forEach { cache(it) }
        }
    }

    private fun cache(task: Task?) {
        Timber.tag(TAG).d("cache: $task")
        task?.id?.let { id ->
            _cachedTasks[id] = task
        }
    }

    private fun saveToDb(tasks: List<Task>?) {
        Timber.tag(TAG).d("saveToDb: $tasks")
        tasks?.let {
            tasksLocalDataSource.saveTasks(it)
                .subscribeOn(schedulerProvider.io)
                .observeOn(schedulerProvider.main)
                .subscribe()
        }
    }

    companion object {
        private const val TAG = "TasksRepository"

        private var INSTANCE: TasksRepositoryImpl? = null

        fun getInstance(
            tasksLocalDataSource: TasksDataSource,
            tasksRemoteDataSource: TasksDataSource = TasksRemoteDataSource.getInstance(),
            schedulerProvider: SchedulerProvider = ToDoSchedulerProvider()
        ): TasksRepositoryImpl = INSTANCE ?: synchronized(this) {
            INSTANCE ?: TasksRepositoryImpl(
                tasksLocalDataSource,
                tasksRemoteDataSource,
                schedulerProvider
            ).also { INSTANCE = it }
        }

        @VisibleForTesting
        fun destroy() {
            INSTANCE = null
        }
    }
}
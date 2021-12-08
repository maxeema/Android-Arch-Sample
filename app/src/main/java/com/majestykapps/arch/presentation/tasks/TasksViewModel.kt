package com.majestykapps.arch.presentation.tasks

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.majestykapps.arch.common.SchedulerProvider
import com.majestykapps.arch.common.ToDoSchedulerProvider
import com.majestykapps.arch.data.common.Resource
import com.majestykapps.arch.data.common.Resource.*
import com.majestykapps.arch.domain.entity.Task
import com.majestykapps.arch.domain.usecase.LoadTasksUseCase
import com.majestykapps.arch.presentation.common.BaseViewModel
import com.majestykapps.arch.presentation.common.SingleArgViewModelFactory
import com.majestykapps.arch.util.SingleLiveEvent
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.util.concurrent.TimeUnit

class TasksViewModel(
    private val params: Params
) : BaseViewModel() {

    companion object {
        private const val TAG = "TasksViewModel"

        val FACTORY = SingleArgViewModelFactory.create(::TasksViewModel)

        class Params(
            val getAllTasksUseCase: LoadTasksUseCase,
            val schedulerProvider: SchedulerProvider = ToDoSchedulerProvider()
        )

        const val firstDataDelay = 500L
    }

    // Here we keep last Success data
    val tasksSource: Subject<List<Task>>

    private lateinit var _areTasksLoaded: LiveData<Boolean>
    val areTasksLoaded: LiveData<Boolean> by lazy { _areTasksLoaded }

    private val _tasksLiveData = MutableLiveData<List<Task>>()
    val tasksLiveData: LiveData<List<Task>> get() = _tasksLiveData

    val hasDataToShow = MediatorLiveData<Boolean>().apply {
        value = true
        addSource(tasksLiveData) {
            value = !it.isNullOrEmpty()
        }
    }

    private val _state = MutableLiveData<Resource<List<Task>>>()
    val state: LiveData<Resource<List<Task>>> = _state

    val isLoading = MediatorLiveData<Boolean>().apply {
        value = true
        addSource(state) {
            value = it is Loading
        }
    }
    val isSuccess = MediatorLiveData<Boolean>().apply {
        value = false
        addSource(state) {
            value = it is Success
        }
    }
    val isFailure = MediatorLiveData<Boolean>().apply {
        value = false
        addSource(state) {
            value = it is Failure
        }
    }

    val _errorEvent = MediatorLiveData<Throwable>().apply {
        addSource(state) {
            if (it is Failure)
                this.value = it.error
        }
    }
    val errorEvent: LiveData<Throwable> = _errorEvent

    val launchTaskEvent = SingleLiveEvent<String>()

    init {
        _state.value = Loading()

        val all = params.getAllTasksUseCase
            .loadAll()
            .delay(500, TimeUnit.MILLISECONDS)
            .observeOn(params.schedulerProvider.main)
            .subscribeOn(params.schedulerProvider.io)
        //
        tasksSource = BehaviorSubject.createDefault(emptyList())

        all
            .doOnNext {
                if (it is Success) {
                    tasksSource.onNext(it.data)
                }
            }
            .subscribe().addTo(disposables)
        //
        _areTasksLoaded = MediatorLiveData<Boolean>().apply {
            this.value = false
            tasksSource.doOnNext {
                this.postValue(it.isNotEmpty())
            }.subscribe().addTo(disposables)
        }
        //
        tasksSource
            .subscribe(_tasksLiveData::postValue)
            .addTo(disposables)

        all
            .subscribe(_state::setValue)
            .addTo(disposables)

    }

    fun onTaskClick(id: String) {
        launchTaskEvent.value = id
    }

    fun refresh() {
        _state.value = Loading()
        params.getAllTasksUseCase.refresh()
    }

}
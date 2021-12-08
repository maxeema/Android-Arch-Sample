package com.majestykapps.arch.presentation.tasks

import androidx.lifecycle.*
import com.majestykapps.arch.common.SchedulerProvider
import com.majestykapps.arch.common.ToDoSchedulerProvider
import com.majestykapps.arch.data.common.Resource.*
import com.majestykapps.arch.domain.entity.Task
import com.majestykapps.arch.presentation.common.BaseViewModel
import com.majestykapps.arch.presentation.common.SingleArgViewModelFactory
import com.majestykapps.arch.util.SingleLiveEvent
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class TasksSearchViewModel(
    private val params: Params
) : BaseViewModel() {

    companion object {
        private const val TAG = "TasksViewModel"

        val FACTORY = SingleArgViewModelFactory.create(::TasksSearchViewModel)

        class Params(
            val source: Observable<List<Task>>,
            val tasks: MutableLiveData<List<Task>>,
            val schedulerProvider: SchedulerProvider = ToDoSchedulerProvider(),
        )
    }

    private val querySubject = BehaviorSubject.createDefault("")

    private val _queryLiveData = MutableLiveData("")
    val queryLiveData: LiveData<String> = _queryLiveData

    val hasQuery = MediatorLiveData<Boolean>().apply {
        value = false
        addSource(queryLiveData) {
            value = it.isNotEmpty()
        }
    }

    val searchViewIconifiedState = MutableLiveData(true)
    val openSearchViewEvent = SingleLiveEvent<Unit>()

    init {
        querySubject
            .distinctUntilChanged()
            .skip(1)
            .debounce(300, TimeUnit.MILLISECONDS)
            .observeOn(params.schedulerProvider.main)
            .map(::filterSource)
            .subscribe()
            .addTo(disposables)
    }

    fun filter(query: String) {
        querySubject.onNext(query)
    }

    private fun filterSource(query: String) {
        Timber.tag(TAG).d("Search query: $query")

        val trimQuery = query.trim()

        _queryLiveData.postValue(trimQuery)

        params.source
            .map {
                it.filter { task ->
                    Timber.tag(TAG).d("Search query, filter: $task")
                    if (trimQuery.isEmpty()) return@filter true
                    task.title.contains(trimQuery, true)
                            || task.description.contains(trimQuery, true)
                }
            }
            .subscribe(params.tasks::postValue)
            .addTo(disposables)
    }

}
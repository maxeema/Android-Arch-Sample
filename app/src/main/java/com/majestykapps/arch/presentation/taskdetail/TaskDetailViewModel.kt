package com.majestykapps.arch.presentation.taskdetail

import androidx.lifecycle.*
import com.majestykapps.arch.data.common.Resource
import com.majestykapps.arch.data.common.Resource.*
import com.majestykapps.arch.domain.entity.Task
import com.majestykapps.arch.domain.usecase.GetTaskUseCase
import com.majestykapps.arch.presentation.common.BaseViewModel
import com.majestykapps.arch.presentation.common.SingleArgViewModelFactory
import kotlinx.coroutines.launch
import timber.log.Timber

class TaskDetailViewModel(
    private val params: Params
) : BaseViewModel() {

    companion object {
        private const val TAG = "TaskDetailViewModel"

        val FACTORY = SingleArgViewModelFactory.create(::TaskDetailViewModel)

        class Params(
            val getTaskUseCase: GetTaskUseCase
        )
    }

    val loadingEvent = MutableLiveData<Boolean>()
    val errorEvent = MutableLiveData<Throwable>()

    private val task = MutableLiveData<Task>()

    val title: LiveData<String> = Transformations.switchMap(task) {
        MutableLiveData(it.title)
    }
    val description: LiveData<String> = Transformations.switchMap(task) {
        MutableLiveData(it.description)
    }

    private val _resource = MutableLiveData<Resource<Task>>()
    val isSuccess = _resource.map { it is Success }

    fun getTask(id: String, forceReload: Boolean = false) {

        val disposable = params.getTaskUseCase.getTask(id)
            .subscribe({ next ->
                viewModelScope.launch {
                    Timber.tag(TAG).d("onNext: resource = $next")

                    _resource.value = next
                    loadingEvent.value = next is Loading

                    when (next) {
                        is Failure -> {
                            errorEvent.value = next.error
                        }
                        is Success -> {
                            task.value = next.data
                        }
                        is Loading -> {
                            // ignored
                        }
                    }

                }
            }, { throwable ->
                loadingEvent.value = false
                viewModelScope.launch {
                    errorEvent.value = throwable
                    _resource.value = Failure(throwable)
                }
            })
        disposables.add(disposable)
    }

}
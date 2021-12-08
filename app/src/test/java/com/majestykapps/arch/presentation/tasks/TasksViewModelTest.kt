package com.majestykapps.arch.presentation.tasks

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.majestykapps.arch.common.SchedulerProvider
import com.majestykapps.arch.data.common.Resource
import com.majestykapps.arch.domain.entity.Task
import com.majestykapps.arch.domain.usecase.LoadTasksUseCase
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Observable
import io.reactivex.internal.schedulers.SingleScheduler
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import org.hamcrest.CoreMatchers.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.random.nextUInt

class TasksViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    lateinit var useCase: LoadTasksUseCase

    lateinit var viewModel: TasksViewModel

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        val loadAllSubject : Observable<Resource<List<Task>>> = BehaviorSubject.create()
        whenever(useCase.loadAll()).thenReturn(loadAllSubject)

        val schedulerProvider : SchedulerProvider = mock()
        val testScheduler = SingleScheduler()

        whenever(schedulerProvider.io).thenReturn(testScheduler)
        whenever(schedulerProvider.main).thenReturn(testScheduler)

        viewModel = TasksViewModel(TasksViewModel.Companion.Params(useCase, schedulerProvider))
    }

    @After
    fun clearMocks() {
        // Ensures inline Kotlin mocks do not leak
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `subscribes to use case on init`() {
        verify(useCase, times(1)).loadAll()
    }

    @Test
    fun `refreshes use case on refresh`() {
        viewModel.refresh()
        verify(useCase, times(1)).refresh()
    }

    @Test
    fun `launch event triggered when task clicked`() {
        val observer: Observer<String> = mock()
        viewModel.launchTaskEvent.observeForever(observer)

        val id = "id"
        viewModel.onTaskClick(id)

        verify(observer, times(1)).onChanged(id)
    }

    @Test
    fun `loading event triggered on view model initialization`() {
        val observer: Observer<Boolean> = mock()
        viewModel.isLoading.observeForever(observer)

        verify(observer, times(1)).onChanged(true)
    }

    @Test
    fun `errorEvent gets correct value when failure resource observed`() {
        val errorEventObserver: Observer<Throwable> = mock()
        viewModel.errorEvent.observeForever(errorEventObserver)

        val stateObserver: Observer<Resource<List<Task>>> = mock()
        viewModel.state.observeForever(stateObserver)

        assertThat(viewModel.state.value, instanceOf(Resource.Loading::class.java))
        assertNull(viewModel.errorEvent.value)

        val error = Throwable("Some error, date: ${Date()}")
        val resource = Resource.Failure<List<Task>>(error)

        (useCase.loadAll() as Subject).onNext(resource)

        TimeUnit.MILLISECONDS.sleep(TasksViewModel.firstDataDelay + 50)

        assertThat(viewModel.state.value, instanceOf(Resource.Failure::class.java))
        assertThat(viewModel.state.value, equalTo(resource))
        assertSame(viewModel.errorEvent.value, error)

        verify(stateObserver, times(1)).onChanged(resource)
    }

    @Test
    fun `tasks event triggered when success resource observed`() {
        val tasksObserver : Observer<List<Task>> = mock()
        viewModel.tasksLiveData.observeForever(tasksObserver)

        val stateObserver : Observer<Resource<List<Task>>> = mock()
        viewModel.state.observeForever(stateObserver)

        val randomId1 = Random.nextUInt(from = 0U, until = 10U)
        val randomId2 = Random.nextUInt(from = 10U, until = 20U)

        val data = listOf(
            Task("$randomId1", "Task $randomId1", "Description $randomId1"),
            Task("$randomId2", "Task $randomId2", "Description $randomId2"),
        )

        val resource = Resource.Success(data)

        (useCase.loadAll() as Subject).onNext(resource)

        TimeUnit.MILLISECONDS.sleep(TasksViewModel.firstDataDelay + 50)

        verify(tasksObserver, times(1)).onChanged(data)
        verify(stateObserver, times(1)).onChanged(resource)
    }
}
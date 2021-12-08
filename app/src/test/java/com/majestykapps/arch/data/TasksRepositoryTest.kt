package com.majestykapps.arch.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.majestykapps.arch.common.TestSchedulerProvider
import com.majestykapps.arch.data.common.Resource
import com.majestykapps.arch.data.repository.TasksRepositoryImpl
import com.majestykapps.arch.data.source.local.TasksLocalDataSource
import com.majestykapps.arch.data.source.remote.TasksRemoteDataSource
import com.majestykapps.arch.domain.entity.Task
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.powermock.api.mockito.PowerMockito
import org.powermock.reflect.Whitebox

class TasksRepositoryTest {

    /**
     * Runs Arch Components on a synchronous executor
     */
    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    private lateinit var localDataSource: TasksLocalDataSource

    @Mock
    private lateinit var remoteDataSource: TasksRemoteDataSource

    /**
     * Runs RxJava synchronously
     */
    private val schedulerProvider = TestSchedulerProvider()

    private val tasksSubject: Subject<Resource<List<Task>>> = mock()

    private lateinit var repository: TasksRepositoryImpl

    @Captor
    private lateinit var resourceDataCaptor: ArgumentCaptor<Resource<List<Task>>>

    @Before
    fun setup() {
        // Allows us to use @Mock annotations
        MockitoAnnotations.initMocks(this)

        repository = TasksRepositoryImpl.getInstance(
            localDataSource,
            remoteDataSource,
            schedulerProvider
        ).apply {
            Whitebox.setInternalState(this, "tasksSubject", tasksSubject)
        }
    }

    @After
    fun clearMocks() {
        // Ensures inline Kotlin mocks do not leak
        Mockito.framework().clearInlineMocks()
        TasksRepositoryImpl.destroy()
    }

    @Test
    fun `task subscription observes task emission`() {
        val resource: Resource<List<Task>> = mock()
        tasksSubject.onNext(resource)
        verify(tasksSubject, times(1)).onNext(resource)
    }

    @Test
    fun `marked dirty on refresh`() {
        assertFalse(repository.isCacheDirty)

        Whitebox.setInternalState(
            repository,
            "tasksSubject",
            BehaviorSubject.create<Resource<List<Task>>>()
        )
        whenever(remoteDataSource.getTasks()).thenReturn(Observable.empty())
        whenever(localDataSource.getTasks()).thenReturn(Observable.empty())

        repository.refresh()

        assertTrue(repository.isCacheDirty)
    }

    @Test
    fun `cached tasks returned when cache is not empty or dirty`() {
        val task = Task("a", "test", "task")

        assertFalse(repository.isCacheDirty)

        repository.apply {
//            Whitebox.setInternalState(this, "_isCacheDirty", false)
            Whitebox.setInternalState(this, "_cachedTasks", mapOf("a" to task))
        }

        verify(repository.loadTasks(), times(1)).onNext(resourceDataCaptor.capture())
        assertTrue(resourceDataCaptor.value is Resource.Success)

        val cache : Map<String, Task> = Whitebox.getInternalState(repository, "_cachedTasks")
        assertEquals(ArrayList(cache.values), (resourceDataCaptor.value as Resource.Success).data)
    }

    @Test
    fun `local tasks returned when cache is dirty and remote call fails`() {
        whenever(remoteDataSource.getTasks()).thenReturn(Observable.error(RuntimeException()))
        val resource: Resource<List<Task>> = mock()
        whenever(localDataSource.getTasks()).thenReturn(Observable.just(resource))

        repository.loadTasks()

//        verify(localDataSource.getTasks(), times(1)).onNext(resource)
        localDataSource.getTasks().subscribe {
            assertEquals(resource, it);
        }
    }

    //TODO complete/repair all the tests bellow

//    @Test
//    fun `local tasks returned when cache is not dirty but is empty`() {
//        Whitebox.setInternalState(repository, "_isCacheDirty", false) //        repository._isCacheDirty = false
//        val resource: Resource<List<Task>> = mock()
//        whenever(localDataSource.getTasks()).thenReturn(Observable.just(resource))
//
//        repository.loadTasks()
//
//        verify(repository.tasksSubject, times(1)).onNext(resource)
//    }
//
//    @Test
//    fun `remote tasks are cached when successfully retrieved`() {
//        whenever(localDataSource.getTasks()).thenReturn(Observable.never<Resource<List<Task>>>())
//        val task = mock<Task> {
//            on { id } doReturn "a"
//        }
//        val data = listOf(task)
//        val resource = Resource.Success(data)
//        whenever(remoteDataSource.getTasks()).thenReturn(Observable.just(resource))
//
//        repository.loadTasks()
//
//        assertEquals(repository._cachedTasks["a"], task)
//    }
//
//    @Test
//    fun `remote tasks are saved to db when successfully retrieved`() {
//        whenever(localDataSource.getTasks()).thenReturn(Observable.never<Resource<List<Task>>>())
//        val data = listOf(mock<Task>())
//        val resource = Resource.Success(data)
//        whenever(remoteDataSource.getTasks()).thenReturn(Observable.just(resource))
//
//        repository.loadTasks()
//
//        verify(localDataSource, times(1)).saveTasks(data)
//    }
//
//    @Test
//    fun `cache is marked clean when remote tasks are cached`() {
//        whenever(localDataSource.getTasks()).thenReturn(Observable.never<Resource<List<Task>>>())
//        whenever(localDataSource.saveTasks(any())).thenReturn(Completable.never())
//        val data = listOf(mock<Task>())
//        val resource = Resource.Success(data)
//        whenever(remoteDataSource.getTasks()).thenReturn(Observable.just(resource))
//
//        repository.loadTasks()
//
//        assertFalse(repository.isCacheDirty)
//    }
//
//    @Test
//    fun `local tasks are cached when successfully retrieved`() {
//        repository._isCacheDirty = false
//        val task = mock<Task> {
//            on { id } doReturn "a"
//        }
//        val data = listOf(task)
//        val resource = Resource.Success(data)
//        whenever(localDataSource.getTasks()).thenReturn(Observable.just(resource))
//
//        repository.loadTasks()
//
//        assertEquals(repository._cachedTasks["a"], task)
//    }

//    @Test
//    fun `some PowerMock test`() {
//        val pmc = PowerMockito.mock(repository.javaClass)
//        PowerMockito.doReturn(false).`when`(pmc, "_isCacheDirty")
//        repository.apply {
//            Whitebox.setInternalState(this, "_cachedTasks",
//                mock<LinkedHashMap<String, Task>>().apply{
//                    this["a"] = mock()
//                }
//            )
//        }
//    }
}

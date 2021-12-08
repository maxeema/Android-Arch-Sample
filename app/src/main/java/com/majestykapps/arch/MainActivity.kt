package com.majestykapps.arch

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.majestykapps.arch.domain.entity.Task
import com.majestykapps.arch.domain.usecase.GetTask
import com.majestykapps.arch.domain.usecase.LoadTasks
import com.majestykapps.arch.presentation.taskdetail.TaskDetailViewModel
import com.majestykapps.arch.presentation.tasks.TasksSearchViewModel
import com.majestykapps.arch.presentation.tasks.TasksViewModel

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViewModels()
    }

    private fun initViewModels() {
        val app = application as App
        //
        val tasksViewModel = ViewModelProvider(
            this, TasksViewModel.FACTORY(
                TasksViewModel.Companion.Params(
                    LoadTasks(app.tasksRepository)
                )
            )
        ).get(TasksViewModel::class.java)
        //
        val tasksSearchViewModel = ViewModelProvider(
            this, TasksSearchViewModel.FACTORY(
                TasksSearchViewModel.Companion.Params(
                    tasksViewModel.tasksSource,
                    tasksViewModel.tasksLiveData as MutableLiveData<List<Task>>
                )
            )
        ).get(TasksSearchViewModel::class.java)
        //
        val tasksDetailViewModel = ViewModelProvider(
            this, TaskDetailViewModel.FACTORY(
                TaskDetailViewModel.Companion.Params(
                    GetTask(app.tasksRepository)
                )
            )
        ).get(TaskDetailViewModel::class.java)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

}

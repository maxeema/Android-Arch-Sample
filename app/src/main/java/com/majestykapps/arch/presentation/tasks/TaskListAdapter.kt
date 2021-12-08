package com.majestykapps.arch.presentation.tasks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.majestykapps.arch.databinding.TasksListItemBinding
import com.majestykapps.arch.domain.entity.Task

class TaskListAdapter(private val onClick: (View) -> Unit) : ListAdapter<Task, TaskListViewHolder>(
    DiffCallback
) {

    companion object DiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Task, newItem: Task) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        TaskListViewHolder.from(parent)

    override fun onBindViewHolder(holder: TaskListViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

}

class TaskListViewHolder(private var binding: TasksListItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(bindTask: Task, onClick: (View) -> Unit) = binding.apply {
        task = bindTask
        root.tag = bindTask
        root.setOnClickListener(onClick)
        executePendingBindings()
    }

    companion object {
        fun from(parent: ViewGroup) =
            TaskListViewHolder(
                TasksListItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
    }

}
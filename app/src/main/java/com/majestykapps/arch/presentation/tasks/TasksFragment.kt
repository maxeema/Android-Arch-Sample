package com.majestykapps.arch.presentation.tasks

import android.animation.ValueAnimator
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import androidx.vectordrawable.graphics.drawable.ArgbEvaluator
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.majestykapps.arch.R
import com.majestykapps.arch.databinding.FragmentTasksBinding
import com.majestykapps.arch.domain.entity.Task
import com.majestykapps.arch.util.NetworkUtils
import kotlinx.android.synthetic.main.fragment_tasks.*
import timber.log.Timber
import java.util.*

class TasksFragment : Fragment() {

    companion object {
        private const val TAG = "TasksFragment"
    }

    private val tasksModel: TasksViewModel by activityViewModels()
    private val searchModel: TasksSearchViewModel by activityViewModels()

    private lateinit var binding: FragmentTasksBinding

    private lateinit var searchView: SearchView
    private lateinit var searchMenuItem: MenuItem

    private val backHandler = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (searchView.isOpened()) {
                searchView.clearAndClose()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTasksBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.model = tasksModel
        binding.searchModel = searchModel

        binding.swipeRefresh.setOnRefreshListener {
            searchView.clearAndClose()
            refreshData(false)
        }

        val adapter = TaskListAdapter { view ->
            tasksModel.onTaskClick((view.tag as Task).id!!)
        }
        binding.recycler.adapter = adapter

        val dividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        dividerItemDecoration.setDrawable(ColorDrawable(resources.getColor(R.color.colorAccent)))
        binding.recycler.addItemDecoration(dividerItemDecoration);

        binding.refreshBtn.setOnClickListener {
            refreshData()
        }
        binding.retryBtn.setOnClickListener {
            refreshData()
        }

        //
        val updateTitleText = lambda@{ show: Boolean ->
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) return@lambda
            val titleTextColor = MaterialColors.getColor(binding.root, R.attr.colorOnPrimary)
            val colorFrom = if (!show) titleTextColor else Color.TRANSPARENT
            val colorTo = if (!show) Color.TRANSPARENT else titleTextColor
            ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo).apply {
                addUpdateListener { animator ->
                    binding.toolbar.setTitleTextColor(animator.animatedValue as Int)
                }
                start()
            }
        }
        //
        searchMenuItem = binding.toolbar.menu.findItem(R.id.search)
        enableSearch(false)
        searchView = searchMenuItem.actionView as SearchView
        searchView.apply {
            setQuery(searchModel.queryLiveData.value, false)
            isIconified = searchModel.searchViewIconifiedState.value == true
            //
            isSubmitButtonEnabled = false
            setOnSearchClickListener {
                searchModel.openSearchViewEvent.value = Unit
            }
            setOnCloseListener {
                searchModel.searchViewIconifiedState.value = true
                TransitionManager.beginDelayedTransition(binding.toolbar, ChangeBounds())
                binding.toolbar.title = resources.getString(R.string.app_name)
                updateTitleText(true)
                binding.appbar.setExpanded(true, true)
                backHandler.isEnabled = false
                false
            }
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String) = true.apply {
                    searchModel.filter(newText)
                }

                override fun onQueryTextSubmit(query: String?) = true //false
            })
        }

        return binding.root
    }

    private fun refreshData(update: Boolean = true) {
        if (update) {
            binding.swipeRefresh.isRefreshing = true
        }
        tasksModel.refresh()
    }

    private fun openDetailed(id: String) {
        val action = TasksFragmentDirections.actionTasksFragToTaskDetailFrag(id)
        findNavController().navigate(action)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initViewModelObservers()

        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backHandler)
    }

    private fun initViewModelObservers() {
        tasksModel.apply {
            areTasksLoaded.observe(viewLifecycleOwner) {
                enableSearch(it)
            }

            tasksLiveData.observe(viewLifecycleOwner) {
                binding.recycler.apply {
                    (adapter as TaskListAdapter).submitList(it) {
                        scrollToPosition(0)
                    }
                }
            }

            launchTaskEvent.observe(viewLifecycleOwner) { taskId ->
                Timber.tag(TAG).d("launchTaskEvent observed: $taskId")

                openDetailed(taskId)
            }

            isLoading.observe(viewLifecycleOwner) {
                Timber.tag(TAG).d("isLoading observed: $it")

                binding.swipeRefresh.isRefreshing = it
            }

            errorEvent.observe(viewLifecycleOwner) { throwable ->
                Timber.tag(TAG).d(
                    throwable,
                    "errorEvent observed, isConnectionError: ${
                        NetworkUtils.isConnectionError(
                            throwable
                        )
                    }"
                )
                // Show the snackbar only when we have some tasks shown already
                if (tasksModel.hasDataToShow.value == true) {
                    // Show the snackbar only if the fragment is resumed (active now)
                    if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                        val msgId = when (NetworkUtils.isConnectionError(throwable)) {
                            true -> R.string.cant_refresh_tasks_check_internet
                            else -> R.string.cant_refresh_tasks;
                        }
                        Snackbar.make(requireView(), msgId, Snackbar.LENGTH_LONG)
                            .setAction(R.string.retry_btn) {
                                refreshData()
                            }.show();
                    }
                }
            }
        }

        searchModel.apply {
            openSearchViewEvent.observe(viewLifecycleOwner) {
                TransitionManager.beginDelayedTransition(binding.toolbar, ChangeBounds())
    //                updateTitleText(false)
                searchModel.searchViewIconifiedState.value = false
                backHandler.isEnabled = true
            }
        }
    }

    private fun enableSearch(enable: Boolean) {
        searchMenuItem.isVisible = enable
        searchMenuItem.isEnabled = enable
    }

    override fun onStop() {
        super.onStop()
        searchView.clearFocus()
    }

    private fun SearchView.isOpened() = !isIconified
    private fun SearchView.clearAndClose() {
        setQuery("", false)
        isIconified = true
    }

}
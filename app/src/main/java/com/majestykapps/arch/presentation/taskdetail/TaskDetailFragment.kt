package com.majestykapps.arch.presentation.taskdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import com.majestykapps.arch.R
import com.majestykapps.arch.databinding.FragmentDetailTaskBinding
import com.majestykapps.arch.util.NetworkUtils
import timber.log.Timber

class TaskDetailFragment : Fragment(R.layout.fragment_detail_task) {

    private val viewModel: TaskDetailViewModel by activityViewModels()

    private lateinit var binding: FragmentDetailTaskBinding

    private val args: TaskDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDetailTaskBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.model = viewModel

        binding.refreshBtn.setOnClickListener {
            getData()
        }

        return binding.root
    }

    private fun getData() {
        viewModel.loadingEvent.value = true
        viewModel.getTask(args.taskId)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initViewModelObservers()

        (requireActivity() as AppCompatActivity).apply {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        getData()
    }

    private fun initViewModelObservers() {
        viewModel.apply {

            loadingEvent.observe(viewLifecycleOwner, Observer { isRefreshing ->
                Timber.tag(TAG).d("loadingEvent observed: $isRefreshing")
            })

            errorEvent.observe(viewLifecycleOwner, Observer { throwable ->
                Timber.tag(TAG).e(throwable,"errorEvent observed, isConnectionError: ${NetworkUtils.isConnectionError(throwable)}")
            })
        }
    }

    companion object {
        private const val TAG = "TaskDetailFragment"

        fun newInstance() = TaskDetailFragment()
    }

}
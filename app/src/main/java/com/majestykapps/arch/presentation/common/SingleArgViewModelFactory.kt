package com.majestykapps.arch.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SingleArgViewModelFactory {
    companion object {
        fun <T : ViewModel, A> create(constructor: (A) -> T):
                    (A) -> ViewModelProvider.NewInstanceFactory {
            return { arg: A ->
                object : ViewModelProvider.NewInstanceFactory() {
                    @Suppress("UNCHECKED_CAST")
                    override fun <V : ViewModel> create(modelClass: Class<V>): V {
                        return constructor(arg) as V
                    }
                }
            }
        }
    }
}
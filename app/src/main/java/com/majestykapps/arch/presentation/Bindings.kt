package com.majestykapps.arch.presentation

import android.view.View
import androidx.databinding.BindingAdapter

@BindingAdapter("visibleOn")
fun View.visibleOn(condition: Boolean?) {
    visibility = if (condition == true) View.VISIBLE else View.INVISIBLE
}

@BindingAdapter("goneIf")
fun View.goneIf(condition: Boolean?) {
    visibility = if (condition == true) View.GONE else View.VISIBLE
}





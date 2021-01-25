package com.dsp.androidsample

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

fun Context.toast(text: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, duration).show()
}

fun Context.toast(@StringRes resId: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, resId, duration).show()
}

var CompositeDisposable.add: Disposable?
    set(value) {
        value?.let { add(it) }
    }
    get() = null


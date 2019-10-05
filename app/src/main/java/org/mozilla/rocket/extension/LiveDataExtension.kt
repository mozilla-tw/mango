/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.extension

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations

fun <T> LiveData<T>.nonNullObserve(owner: LifecycleOwner, observer: (t: T) -> Unit) {
    this.observe(owner, Observer {
        observer(it!!)
    })
}

fun <X> LiveData<X>.take(count: Int): LiveData<X> {
    var remain = count
    val mediator = MediatorLiveData<X>()
    mediator.addSource(this) {
        mediator.value = it
        if (--remain == 0) {
            mediator.removeSource(this)
        }
    }

    return mediator
}

fun <X> LiveData<X>.first(): LiveData<X> = take(1)

/** Uses `Transformations.map` on a LiveData */
fun <X, Y> LiveData<X>.map(body: (X) -> Y): LiveData<Y> {
    return Transformations.map(this, body)
}

/** Uses `Transformations.switchMap` on a LiveData */
fun <X, Y> LiveData<X>.switchMap(body: (X) -> LiveData<Y>): LiveData<Y> {
    return Transformations.switchMap(this, body)
}

fun <X, Y> LiveData<X>.switchFrom(source: LiveData<Y>): LiveData<X> =
        source.switchMap { this.map { it } }

fun <A, B> combineLatest(a: LiveData<A>, b: LiveData<B>): LiveData<Pair<A?, B?>> {
    return MediatorLiveData<Pair<A?, B?>>().apply {
        var lastA: A? = null
        var lastB: B? = null
        var hasDataA = false
        var hasDataB = false

        addSource(a) {
            hasDataA = true
            lastA = it
            if (hasDataB) value = lastA to lastB
        }

        addSource(b) {
            hasDataB = true
            lastB = it
            if (hasDataA) value = lastA to lastB
        }
    }
}

fun <A, B> combineLatestNonNull(a: LiveData<A>, b: LiveData<B>): LiveData<Pair<A, B>> {
    return MediatorLiveData<Pair<A, B>>().apply {
        var lastA: A? = null
        var lastB: B? = null
        var hasDataA = false
        var hasDataB = false

        addSource(a) {
            hasDataA = true
            lastA = it
            if (hasDataB) value = lastA!! to lastB!!
        }

        addSource(b) {
            hasDataB = true
            lastB = it
            if (hasDataA) value = lastA!! to lastB!!
        }
    }
}
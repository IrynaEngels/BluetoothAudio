package com.irene.bluetoothaudio.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel

inline fun <T1 : Any, T2 : Any, R : Any> safeLet(p1: T1?, p2: T2?, block: (T1, T2) -> R?): R? {
    return if (p1 != null && p2 != null) block(p1, p2) else null
}

fun <E> SendChannel<E>.safeOfferCatching(element: E): Boolean {
    return runCatching { this.offer(element) }.getOrDefault(false)
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Channel<T>.safeOffer(source: T) {
    try {
        if(!this.isClosedForSend) {
            this.trySend(source).isSuccess
        }
    } catch (e: Exception) {
        log("Channel error occurred -> ${e.message}")
    }
}
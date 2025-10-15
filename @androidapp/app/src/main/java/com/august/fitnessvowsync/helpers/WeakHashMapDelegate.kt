package com.august.fitnessvowsync.helpers

import java.util.WeakHashMap
import kotlin.reflect.KProperty

class WeakHashMapDelegate<T, V>(
    private val weakMap: WeakHashMap<T, V>,
    private val defaultValue: V
) {
    operator fun getValue(thisRef: T, property: KProperty<*>): V {
        return weakMap[thisRef] ?: defaultValue
    }

    operator fun setValue(thisRef: T, property: KProperty<*>, value: V) {
        weakMap[thisRef] = value
    }
}
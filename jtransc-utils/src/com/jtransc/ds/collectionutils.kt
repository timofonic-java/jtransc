/*
 * Copyright 2016 Carlos Ballesteros Velasco
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jtransc.ds

import java.util.*

inline fun <reified T> Iterable<Any?>.cast() = this.filterIsInstance<T>()

fun <A, B> List<Pair<A, B>>.uniqueMap(): HashMap<A, B>? {
    val h = hashMapOf<A, B>()
    val okay = this.all { x ->
        val y = h.put(x.first, x.second)
        (y == null) || (y == x.second)
    }
    return if (okay) h else null
}

val <T> List<T>.head:T get() = this.first()
val <T> List<T>.tail:List<T> get() = this.drop(1)

fun <K, V> Map<K, V>.flip():Map<V, K> = this.map { it.value to it.key }.toMap()

val <T1, T2> Pair<List<T1>, List<T2>>.zipped:List<Pair<T1, T2>> get() {
    return this.first.zip(this.second)
}

class Queue<T>() : Iterable<T> {
    constructor(items:Iterable<T>) : this() {
        queueAll(items)
    }

    private val data = LinkedList<T>()

    val hasMore:Boolean get() = data.isNotEmpty()
    val length:Int get() = data.size

    fun queue(value: T): T {
        data.addFirst(value)
        return value
    }

    fun queueAll(items: Iterable<T>): Queue<T> {
        for (item in items) queue(item)
        return this
    }

    fun dequeue(): T {
        return data.removeLast()
    }

    override fun iterator(): Iterator<T> = data.iterator()
}

class Stack<T> : Iterable<T> {
    private val data = LinkedList<T>()

    val hasMore:Boolean get() = data.isNotEmpty()

    val length:Int get() = data.size

    fun push(value: T): T {
        data.addLast(value)
        return value
    }

    fun pop(): T {
        return data.removeLast()
    }

    override fun iterator(): Iterator<T> = data.iterator()

	fun isEmpty() = !hasMore
}

fun <T> List<T>.flatMapInChunks(chunkSize:Int, handler: (items: List<T>) -> List<T>): List<T> {
    var out = this
    var n = 0
    while (n + chunkSize <= out.size) {
	    //println("IN: $out")
        val extracted = out.slice(n until n + chunkSize)
        val toInsert = handler(extracted)
        out = out.slice(0 until n) + toInsert + out.slice(n + chunkSize until out.size)
	    //println("OUT: $out")
        n++
    }
    return out
}

fun <K, V> Map<K, V>.toHashMap():HashMap<K, V> {
    val out = hashMapOf<K, V>()
    for (pair in this) {
        out.put(pair.key, pair.value)
    }
    return out
}

fun <T> Iterable<T?>.stripNulls():List<T> {
	return this.filter { it != null }.map { it!! }
}
package ch.passenger.kinterest.kijs

import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import js.dom.html.HTMLOptionsCollection
import js.dom.html.HTMLOptionElement
import org.w3c.dom.NodeList
import org.w3c.dom.Node

/**
 * Created by svd on 07/01/2014.
 */

fun<T> Array<T>.forEach(cb: (T)->Unit) {
    for(it in this) cb(it)
}

fun<T> Array<T>.contains(t:T) :Boolean = any { it==t }

fun<T> Array<T>.indexOf(t:T) : Int {
    for(i in 0..(size-1)) if(get(i)==t) return i
    return -1
}

fun<T> Array<T>.any(cond:(T)->Boolean) : Boolean {
    for(it in this) if(cond(it)) return true
    return false
}

fun<T> Array<T>.none(cond:(T)->Boolean) : Boolean {
    for(it in this) if(cond(it)) return false
    return true
}

fun<T> Iterable<T>.forEach(cb: (T)->Unit) {
    for(it in this) cb(it)
}

fun<T> Iterable<T?>.notNulls() : Iterable<T> {
    val res = ArrayList<T>()
    for(it in this) if(it != null) res.add(it)

    return res
}

fun<T> Iterable<T>.filter(cb: (T)->Boolean) : Iterable<T> {
    val res = ArrayList<T>()
    for(it in this) if(cb(it)) res.add(it)
    return res
}

fun<T,U> Iterable<T>.map(cb: (T)->U) : Iterable<U> {
    val res = ArrayList<U>()
    this.forEach { res.add(cb(it)) }
    return res
}


fun<T,U> Iterable<T>.reduce(initial:U,cb: (U,T)->U) : U {
    var res : U = initial
    for(it in this) res = cb(res, it)
    return res
}

fun<T> Iterable<T>.any(cond:(T)->Boolean) : Boolean {
    for(it in this) if(cond(it)) return true
    return false
}

fun<T> Iterable<T>.all(cond:(T)->Boolean) : Boolean {
    for(it in this) if(!cond(it)) return false
    return true
}


fun<T> Iterable<T>.firstThat(cond:(T)->Boolean) : T? {
    for(it in this) if(cond(it)) return it
    return null
}

fun<T> Iterable<T>.none(cond:(T)->Boolean) : Boolean {
    for(it in this) if(cond(it)) return false
    return true
}

fun<T> Iterable<T>.count() : Int {
    var res = 0
    forEach { res++ }
    return res
}


fun<T,U> Array<T>.map(cb:(T)->U) : Iterable<U> {
    val res = ArrayList<U>()
    this.forEach { res.add(cb(it)) }
    return res
}

fun<T,U> Array<T>.reduce(initial:U,cb: (U,T)->U) : U {
    var res : U = initial
    for(it in this) res = cb(res, it)
    return res
}

fun<K,V> mapOf(ps:Iterable<Tuple2<K,V>>) : Map<K,V> {
    val m = HashMap<K,V>()
    ps.forEach { m.put(it.first, it.second) }
    return m
}

fun<T> setOf(vararg ts:T) : Set<T> {
    val s = HashSet<T>()
    ts.forEach { s.add(it) }
    return s
}

fun<T> listOf(vararg ts:T) : List<T> {
    val l = ArrayList<T>(ts.size)
    ts.forEach { l.add(it) }
    return l
}

public class Tuple2<A, B> (
        public val first: A,
        public val second: B
) {
    public fun component1(): A = first
    public fun component2(): B = second

    public fun toString(): String = "($first, $second)"
}

fun Int.to(n:Int) : Iterable<Int> {
    val res = ArrayList<Int>()
    if(this<=n) {
        var i = this
        while(i<=n) {
            res.add(i)
            i=i+1
        }
    } else {
        var i = this
        while(i>=n) {
            res.add(i)
            i=i-1
        }
    }
    return res
}

fun HTMLOptionsCollection.forEach(cb:(HTMLOptionElement)->Unit) {
    for(i in 0..(this.length.toInt()-1)) cb(item(i) as HTMLOptionElement)
}

fun HTMLOptionsCollection.firstThat(cb:(HTMLOptionElement)->Boolean) : HTMLOptionElement? {
    for(i in 0..(this.length.toInt()-1)) if(cb(item(i) as HTMLOptionElement)) return item(i) as HTMLOptionElement
    return null
}

fun HTMLOptionsCollection.indexWhere(cb:(HTMLOptionElement)->Boolean) : Int {
    for(i in 0..(this.length.toInt()-1)) if(cb(item(i) as HTMLOptionElement)) return i
    return -1
}

fun NodeList.forEach(cb:(Node)->Unit) {
    for(i in 0..(this.length-1)) cb(item(i))
}

fun NodeList.any(cb:(Node)->Boolean) : Boolean {
    for(i in 0..(this.length-1)) if(cb(item(i))) return  true
    return false
}

fun<T> NodeList.map(cb:(Node)->T) : Iterable<T> {
    val res = ArrayList<T>()
    for(i in 0..(this.length-1)) res.add(cb(item(i)))
    return res
}

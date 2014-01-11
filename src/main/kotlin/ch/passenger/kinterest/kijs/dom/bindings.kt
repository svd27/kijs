package ch.passenger.kinterest.kijs.dom

import org.w3c.dom.events.EventTarget
import org.w3c.dom.Element
import js.dom.html.HTMLElement
import js.dom.html.HTMLDocument
import org.w3c.dom.events.Event

/**
 * Created by svd on 07/01/2014.
 */

trait Output {
    public native fun log(message: Any): Unit
    public native fun debug(message: Any): Unit
    public native fun warn(message: Any): Unit
    public native fun error(message: Any): Unit
}

public native var console : Output = js.noImpl

native trait EventSource {
    fun addEventListener(kind:String, cb:(Event)->Unit) {addEventListener(kind, cb, false)}
    fun addEventListener(kind:String, cb:(Event)->Unit, capture:Boolean) = js.noImpl
}

native trait MessageEvent : org.w3c.dom.events.Event {
    val data : String
}

native trait DOMTokenList {
    val length : Int
    fun add(token:String)
    fun remove(token:String)
    fun toggle(token:String)
    fun item(idx:Int) : String
    fun contains(token:String) : Boolean
}

public native val Element.classList : DOMTokenList = js.noImpl
public native val Element.dataset : MutableMap<String,String> = js.noImpl
public native fun Element.setAttribute(name:String, value:String):Unit = js.noImpl
public native fun Element.getAttribute(name:String):String = js.noImpl
public native fun Element.removeAttribute(name:String):String = js.noImpl
public native fun HTMLElement.focus() : Unit = js.noImpl

public native fun KIDATAset(e:HTMLElement, name:String, value:String) : Unit = js.noImpl
public native fun KIDATAget(e:HTMLElement, name:String) : String = js.noImpl
public native fun KIStyle(e:HTMLElement, name:String, value:String) : Unit = js.noImpl
public native fun KIStyle(e:HTMLElement, name:String) : String = js.noImpl


public native var HTMLDocument.kidata : MutableMap<String,Any>? = null

native fun EventTarget.addEventListener(kind:String, cb:(Event)->Unit, capture:Boolean=false) = js.noImpl

native trait KeyboardEvent : Event {
    val keyCode : Long
}

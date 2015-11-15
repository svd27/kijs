package ch.passenger.kinterest.kijs.dom

import org.w3c.dom.events.EventTarget
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.Document
import org.w3c.dom.HTMLCollection
import org.w3c.dom.css.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent




/**
 * Created by svd on 07/01/2014.
 */

interface  Output {
    public @native fun log(message: Any): Unit
    public @native fun debug(message: Any): Unit
    public @native fun warn(message: Any): Unit
    public @native fun error(message: Any): Unit
}

//public @native var console: Output = noImpl

@native interface EventSource {
    fun addEventListener(kind: String, cb: (Event) -> Unit) {
        addEventListener(kind, cb, false)
    }
    @native fun addEventListener(kind: String, cb: (Event) -> Unit, capture: Boolean) : Unit = noImpl
}

@native class MessageEvent : org.w3c.dom.events.Event("message") {
    val data: String = noImpl
}

@native interface DOMTokenList {
    val length: Int
    fun add(token: String)
    fun remove(token: String)
    fun toggle(token: String)
    fun item(idx: Int): String
    fun contains(token: String): Boolean
}

public @native val Element.classList: DOMTokenList get() = noImpl
public @native val Element.dataset: MutableMap<String, String> get() = noImpl
public @native fun Element.setAttribute(name: String, value: String): Unit = noImpl
public @native fun Element.getAttribute(name: String): String = noImpl
public @native fun Element.removeAttribute(name: String): String = noImpl
public @native fun HTMLElement.focus(): Unit = noImpl

public @native fun KIDATAset(e: HTMLElement, name: String, value: String): Unit = noImpl
public @native fun KIDATAget(e: HTMLElement, name: String): String = noImpl
public @native fun KIStyle(e: HTMLElement, name: String, value: String): Unit = noImpl
public @native fun KIStyle(e: HTMLElement, name: String): String = noImpl



@native fun EventTarget.addEventListener(kind: String, cb: (Event) -> Unit, capture: Boolean = false) : Unit = noImpl

@native class KeyboardEvent : Event("keyboard") {
    val keyCode: Long = noImpl
}



public @native var Document.kidata: MutableMap<String,Any>?
    get() = noImpl
    set(v) = noImpl


public fun StyleSheetList.forEach(cb: (StyleSheet) -> Unit) {
    for (i in (0..this.length - 1)) cb(item(i)!!)
}

fun HTMLCollection.forEach(cb:HTMLCollection.(Element)->Unit) {
    for(i in 0..length-1) cb(get(i)!!)
}

fun StyleSheet.deleteRule(idx:Int) : Unit = noImpl
fun StyleSheet.insertRule(css:String,idx:Int) : Int = noImpl

fun CSSRuleList.forEach(cb:(CSSRule)->Unit) {
    for(i in 0..length-1) cb(item(i)!!)
}


/*
public @native interface CSSRuleList {
    public val length: Int
    public fun item(idx: Int): CSSRule
}

public @native interface CSSStyleRule : CSSRule {
    public val style: CSSStyleDeclaration
}
*/

public fun CSSStyleDeclaration.forEach(cb:(String,String)->Unit) {
    var i = 0
    while(i<length) {
        cb(item(i), this.getPropertyValue(item(i)))
        i++
    }
}

/*
public @native val CSSRule.`type`: Int get() = noImpl

public fun CSSRuleList.forEach(cb: (CSSRule) -> Unit) {
    for (i in (0..this.length - 1)) cb(item(i))
}
*/

/*
public @native fun Document.createEvent(kind: String): Event = noImpl
public fun Document.createMouseEvent(): MouseEvent = this.createEvent("MouseEvents") as MouseEvent
public fun Document.createChangeEvent(): Event {
    val event = this.createEvent("HTMLEvents")
    event.initEvent("change", false, true)
    return event
}
*/

/*
public @native fun HTMLElement.dispatchEvent(ev:Event) : Boolean = noImpl

public @native fun MouseEvent.initMouseEvent(name: String, canBubble: Boolean, cancelable: Boolean, view: Any = kotlin.browser.window,
                                            detail: Int = 0, screenX: Int = 0, screenY: Int = 0, clientX: Int = 0, clientY: Int = 0,
                                            ctrlKey: Boolean = false, altKey: Boolean = false, shiftKey: Boolean = false, metaKey: Boolean = false,
                                            button: Int = 0, relatedTarget: Any? = null): Unit = noImpl

*/


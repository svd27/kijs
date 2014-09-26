package ch.passenger.kinterest.kijs.dom

import org.w3c.dom.events.EventTarget
import org.w3c.dom.Element
import js.dom.html.HTMLElement
import js.dom.html.HTMLDocument
import org.w3c.dom.events.Event
import js.dom.html.CSSRule
import js.dom.html.CSSStyleDeclaration
import org.w3c.dom.events.MouseEvent

/**
 * Created by svd on 07/01/2014.
 */

trait Output {
    public native fun log(message: Any): Unit
    public native fun debug(message: Any): Unit
    public native fun warn(message: Any): Unit
    public native fun error(message: Any): Unit
}

public native var console: Output = js.noImpl

native trait EventSource {
    fun addEventListener(kind: String, cb: (Event) -> Unit) {
        addEventListener(kind, cb, false)
    }
    fun addEventListener(kind: String, cb: (Event) -> Unit, capture: Boolean) = js.noImpl
}

native trait MessageEvent : org.w3c.dom.events.Event {
    val data: String
}

native trait DOMTokenList {
    val length: Int
    fun add(token: String)
    fun remove(token: String)
    fun toggle(token: String)
    fun item(idx: Int): String
    fun contains(token: String): Boolean
}

public native val Element.classList: DOMTokenList get() = js.noImpl
public native val Element.dataset: MutableMap<String, String> get() = js.noImpl
public native fun Element.setAttribute(name: String, value: String): Unit = js.noImpl
public native fun Element.getAttribute(name: String): String = js.noImpl
public native fun Element.removeAttribute(name: String): String = js.noImpl
public native fun HTMLElement.focus(): Unit = js.noImpl

public native fun KIDATAset(e: HTMLElement, name: String, value: String): Unit = js.noImpl
public native fun KIDATAget(e: HTMLElement, name: String): String = js.noImpl
public native fun KIStyle(e: HTMLElement, name: String, value: String): Unit = js.noImpl
public native fun KIStyle(e: HTMLElement, name: String): String = js.noImpl



native fun EventTarget.addEventListener(kind: String, cb: (Event) -> Unit, capture: Boolean = false) = js.noImpl

native trait KeyboardEvent : Event {
    val keyCode: Long
}



public native var HTMLDocument.kidata: MutableMap<String,Any>?
    get() = js.noImpl
    set(v) = js.noImpl

public native val HTMLDocument.styleSheets: StyleSheetList get() = js.noImpl

public native trait StyleSheetList {
    public val length: Int
    public fun item(idx: Int): StyleSheet
}

public fun StyleSheetList.forEach(cb: (StyleSheet) -> Unit) {
    for (i in (0..this.length - 1)) cb(item(i))
}

public native trait CSSRuleList {
    public val length: Int
    public fun item(idx: Int): CSSRule
}

public native trait CSSStyleRule : CSSRule {
    public val style: CSSStyleDeclaration
}

public fun CSSStyleDeclaration.forEach(cb:(String,String)->Unit) {
    var i = 0
    while(i<length) {
        cb(item(i), this.getPropertyValue(item(i)))
        i++
    }
}

public native val CSSRule.`type`: Int get() = js.noImpl

public fun CSSRuleList.forEach(cb: (CSSRule) -> Unit) {
    for (i in (0..this.length - 1)) cb(item(i))
}

public native fun HTMLDocument.createEvent(kind: String): Event = js.noImpl
public fun HTMLDocument.createMouseEvent(): MouseEvent = this.createEvent("MouseEvents") as MouseEvent
public fun HTMLDocument.createChangeEvent(): Event {
    val event = this.createEvent("HTMLEvents")
    event.initEvent("change", false, true)
    return event
}

public native fun HTMLElement.dispatchEvent(ev:Event) : Boolean = js.noImpl

public native fun MouseEvent.initMouseEvent(name: String, canBubble: Boolean, cancelable: Boolean, view: Any = js.dom.html.window,
                                            detail: Int = 0, screenX: Int = 0, screenY: Int = 0, clientX: Int = 0, clientY: Int = 0,
                                            ctrlKey: Boolean = false, altKey: Boolean = false, shiftKey: Boolean = false, metaKey: Boolean = false,
                                            button: Int = 0, relatedTarget: Any? = null): Unit = js.noImpl

public native trait StyleSheet {
    public val href: String
    public val cssRules: CSSRuleList
    public fun insertRule(css: String, idx: Int): Int
    public fun deleteRule(idx: Int)
}



package ch.passenger.kinterest.kijs.ui

import kotlin.js.dom.html.document
import org.w3c.dom.Node
import kotlin.js.dom.html.HTMLElement
import kotlin.js.dom.html.HTMLDivElement
import kotlin.js.dom.html.HTMLDListElement

import ch.passenger.kinterest.kijs.dom.*
import kotlin.js.dom.html.HTMLInputElement
import ch.passenger.kinterest.kijs.forEach
import kotlin.js.dom.html.HTMLAnchorElement
import rx.js.Disposable
import java.util.HashSet
import org.w3c.dom.events.Event
import rx.js.*
import org.w3c.dom.Element
import java.util.ArrayList
import kotlin.js.dom.html.HTMLSelectElement
import kotlin.js.dom.html.HTMLOptionElement
import ch.passenger.kinterest.kijs.indexWhere
import kotlin.js.dom.html.HTMLLabelElement
import ch.passenger.kinterest.kijs.map
import kotlin.js.dom.html.HTMLButtonElement
import kotlin.js.dom.html.HTMLTextAreaElement

/**
 * Created by svd on 07/01/2014.
 */

trait BaseComponent<T : Element> : Disposable{
    val root: T
    val disposables: MutableSet<Disposable>
    val ready: Subject<Boolean>

    protected abstract fun node(): T

    public fun<C : BaseComponent<*>> plus(c: C): C {
        root.appendChild(c.root)
        return c
    }

    /**
     * normally ready subscribers are called after root node has been added.
     * if a component needs to initialise after that point, override readyState with false
     * it is then your responsibility to call iAmReady() when appropriate
     */
    public open fun readyState(): Boolean = true
    //make sure ready is only called once
    protected var readyCalled: Boolean

    protected fun iAmReady() {
        if (!readyCalled) {
            readyCalled = true
            ready.onNext(true)
        } else {
            console.error("cant be doubly ready....")
        }
    }

    public fun onReady(cb: (Boolean) -> Unit): Disposable {
        if (!readyCalled)
            return ready.subscribe(cb)
        else cb(true)
        return object : Disposable {
            override fun dispose() {
            }
        }
    }

    public fun String.plus() {
        this@BaseComponent.root.textContent = this
    }

    public fun att(name: String): String = root.getAttribute(name)
    public fun att(name: String, value: String): Unit = root.setAttribute(name, value)
    public fun hasAtt(name:String) : Boolean = root.hasAttribute(name)

    public fun removeAtt(name: String): Unit = root.removeAttribute(name)

    public fun enabled(fl: Boolean) {
        if (fl) removeAtt("disabled") else att("disabled", "disabled")
    }

    fun<E : HTMLElement, U : BaseComponent<E>> add(c: U, init: U.() -> Unit): U {
        c.init()
        this + c
        return c
    }


    public fun on(event: String, cb: (Event) -> Unit): Disposable {
        return on(event).subscribe(cb)
    }

    public fun on(event: String): Observable<Event> {
        if (event == "dblclick") {
            return Rx.Observable.merge(Rx.Observable.fromEvent<Event>(root as EventSource, event), Rx.Observable.fromEvent<Event>(root as EventSource, "dbltap"))
        } else
            return Rx.Observable.fromEvent<Event>(root as EventSource, event)
    }

    fun remove() {
        if (root.parentNode != null) {
            root.parentNode.removeChild(root)
        }
        dispose()
    }

    fun removeChildren() {
        //if nodelist is live collection removing will change it
        //so map just copies first then deletes
        root.childNodes.map { it }.forEach { it.parentNode.removeChild(it) }
    }

    override fun dispose() {
        disposables.forEach { it.dispose() }
        disposables.clear()
    }

    var textContent: String
        get() = root.textContent
        set(v) {
            root.textContent = v
        }

    class object {
        var idcount = 0
        fun id() = idcount++.toString()
    }
}


open class Tag<T : HTMLElement>(val name: String, val id: String = BaseComponent.id().toString()) : BaseComponent<T> {
    override val ready: Subject<Boolean> = Subject()
    override var readyCalled: Boolean = false
    var aroot: T? = null
    override final val root: T
        get() {
            if (aroot == null) {
                aroot = node(); root.id = id; initialise(aroot!!); if (readyState()) iAmReady();
            };  return aroot!!
        }



    override val disposables: MutableSet<Disposable> = HashSet()

    override final fun node(): T {
        val element = document.createElement(name)
        if (element is HTMLElement) element.id = id

        return element as T
    }

    open fun initialise(n: T) {
    }

    fun addClasses(vararg cls: String) = cls.forEach { addClass(it) }
    fun addClass(cls: String) = root.classList.add(cls)
    fun removeClass(cls: String) = root.classList.remove(cls)
    fun containsClass(cls: String): Boolean = root.classList.contains(cls)
    fun icon(id: String = BaseComponent.id(), init: Icon.() -> Unit): Icon {
        val i = Icon(id)
        i.init()
        return plus(i)
    }

    fun data(name: String): String? = KIDATAget(root, name)
    fun data(name: String, value: String): Unit {
        KIDATAset(root, name, value)
    }

    fun style(name: String): String = KIStyle(root, name)
    fun style(name: String, value: String) {
        KIStyle(root, name, value)
    }

    fun blur(cb: (Event) -> Unit): Disposable = on("blur", cb)

    fun hide() = style("display", "none")
    fun show(style: String = "inline") = style("display", style)

    fun tags(name: String): Iterable<HTMLElement> {
        val l = ArrayList<HTMLElement>()
        val nl = root.getElementsByTagName(name)
        for (i in 0..(nl.length - 1)) {
            l.add(nl.item(i) as HTMLElement)
        }
        return l
    }
}

abstract class PhraseContainer<T : HTMLElement>(name: String, id: String) : Tag<T>(name, id) {
    fun span(id: String = BaseComponent.id(), init: Span.() -> Unit): Span {
        return this + Span(id, init)
    }

    fun button(id: String = BaseComponent.id(), init: Button.() -> Unit): Button {
        val b = Button(id)
        b.init()
        return plus(b)
    }

    fun anchor(id: String = BaseComponent.id(), init: Anchor.() -> Unit): Anchor {
        val a = Anchor(id)
        a.init()
        return plus(a)
    }

    fun input(id: String = BaseComponent.id(), init: TextInput.() -> Unit): TextInput {
        val ti = TextInput(id)
        ti.init()
        return plus(ti)
    }

    fun checkbox(id: String = BaseComponent.id(), init: CheckBox.() -> Unit): CheckBox {
        val ti = CheckBox(id)
        ti.init()
        return plus(ti)
    }

    fun label(id: String = BaseComponent.id(), init: Label.() -> Unit): Label {
        val l = Label(id)
        return add(l, init)
    }

    fun<T : Tag<*>> labelled(lbl: String, id: String = BaseComponent.id(), t: T, init: T.() -> Unit): T {
        val l = label() { target = t.id; textContent = lbl }
        t.init()
        plus(t)
        return t
    }

    fun labelledInput(lbl: String, id: String = BaseComponent.id(), init: TextInput.() -> Unit): TextInput {
        val ti = TextInput()
        return labelled(lbl, id, ti, init)
    }

    fun select(id: String = BaseComponent.id(), init: SelectOne.() -> Unit): SelectOne {
        val sel = SelectOne(id)
        return add(sel, init)
    }
    fun textarea(content:String, id:String = BaseComponent.id(), init:TextArea.()->Unit) : TextArea {
        val ta = TextArea()
        ta.textContent = content
        return add(ta, init)
    }

}

abstract class FlowContainer<T : HTMLElement>(name: String, id: String) : PhraseContainer<T>(name, id) {
    fun div(id: String = BaseComponent.id(), init: Div.() -> Unit): Div {
        val d = Div(id)
        d.init()
        return plus(d)
    }


    fun dl(id: String = BaseComponent.id(), init: DL.() -> Unit): DL {
        return this + DL(id, init)
    }


    fun table(id: String = BaseComponent.id(), init: Table.() -> Unit): Table {
        val tbl = Table(id)
        return add(tbl, init)
    }



    fun datalist(id: String = BaseComponent.id(), init: DataList.() -> Unit): DataList {
        val sel = DataList(id)
        return add(sel, init)
    }


}

class Div(id: String = BaseComponent.id()) : FlowContainer<HTMLDivElement>("div", id)
class Span(id: String = BaseComponent.id(), init: Span.() -> Unit) : FlowContainer<HTMLElement>("span", id) {
    {
        init()
    }
}

class DL(id: String = BaseComponent.id(), init: DL.() -> Unit) : Tag<HTMLDListElement>("dl", id) {
    {
        init()
    }
    fun dt(id: String = BaseComponent.id(), init: DT.() -> Unit): DT {
        val dt = DT(id)
        dt.init()
        return plus(dt)
    }

    fun dd(id: String = BaseComponent.id(), init: DD.() -> Unit): DD {
        val dd = DD(id)
        dd.init()
        return plus(dd)
    }
}


class DT(id: String = BaseComponent.id()) : Tag<HTMLElement>("dt", id)

class DD(id: String = BaseComponent.id()) : FlowContainer<HTMLElement>("dd", id)

open class Input(val itype: String, id: String = BaseComponent.id()) : Tag<HTMLInputElement>("input", id) {
    override fun initialise(n: HTMLInputElement) {
        super<Tag>.initialise(n)
        n.`type` = itype
    }

    var value: String
        get() = if (root.value == null) "" else root.value
        set(v) {
            root.value = v
        }

    fun change(cb: (Event) -> Unit): Disposable = on("change", cb)
}

open class TextInput(id: String = BaseComponent.id()) : Input("text", id)
open class NumberInput(id: String = BaseComponent.id()) : Input("number", id) {
    open var valuesAsNumber: Number?
        get() = safeParseDouble(value)
        set(v) {
            value = v.toString()
        }
}

open class IntegerInput(id: String = BaseComponent.id()) : NumberInput(id) {

    override fun initialise(n: HTMLInputElement) {
        super.initialise(n)
        att("pattern", "[+-]?[0-9]+")
    }
}

open class CheckBox(id : String = BaseComponent.id()) : Input("checkbox", id) {
    var checked : Boolean
      get() = root.checked
      set(v) {
          root.checked = v
      }
}

class IButton(id: String = BaseComponent.id()) : Input("button", id)

class Icon(id: String = BaseComponent.id()) : Tag<HTMLElement>("i", id)

class Anchor(id: String = BaseComponent.id()) : Tag<HTMLAnchorElement>("a", id) {
    var href: String
        get() = root.href
        set(v) {
            root.href = v
        }

    fun click(cb: (Event) -> Unit): Disposable = on("click", cb)
}

class Option(id: String = BaseComponent.id()) : Tag<HTMLOptionElement>("option", id) {
    public var label: String
        get() = root.label
        set(v) {
            root.label = v
        }

    public var value: String
        get() = root.value
        set(v) {
            root.value = v
        }

    public var selected: Boolean
        get() = root.selected
        set(v) {
            root.selected = v
        }
}

open class Select(id: String = BaseComponent.id()) : Tag<HTMLSelectElement>("select", id) {
    public fun option(value: String, label: String, init: Option.() -> Unit): Option {
        val o = Option()
        o.label = label
        o.value = value
        o.init()
        return plus(o)
    }

    public fun contains(v:String) : Boolean {
        var fl = false
        root.options.forEach {
            fl = fl || it.value == v
        }
        return fl
    }
}

class SelectOne(id: String = BaseComponent.id()) : Select(id) {
    var selected: Int
        get() = root.selectedIndex.toInt()
        set(v) {
            root.selectedIndex = v.toDouble()
        }

    var selectedValue: String?
        get() = (root.options.item(root.selectedIndex) as HTMLOptionElement?)?.value
        set(v) {
            val idx = root.options.indexWhere { it.value == v }; root.selectedIndex = idx.toDouble()
        }


    override fun initialise(n: HTMLSelectElement) {
        root.multiple = false
    }
}

class SelectMulitple(id: String = BaseComponent.id()) : Select(id) {
    var selected: Array<Int>
        get() {
            var res = ArrayList<Int>();
            for (i in 0..(root.options.length.toInt() - 1)) if ((root.options.item(i) as HTMLOptionElement).selected) res.add(i)
            return res.copyToArray()
        }
        set(v) {
            root.options.forEach { it.selected = false }; v.forEach { (root.options.item(it) as HTMLOptionElement).selected = true }
        }

    var selectedValues: Array<String>
        get()  {
            var res = ArrayList<String>(); root.options.forEach { if (it.selected) res.add(it.value) }; return res.copyToArray()
        }
        set(v) {
            root.options.forEach { it.selected = it.value in v }
        }


    override fun initialise(n: HTMLSelectElement) {
        root.multiple = true
    }
}

class Label(id: String = BaseComponent.id()) : Tag<HTMLLabelElement>("label", id) {
    var target: String
        get() = root.htmlFor
        set(v) {
            root.htmlFor = v
        }
}

class Button(id: String = BaseComponent.id()) : Tag<HTMLButtonElement>("button", id)

class DataList(id: String = BaseComponent.id()) : Tag<HTMLElement>("datalist", id) {
    public fun option(value: String, label: String, init: Option.() -> Unit): Option {
        val o = Option()
        o.label = label
        o.value = value
        o.init()
        return plus(o)
    }
}

open class TextArea(id: String = BaseComponent.id()) : Tag<HTMLTextAreaElement>("textarea", id)

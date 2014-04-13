package ch.passenger.kinterest.kijs.ui

import ch.passenger.kinterest.kijs.model.Interest
import js.dom.html.HTMLDivElement
import ch.passenger.kinterest.kijs.model.PropertyDescriptor
import ch.passenger.kinterest.kijs.forEach
import ch.passenger.kinterest.kijs.dom.console
import ch.passenger.kinterest.kijs.model.EntityDescriptor
import ch.passenger.kinterest.kijs.model.Galaxy
import js.dom.html.HTMLInputElement
import ch.passenger.kinterest.kijs.model.RelationFilter
import ch.passenger.kinterest.kijs.model.PropertyFilter
import ch.passenger.kinterest.kijs.model.InterestOrderEvent
import ch.passenger.kinterest.kijs.map
import ch.passenger.kinterest.kijs.reduce
import ch.passenger.kinterest.kijs.model.InterestLoadEvent
import ch.passenger.kinterest.kijs.any
import js.dom.html.HTMLOptionElement
import ch.passenger.kinterest.kijs.dom.KIDATAset
import ch.passenger.kinterest.kijs.dom.*
import js.dom.html.HTMLElement
import ch.passenger.kinterest.kijs.dom.KIDATAget
import ch.passenger.kinterest.kijs.dom.KeyboardEvent
import ch.passenger.kinterest.kijs.listOf
import org.w3c.dom.Element
import java.util.ArrayList
import ch.passenger.kinterest.kijs.model.Entity
import rx.js.Subject
import rx.js.Disposable
import org.w3c.dom.events.Event
import ch.passenger.kinterest.kijs.filter
import ch.passenger.kinterest.kijs.makeString
import ch.passenger.kinterest.kijs.APP
import ch.passenger.kinterest.kijs.model.EntityState

/**
 * Created by svd on 10/01/2014.
 */
class FilterComponent(val interest: Interest, id: String = BaseComponent.id()) : Component<HTMLDivElement>(id) {

    override fun initialise(n: HTMLDivElement) {
        val d = this
        d.addClasses("filter", interest.galaxy.descriptor.entity)
    }
}

class FilterRow(val interest: Interest, val property: PropertyDescriptor, id: String = BaseComponent.id()) : Component<HTMLDivElement>(id) {
    var value: Tag<*> = TextInput()
    var op: String = "EQ"
    val row: FlowContainer<HTMLDivElement> = this

    override fun initialise(n: HTMLDivElement) {
        row.select {
            if (!property.enum) {
                option("EQ", "=") { }
                option("GT", ">") { }
                option("LT", "<") { }
                option("LTE", "<=") { }
                option("GTE", ">=") { }
                option("LIKE", "~=") { }
                selectedValue = "EQ"
            } else {
                option("IN", "IN") { }
            }
        }

        update()
    }

    fun update() {
        value.remove()
        if (op == "LIKE") {
            value = row.input() { value = "" }
        } else if (property.enum) {
            val sel = SelectMulitple()
            value = row + sel

            property.enumvalues.forEach {
                sel.option(it, it) { }
            }
        } else if (property.floaty) {
            val ni = NumberInput()
            ni.valuesAsNumber = 0.0
            row + ni
        }
    }
}

class CustomCompleter(val galaxy: Galaxy, val projections: Array<String>, val label: String, id: String = BaseComponent.id()) : Component<HTMLDivElement>(id) {
    var interest: Interest? = null
    val input: TextInput = TextInput();
    val list: Div = Div()
    val candidates : MutableList<Long> = ArrayList()
    var selected : Long? = null
    val subject : Subject<Entity?> = Subject()
    var silent : Boolean = false
      set(v) {
          if(v) {
          candidates.clear()
          selected = null
          }
      }

    var createFilter : (String)->String = {
        (pre) ->
        projections.map { "$it ~= \"$pre.*\"" }.makeString(" or ")
    }

    {
        this.onReady { build() }
    }

    fun on(cb:(Entity?)->Unit) : Disposable = subject.subscribe(cb)

    override fun initialise(n: HTMLDivElement) {
        val d = this
        d.addClass("completer")
        input.addClasses("completerinput", galaxy.descriptor.entity)
        list.addClass("completerlist")
        d+input
        d+list
    }

    fun select() {
        val entity = interest?.entity(selected!!)!!
        input.value = entity[label].toString()
        if(!silent)
            subject.onNext(entity)
        list.removeChildren()
    }

    fun build() {
        console.log("completer: list#${list.id}")
        val that = this
        val input = this.input
        val galaxy = this.galaxy
        val dl = list
        val candidates = that.candidates
        galaxy.create("completer") {
            that.interest = it
            that.input.on("keyup").select { input.value }.where { it.length > 0 }.throttle(750).distinctUntilChanged().subscribe {
                if(!that.silent) {
                val search: String = it
                //val f = RelationFilter(galaxy.descriptor, "OR", Array<PropertyFilter>(projections.size) { PropertyFilter(galaxy.descriptor, "LIKE", projections[it], "$search.*") })
                //console.log("COMPLETER START: ${JSON.stringify(f.serialise())}")
                that.list.removeChildren()
                candidates.clear()
                    val fs = createFilter(search)
                    console.log("filter: $fs")
                    that.interest?.filterJson(APP!!.filterParser!!.parse<Json>(fs)!!)
                }
            }
            val kd = input.on("keydown")
            kd.where { console.log(it); val ke = it as KeyboardEvent; console.log(ke.keyCode); ke.keyCode in listOf(13, 38,40) }.subscribe {
                val ke = it as KeyboardEvent

                if (ke.keyCode in listOf(13)) {
                    if(that.selected!=null) {
                       that.select()
                    }
                } else {
                    var idx = -1
                    if (that.selected!=null) {
                        for(i in 0..(that.candidates.size()-1)) {
                            if(that.candidates[i]==that.selected) idx = i
                        }
                    }
                    console.log("indexof: $idx kc: ${ke.keyCode}")
                    if(idx<0) idx = 0
                    else if(ke.keyCode==(38 as Long)) {
                        if(idx==0) idx=that.candidates.size()-1
                        else idx=idx-1
                    } else if(ke.keyCode==(40 as Long)) {
                        if(idx+1>=that.candidates.size()) idx = 0
                        else idx=idx+1
                    }
                    console.log("selected idx $idx")
                    if(idx>=0 && idx<that.candidates.size()) that.selected = that.candidates[idx]
                    console.log(that.selected?:"")
                    that.list.root.childNodes.forEach {
                        val o = KIDATAget(it as HTMLElement, "entity")
                        if(o=="${that.selected}") it.classList.add("selected")
                        else it.classList.remove("selected")
                    }
                }
                //if(that.selected!=null) input.value = that.selected!![that.label].toString()

            }
            that.interest?.eager = true
            that.interest?.on {
                console.log(it)
                console.log("event looks at list#${that.list.id}")
                when(it) {
                    is InterestLoadEvent -> {
                        console.log("COMPLETE LOAD ${it.entity.id}")
                        console.log(it)
                        if(!that.candidates.contains(it.entity.id)) {
                            that.candidates.add(it.entity.id)
                        }

                        val e = it.entity
                        that.list.root.childNodes.filter { it.nodeName.toLowerCase() == "span" }.forEach {
                            val did = KIDATAget(it as HTMLElement, "entity")
                            console.log("$did == ${e.id} = ${did == "${e.id}"}")
                            if(did == "${e.id}") {
                                it.textContent = e[label]?.toString()?:did
                            }
                        }

                    }
                    is InterestOrderEvent -> {
                        that.candidates.clear()
                        that.list.removeChildren()
                        console.log("COMPLETE ORDER ${that.root.id}:${dl.id} ${that.list.id}")
                        console.log(it.order)
                        it.order.forEach { that.candidates.add(it) }
                        console.log(that.candidates)
                        that.list.removeChildren()
                        for(i in 0..(that.candidates.size()-1)) {
                            that.list.span {
                                console.log("ADDING $i")
                                console.log(this)
                                val eid = that.candidates[i]
                                data("entity", "${eid}")
                                data("order", "$i")
                                val entity = that.interest?.entity(eid)
                                if(entity?.state!=EntityState.LOADED) {
                                    textContent = "$eid"+entity?.state
                                } else
                                    textContent = entity?.get(that.label)?.toString()?:"BAD"

                                if(eid==that.selected) addClass("selected")
                                on("click") {
                                    that.selected = eid
                                    console.log("select $eid")
                                    that.select()
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    public fun onBlur(cb:(Event)->Unit) : Disposable = input.on("blur", cb)
}


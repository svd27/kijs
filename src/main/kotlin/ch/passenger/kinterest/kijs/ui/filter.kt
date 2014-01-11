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

/**
 * Created by svd on 10/01/2014.
 */
class FilterComponent(val interest: Interest, id: String = BaseComponent.id()) : Component<HTMLDivElement>(id) {

    override fun node(): HTMLDivElement {
        val d = Div()
        d.addClasses("filter", interest.galaxy.descriptor.entity)


        return d.root
    }
}

class FilterRow(val interest: Interest, val property: PropertyDescriptor, id: String = BaseComponent.id()) : Component<HTMLDivElement>(id) {
    var value: Tag<*> = TextInput()
    var op: String = "EQ"
    val row: Div = Div()

    override fun node(): HTMLDivElement {
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
        return row.root
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
    val candidates : MutableList<Entity> = ArrayList()
    var selected : Entity? = null
    val subject : Subject<Entity?> = Subject()


    fun on(cb:(Entity?)->Unit) : Disposable = subject.subscribe(cb)

    override fun node(): HTMLDivElement {
        val d = Div()
        d.addClass("completer")
        input.addClasses("completerinput", galaxy.descriptor.entity)
        list.addClass("completerlist")
        d+input
        d+list
        return d.root
    }

    {
        console.log("Completer ${galaxy.descriptor.entity} ${label} proj: ${projections.reduce("") {(i, n) -> "$i $n" }}")
        val that = this
        val input = this.input
        val projections = this.projections
        val galaxy = this.galaxy
        val dl = list
        val candidates = that.candidates
        val selected = that.selected
        galaxy.create("completer") {
            that.interest = it
            input.on("keyup").select { input.value }.where { it.length > 0 }.throttle(750).distinctUntilChanged().subscribe {
                val search: String = it
                val f = RelationFilter(galaxy.descriptor, "OR", Array<PropertyFilter>(projections.size) { PropertyFilter(galaxy.descriptor, "LIKE", projections[it], "$search.*") })
                console.log("COMPLETER START: ${JSON.stringify(f.serialise())}")
                dl.removeChildren()
                candidates.clear()
                that.interest?.filter(f)
            }
            val kd = input.on("keydown")
            kd.where { console.log(it); val ke = it as KeyboardEvent; console.log(ke.keyCode); ke.keyCode in listOf(13, 38,40) }.subscribe {
                val ke = it as KeyboardEvent
                console.log(ke)

                if (ke.keyCode in listOf(13)) {
                    if(that.selected!=null) {
                        that.input.value = that.selected!![that.label].toString()
                        subject.onNext(that.selected)
                        dl.removeChildren()
                    }
                } else {
                    var idx = -1
                    if (that.selected!=null) {
                        for(i in 0..(candidates.size()-1)) {
                            if(candidates[i].id==that.selected?.id) idx = i
                        }
                    }
                    console.log("indexof: $idx kc: ${ke.keyCode}")
                    if(idx<0) idx = 0
                    else if(ke.keyCode==(38 as Long)) {
                        if(idx==0) idx=candidates.size()-1
                        else idx=idx-1
                    } else if(ke.keyCode==(40 as Long)) {
                        if(idx+1>=candidates.size()) idx = 0
                        else idx=idx+1
                    }
                    console.log("selected idx $idx")
                    if(idx>=0 && idx<candidates.size()) that.selected = candidates[idx]
                    console.log(that.selected?:"")
                    dl.root.childNodes.forEach {
                        val o = KIDATAget(it as HTMLElement, "entity")
                        console.log("compare $o = ${that.selected?.id}")
                        if(o=="${that.selected?.id}") it.classList.add("selected")
                        else it.classList.remove("selected")
                    }
                }
                //if(that.selected!=null) input.value = that.selected!![that.label].toString()

            }
            that.interest?.eager = true
            that.interest?.on {
                val ai = it.interest

                console.log("completer")
                when(it) {
                    is InterestLoadEvent -> {
                        console.log(it)
                        if(!candidates.contains(it.entity)) {
                            candidates.add(it.entity)
                        }

                        dl.removeChildren()
                        for(i in 0..(candidates.size()-1)) {
                            dl.span {
                                val entity = candidates[i]
                                data("entity", "${entity.id}")
                                data("order", "$i")
                                textContent = entity[that.label].toString()

                                if(entity==selected) addClass("selected")
                            }
                        }
                    }
                }

            }
        }
    }

    public fun onBlur(cb:(Event)->Unit) : Disposable = input.on("blur", cb)
}


class Completer(val galaxy: Galaxy, val projections: Array<String>, val label: String, id: String = BaseComponent.id()) : Component<HTMLDivElement>(id) {
    var interest: Interest? = null
    val input: TextInput = TextInput();
    val list: DataList = DataList()


    override fun node(): HTMLDivElement {
        val d = Div()
        input.addClasses("completer", galaxy.descriptor.entity)
        input.att("list", list.id)
        d+input
        d+list
        return d.root
    }

    {
        console.log("Completer ${galaxy.descriptor.entity} ${label} proj: ${projections.reduce("") {(i, n) -> "$i $n" }}")
        val that = this
        val input = this.input
        val projections = this.projections
        val galaxy = this.galaxy
        val dl = list
        galaxy.create("completer") {
            that.interest = it
            input.on("keyup").select { input.value }.where { it.length > 2 }.throttle(750).distinctUntilChanged().subscribe {
                val search: String = it
                val f = RelationFilter(galaxy.descriptor, "OR", Array<PropertyFilter>(projections.size) { PropertyFilter(galaxy.descriptor, "LIKE", projections[it], "$search.*") })
                console.log("COMPLETER START: ${JSON.stringify(f.serialise())}")
                that.interest?.filter(f)
            }
            that.interest?.eager = true
            that.interest?.on {
                val ai = it.interest
                console.log("completer $it")
                when(it) {
                    is InterestLoadEvent -> {
                        dl.removeChildren()
                        val did = "${it.entity.id}"

                        val fd = dl.root.childNodes.any {
                            val on = it as HTMLOptionElement
                            on.value == did
                        }
                        if (!fd) {
                            dl.option("$did", "${it.entity[that.label]}") { }
                        }
                        that.input.att("list", that.list.id)
                    }
                }

            }
        }
    }
}


package ch.passenger.kinterest.kijs.ui

import ch.passenger.kinterest.kijs.model.Universe
import ch.passenger.kinterest.kijs.dom.*
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.Element
import kotlin.browser.document
import ch.passenger.kinterest.kijs.forEach
import rx.js.Disposable
import java.util.HashSet
import ch.passenger.kinterest.kijs.model.Interest
import org.w3c.dom.HTMLElement
import ch.passenger.kinterest.kijs.model.Entity
import ch.passenger.kinterest.kijs.model.PropertyDescriptor
import ch.passenger.kinterest.kijs.model.EntityDescriptor
import java.util.HashMap
import ch.passenger.kinterest.kijs.model.InterestUpdateEvent
import ch.passenger.kinterest.kijs.model.InterestOrderEvent
import ch.passenger.kinterest.kijs.APP
import ch.passenger.kinterest.kijs.filter
import ch.passenger.kinterest.kijs.indexOf
import ch.passenger.kinterest.kijs.none
import ch.passenger.kinterest.kijs.model.InterestLoadEvent
import ch.passenger.kinterest.kijs.model.PropertyFilter
import java.util.ArrayList
import rx.js.Subject
import ch.passenger.kinterest.kijs.map
import ch.passenger.kinterest.kijs.notNulls
import moments.Moment
import ch.passenger.kinterest.kijs.model.InterestConfigEvent
import ch.passenger.kinterest.kijs.model.SortKey
import ch.passenger.kinterest.kijs.model.SortDirection
import ch.passenger.kinterest.kijs.any
import ch.passenger.kinterest.kijs.firstThat
import org.w3c.dom.events.MouseEvent

import kotlin.js.dom.html.HTMLSelectElement
import ch.passenger.kinterest.kijs.model.EntityTemplate

/**
 * Created by svd on 08/01/2014.
 */

abstract class Component<T : HTMLElement>(id: String = BaseComponent.id(), name:String="div") : FlowContainer<T>(name, id) {

    override fun dispose() {
        disposables.forEach { dispose() }
    }
    override val disposables: MutableSet<Disposable> = HashSet()

    fun createElement(name: String): T = document.createElement(name) as T
}

class UniverseMenu(val universe: Universe, id: String = BaseComponent.id()) : Component<HTMLDivElement>(id) {
    override fun initialise(div:HTMLDivElement) {
        val div = this
        val that = this
        div.anchor {
            textContent = "DUMP"
            click {
                val ax = Ajax("${APP!!.HTTP}${APP!!.base}/dump")
                ax.asObservabe().subscribe {
                    console.log(JSON.parse<Json>(it))
                }
                ax.start()
            }
        }
        universe.galaxies.values().forEach {
            val g = it

            div.div {
                val di = Div("interests")
                var a: Anchor? = null
                addClass(it.descriptor.entity)
                span { +it.descriptor.entity }
                val ti = input {
                    root.defaultValue = "name";
                    val ti = this; change { a?.enabled(ti.value.length > 0) }
                }
                div {
                    addClass("fa-hover");

                    anchor() {
                        +"Bag"
                        click {
                            g.create("bag") {
                                val ai = it
                                ai.buffer(0, 10)
                                val tbl = that.crtTable(ai)
                                val bd = di.div {
                                    +"${ai.id}--${ai.name}"
                                    plus(tbl)
                                    if(ai.galaxy.descriptor.entity=="DiaryOwner") {
                                        val pit = ai.galaxy.descriptor.properties.values().filter { !it.nullable && it.datatype.endsWith("String") }.map { it.property }
                                        val c = pit.count()
                                        val ait = pit.iterator()
                                        val pa = Array<String>(c) {ait.next()}
                                        val lbl = ai.galaxy.descriptor.properties.values().firstThat { it.datatype.endsWith("String") && it.unique }?.property
                                        val cc = CustomCompleter(ai.galaxy, pa, lbl?:"id")
                                        cc.on {
                                            console.log("COMPLETED")
                                            val e = it; if(e!=null) {
                                            console.log("ADDING: ")
                                            console.log(e)
                                            ai.addEntity(e)
                                        } }
                                        plus(cc)
                                    }

                                    val rem = anchor {
                                        +"Remove"
                                        addClass("removeentity")
                                        enabled(false)
                                        click {
                                            tbl.selected.forEach { ai.remove(ai.entity(it)) }
                                        }
                                    }

                                    anchor {
                                        +"Clear"
                                        addClass("clearinterest")
                                        click { ai.clear() }
                                    }

                                    tbl.onSelection { rem.enabled(it.count()>0 ) }
                                }
                            }
                        }
                    }
                    a = anchor() {
                    +"New"; icon { addClasses("fa", "fa-plus-square-o") }
                    enabled(false)
                    click {
                        g.create(ti.value) {
                            val interest = it
                            di.div {
                                addClass("interest")
                                data("interest", "${it.id}")
                                span { +it.id.toString() }
                                span { +it.name }
                                anchor {
                                    addClass("createentity")
                                    on("click") {
                                        val ee = GenericEntityEditor(interest, true)

                                        di+ ee;
                                    }
                                    textContent = "Create"
                                }

                                val tbl = that.crtTable(it)
                                val ai = it
                                plus(tbl)
                                it.buffer(0, 5)
                                it.filter(PropertyFilter(it.galaxy.descriptor, "GTE", "id", 0))
                                div {
                                    addClass("interestdetail")

                                    val ee = GenericEntityEditor(tbl.interest)
                                    tbl.onSelection {
                                        if(it.iterator().hasNext()) {
                                            ee.entity = ai.entity(it.iterator().next())
                                        } else {
                                            ee.entity = ai.galaxy.NOTLOADED
                                        }
                                    }
                                    this+ ee
                                }
                            }
                        }
                    }
                }
                }
                this + di
            }
        }
    }

    fun crtTable(ai:Interest) : InterestTable {
        val tbl = InterestTable(ai)

        ai.galaxy.descriptor.properties.values().forEach {
            tbl.columns[it.property] = InterestTableColumn(it.property, ai)
        }
        val cc = InterestTableColumn("commit", ai, { CommitRenderer(ai) })
        cc.headerRenderer.label = "Commit"
        tbl.columns["commit"] = cc
        return tbl
    }
}


abstract class EntityEditor<T:HTMLElement>(val interest:Interest, id:String=BaseComponent.id(), name:String="div") : Component<T>(id, name) {
    private var eid : String = ""
    var entity : Entity?
      get() = if(eid.isNotEmpty()) interest.entity(eid) else null
      set(v) {if(v?.id!=eid) {eid=v?.id?:""; update()} }

    init {
        disposables.add(interest.on {
            when(it) {

                is InterestLoadEvent -> { console.log("IE: my: ${entity?.id} <=> ${it.entity.id}"); if(it.entity.id == entity?.id) update() }
                is InterestUpdateEvent -> {console.log("IE: my: ${entity?.id} <=> ${it.entity.id}"); if(it.entity.id == entity?.id) update()}
            }
        })
    }

    abstract fun update()
}

open class GenericEntityEditor(val interest:Interest, val creator:Boolean=false, id:String=BaseComponent.id()) : Component<HTMLDivElement>(id) {
    var entity:Entity=if(creator) EntityTemplate(interest.galaxy.descriptor) else interest.galaxy.NOTLOADED
        set(v) {
            if(field.id!=v.id) {
                field = v
                if(creator) {
                    v.descriptor.properties.values().forEach {
                        if(it.floaty && v[it.property]==null) {
                            v[it.property] = 0.0
                        }
                        if(it.scalar && v[it.property]==null) v[it.property] = 0
                        if(it.daty && v[it.property]==null) v[it.property] = moments.moment()
                        if(it.datatype=="java.lang.String" && (v[it.property]?.toString()?.isEmpty()?:true)) v[it.property] = "---"
                    }
                }
                updaters.forEach { it(v) }
            }
        }
    var header : Div? = null
    var body : Div? = null
    var footer : Div? = null
    val updaters : MutableList<(Entity)->Unit> = ArrayList();
    var commitRenderer : CommitRenderer? = null

    fun update(e:Entity) {
        if(e.id!=entity.id) return
        updaters.forEach { it(e) }
    }

    init {
        val e = entity
        if(e is EntityTemplate && creator) e.updateHook = {update(it)}
        if(creator) {
            val v = e
            e.descriptor.properties.values().forEach {
                if(it.floaty && v[it.property]==null) {
                    v[it.property] = 0.0
                }
                if(it.scalar && v[it.property]==null) v[it.property] = 0
                if(it.daty && v[it.property]==null) v[it.property] = moments.moment()
                if(it.datatype=="java.lang.String" && (v[it.property]?.toString()?.isEmpty()?:true)) v[it.property] = "---"
            }
        }

    }

    init {
        interest.on {
            when(it) {
                is InterestUpdateEvent -> update(it.entity)
                is InterestLoadEvent -> update(it.entity)
            }
        }
    }

    override fun initialise(node:HTMLDivElement) {
        val desc = interest.galaxy.descriptor
        val that = this
        val en = entity
        var adl : DL? = null
        val d = this

        d.addClass("entityeditor")
        d.addClass("entitycreator")
        d.div {
            that.header = this
            that.header?.addClass("header")
            that.header?.dl {
                adl = this
                that.addHeaderTerm(adl!!, "Entity", {it.descriptor.entity})
                that.addHeaderTerm(adl!!, "ID", {"${it.id}"})
                that.addHeaderTerm(adl!!, "DIRTY", {(if(it.dirty)"DIRTY" else "CLEAN")})
                that.addHeaderTerm(adl!!, "CONFLICTED", {(if(it.hasConflict)"CONFLICT" else "CLEAN")})
            }
        }
        d.div {
            that.body=this
            that.body?.addClass("body")
            that.interest.galaxy.descriptor.properties.values().forEach {
                val pd = it;
                that.body?.dl {
                dt { +pd.property }
                dd {
                    val pr = PropertyRendererEditor.bestFor(that.interest, pd.property, that.creator)
                    this+ pr
                    pr.entity=that.entity
                    that.updaters.add { pr.entity=it }
                }
            } }
        }

        footer = d.div {
            that.footer = this
            that.footer?.addClass("footer")
            val cr = CommitRenderer(that.interest, that.creator)
            cr.entity=that.entity
            that.commitRenderer = cr
            that.updaters.add { cr.entity=it }
            plus(cr)
            if(that.creator) cr.subject.subscribe { that.remove() }
            that.updaters.add {
                cr.update()
            }
        }
    }

    fun addHeaderTerm(dl:DL, label:String, retrieve:(Entity)->String) {
        dl.dt {
            +label
        }
        val that = this
        dl.dd {
            +retrieve(that.entity)
            val dd = this
            that.updaters.add { dd.textContent = retrieve(it)}
        }

    }
}

class Tab(val label:Tag<*>, val content:Tag<*>, id:String=BaseComponent.id()) : Component<HTMLDivElement>(id) {

    override fun initialise(n:HTMLDivElement) {
        val d = this
        d+label
    }
}

class Tabber(val gesture:String="click", id:String=BaseComponent.id()) : Component<HTMLDivElement>(id) {
    val tabs : MutableMap<String,Tab> = HashMap()
    var selected: Tab? = null
    val labelPane = Div()
    val contentPane = Div()

    override fun initialise(n:HTMLDivElement) {
        val d = this
        d.addClass("tabber")
        labelPane.addClass("tablabels")
        contentPane.addClass("tabcontent")
        d+labelPane
        d+contentPane
    }

    fun addTab(t:Tab) {
        tabs[t.id] = t
        labelPane+t
        t.content.hide()
        contentPane+t.content
        t.on(gesture) {
            select(t)
        }
        if(selected==null) {
            select(t)
        }
    }

    fun select(t:Tab) {
        if(selected!=null) {
            selected!!.removeClass("selected")
            selected!!.content.hide()
        }
        selected = t
        selected!!.addClass("selected")
        selected!!.content.show()
    }
}


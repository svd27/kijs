package ch.passenger.kinterest.kijs.ui

import ch.passenger.kinterest.kijs.model.*
import kotlin.js.dom.html.*
import rx.js.Subject
import ch.passenger.kinterest.kijs.*
import ch.passenger.kinterest.kijs.dom.*
import org.w3c.dom.events.MouseEvent
import rx.js.Disposable
import java.util.HashMap
import java.util.ArrayList
import java.util.HashSet


/**
 * Created by svd on 10/01/2014.
 */
class InterestTableColumn(val property: String, val interest: Interest, val renderer: (Interest) -> EntityRendererEditor<*> = { PropertyRendererEditor.bestFor(it, property) }, val headerRenderer: HeaderRenderer = HeaderRenderer(interest, property))

class InterestTable(val interest: Interest, id: String = BaseComponent.id()) : Component<HTMLDivElement>(id) {
    var tableBody: TableBody? = null
    val columns: MutableMap<String, InterestTableColumn> = HashMap()
    val colorder: MutableList<String> = ArrayList()
    private val selection : MutableSet<Long> = HashSet()
    public  val selected : Set<Long> get() = selection
    val selector : Subject<Iterable<Long>> = Subject()
    var offset : Int = interest.offset
    var committer : Boolean = false
      set(v) {
          $committer = v
          if(v) {
              colorder.add("commit")
              val cr = {(it:Interest) -> console.log("COMMIT"); CommitRenderer(it)}
              columns["commit"] = InterestTableColumn("commit", interest, cr)
          }
      }


    fun addActions(name:String="Actions", init:(Interest)->ActionListRenderer) {
        colorder.add(name)
        columns[name] = InterestTableColumn(name, interest, init)
    }

    fun createColumns() {
        colorder.forEach {
            val c = it
            if(columns[it]==null)
            columns[it] = InterestTableColumn(it, interest, {PropertyRendererEditor.bestFor(it, c)})
        }
    }

    fun label(col:String, label:String) {
        columns[col]?.headerRenderer?.label = label
    }

    fun onSelection(cb:(Iterable<Long>)->Unit) : Disposable = selector.subscribe(cb)
    fun setSelection(sel:Iterable<Long>) {
        val nsel = setOf(sel)
        if(nsel.same(selection)) return
        selection.clear()

        selection.addAll(sel)
        tableBody?.rows?.forEach {
            val de = it.data("entity")
            if(de !=null) {
                val aid = safeParseInt(de) as Long
                if(sel.any { it==aid }) it.addClass("selected")
                else it.removeClass("selected")
            } else it.removeClass("selected")
        }
        selector.onNext(selection)
    }

    fun reduce(cols: Set<String>) = columns.keySet().filter { it !in cols }.forEach { columns.remove(it) }

    override fun initialise(n: HTMLDivElement) {
        val root = this
        val that = this
        root.table {
            header {
                tr {
                    val tr = this
                    if (that.colorder.isEmpty()) {
                        that.columns.keySet().forEach { that.colorder.add(it) }
                    }
                    that.colorder.map { that.columns[it] }.notNulls().forEach {
                        val col = it
                        tr.th {
                            addClass(col.property)
                            this + (col.headerRenderer)
                        }
                    }
                }
            }
            body {
                that.tableBody = this
                val body = this
                that.interest.on {
                    var rows = body.rows
                    when(it) {
                        is InterestOrderEvent -> {
                            val event = it
                            if (rows.size() > it.order.size) {
                                console.log("order sz: ${it.order.size} < ${rows.size()} removing: ${it.order.size}..${(rows.size() - 1)}")
                                for (i in (rows.size() - 1) to it.order.size) {
                                    //console.log("dead wood $i")
                                    body.remove(i)
                                }
                            }
                            //console.log("IT ORDER SIZE: ${it.order.size} ROWS: ${rows.size()}")
                            for (idx in 0..(it.order.size - 1)) {
                                if (rows.size() <= idx) {
                                    body.tr {
                                        //console.log("CREATE ROW $idx ${rows.size()}")
                                        val tr = this
                                        data("order", "$idx")
                                        on("click") {
                                            val oldsel = HashSet<Long>()
                                            oldsel.addAll(that.selection)
                                            val e = it as MouseEvent
                                            val ord = tr.data("order")
                                            if(ord!=null) {
                                                val sel = safeParseInt(ord)

                                                if(!e.shiftKey && !e.ctrlKey && !e.metaKey) {
                                                    that.selection.clear()
                                                    body.rows.forEach {
                                                        it.removeClass("selected")
                                                    }
                                                    if(sel!=null && sel>=0 && sel<that.interest.order.size()) {
                                                        tr.addClass("selected")
                                                        that.selection.add(that.interest.order[sel])
                                                    }
                                                } else if(e.metaKey) {
                                                    if(sel!=null && sel>=0 && sel<that.interest.order.size()) {
                                                        val id = that.interest.order[sel]
                                                        if(that.selection.size()>1 && that.selection.contains(id)) {
                                                            that.selection.remove(id)
                                                            tr.removeClass("selected")
                                                        } else {
                                                            that.selection.add(id)
                                                            tr.addClass("selected")
                                                        }
                                                    }
                                                }
                                                //console.log("selection now: ${that.selection}")

                                                if(!oldsel.same(that.selected))
                                                that.selector.onNext(that.selection)
                                            }

                                        }
                                        that.colorder.map {that.columns[it]}.notNulls().forEach {
                                            tr.td {
                                                val entity = event.interest[idx]
                                                val ar = it.renderer(event.interest)
                                                if (ar is EntityRendererEditor<*>)
                                                    ar.entity = entity
                                                renderer = ar
                                            }
                                        }
                                    }
                                }
                            }
                            for (row in body.rows) {
                                val idx = parseInt(row.data("order")!!)
                                if (idx >= event.interest.order.size()) {
                                    console.warn("$idx >= ${event.interest.order.size()}")
                                } else {
                                    val entity = event.interest[idx]
                                    row.data("entity", "${entity?.id}")
                                    if(entity?.dirty?:false) row.addClass("dirty") else row.removeClass("dirty")
                                    row.cells.forEach {
                                        val r = it.renderer
                                        when(r ) {
                                            is EntityRendererEditor<*> -> if (r.entity?.id != entity?.id) r.entity = entity else r.update()
                                            else -> r?.update()
                                        }
                                    }
                                }
                            }
                        }
                        is InterestLoadEvent -> {
                            val event = it as InterestLoadEvent
                            val idx = it.idx
                            if(body.rows.size()>idx) {
                            val row = body.rows[idx]
                            row.cells.forEach {
                                val cr = it.renderer
                                if (cr is EntityRendererEditor<*>) {
                                    cr.entity = event.entity
                                    cr.update()
                                }
                            }
                            row.show("table-row")
                            }
                        }
                        is InterestUpdateEvent -> {
                            //console.log("UPDATE...")
                            //console.log(it)
                            if (it.idx<rows.size()) {
                                val row = rows[it.idx]
                                if (it.entity.dirty) row.addClass("dirty") else row.removeClass("dirty")
                                val e = it as InterestUpdateEvent
                                row.cells.forEach {
                                    val r = it.renderer
                                    if (r is EntityRendererEditor<*>) {
                                        if (r.wants(e)) r.update()
                                    }
                                }
                            }
                        }
                        is InterestConfigEvent -> {
                            if(that.interest.offset!=offset) {
                                that.selection.clear()
                                body.rows.forEach { it.removeClass("selected") }
                                that.selector.onNext(that.selection)
                            }
                        }
                    }

                }
            }
        }

        root + Pager(interest)
    }
}

class Pager(val interest: Interest, id: String = BaseComponent.id()) : Component<HTMLDivElement>(id) {

    override fun initialise(n: HTMLDivElement) {
        val d = this
        val that = this
        d.addClass("pager")
        val first = d.anchor {
            addClass("first")
            enabled(false)
            on("click") {
                that.interest.first()
            }
        }

        val prev = d.anchor {
            addClass("previous")
            enabled(false)
            on("click") {
                that.interest.previous()
            }
        }

        val next = d.anchor {
            addClass("next")
            enabled(false)
            on("click") {
                console.log("NEXT")
                that.interest.next()
            }
        }

        d.anchor {
            addClass("cfg")
            on("click") {
                d.input {
                    val inp = this
                    inp.value = "${that.interest.limit}"
                    on("blur") {
                        inp.remove()
                    }
                    on("change") {
                        val v = parseInt(inp.value)
                        that.interest.buffer(that.interest.offset, v)
                    }
                }
            }
        }

        d.labelledInput("Filter:") {
            val ti = this
            console.log(ti)
            ti.on("change").subscribe {
                console.log("CHANGE")
                that.filter(ti)
            }

        }



        update(first, prev, next)

        interest.on {
            if (it is InterestConfigEvent) {
                console.log("UPDATE PAGER")
                update(first, prev, next)
            }
        }

    }

    fun filter(ti:TextInput) {
        if(ti.value.isEmpty()) return
        try {
            val f = APP?.filterParser?.parse<Json>(ti.value)!! as Json
            ti.removeClass("error")
            console.log(f)
            f.set("entity", interest.galaxy.descriptor.entity)
            interest.filterJson(f)
        } catch(e: Exception) {
            ti.addClass("error")
            console.error(e)
        }
    }

    fun update(first: Anchor, prev: Anchor, next: Anchor) {
        if (interest.offset > 0) {
            enable(first)
            enable(prev)
        }
        else {
            disable(first); disable(prev)
        }
        if (interest.estimated>interest.size)
            enable(next)
        else disable(next)
    }

    fun enable(a: Anchor) {
        a.removeClass("disabled")
        a.enabled(true)
    }

    fun disable(a: Anchor) {
        a.addClass("disabled")
        a.enabled(false)
    }
}
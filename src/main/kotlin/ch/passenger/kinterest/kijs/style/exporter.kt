package ch.passenger.kinterest.kijs.style

import js.dom.html.HTMLDivElement
import js.dom.html.document
import ch.passenger.kinterest.kijs.dom.*
import ch.passenger.kinterest.kijs.ui.Component
import ch.passenger.kinterest.kijs.ui.SelectOne
import ch.passenger.kinterest.kijs.ui.TableBody
import ch.passenger.kinterest.kijs.forEach
import ch.passenger.kinterest.kijs.model.ALL
import ch.passenger.kinterest.kijs.APP
import js.dom.html.HTMLElement
import ch.passenger.kinterest.kijs.model.EntityTemplate
import ch.passenger.kinterest.kijs.ui.TextInput
import ch.passenger.kinterest.kijs.ui.InterestTable
import ch.passenger.kinterest.kijs.listOf
import ch.passenger.kinterest.kijs.model.InterestLoadEvent
import js.dom.html.HTMLInputElement
import ch.passenger.kinterest.kijs.model.Entity
import ch.passenger.kinterest.kijs.model.EntityState
import java.util.HashMap
import js.lastIndexOf

/**
 * Created by svd on 18/01/2014.
 */
class CSSExporter() : Component<HTMLDivElement>() {
    val gs = ALL.galaxies["CSSStylesheet"]!!
    var sheets : SelectOne? = null
    var tbl : TableBody? = null
    var sname : TextInput? = null
    //val managers : MutableMap<Long,StyleManager> = HashMap()

    override fun initialise(n: HTMLDivElement) {
        super<Component>.initialise(n)
        val that = this
        sheets = this.select {
            val that = this
            document.styleSheets.forEach {
                if(it.href!=null)
                that.option(it.href, it.href.substring(it.href.lastIndexOf("/")..it.href.length)) {}
            }
            selected = 0
        }
        sheets!!.onReady {
            val et = that.sheets!!.root as EventSource
            et.addEventListener("change") {
                console.log("sel: ${that.sheets!!.selectedValue}")
                if(that.sheets!!.selectedValue!=null) {
                    console.log("sel: ${that.sheets!!.selectedValue}")
                    renderSheet(that.sheets!!.selectedValue!!)
                }
            }
        }
        this.labelledInput("Stylesheet") {
            value = "mystylesheet"
            that.sname = this
        }
        this.anchor {
            textContent = "Create"
            click {
                that.createSheet()
            }
        }
        this.div {
            val d = this
            val sel = d.select {

            }
            that.gs.create("sheets") {
                it.on {
                    when(it) {
                        is InterestLoadEvent -> {
                            if (!sel.contains("${it.entity.id}")) {
                                sel.option("${it.entity.id}", "${it.entity["name"]}") { }
                                sel.selectedValue = "${it.entity.id}"
                            }
                        }
                    }
                }
                val js = APP!!.filterParser!!.parse<Json>("id >= 0")
                it.eager = true
                it.buffer(0, 3)
                it.filterJson(js!!)
                d.anchor {
                    textContent = "Export"
                    click {
                        val id = sel.selectedValue
                        if(id!=null) {
                            val sheet = that.gs.get(safeParseInt(id) as Long)
                            if(sheet.state==EntityState.LOADED) {
                                that.export(sheet)
                            }
                        }
                    }
                }
            }
        }
        this.table() {
            header {
                tr {
                    th {
                        checkbox {
                        val chk = this
                        on("change") {
                        val st = chk.checked
                        that.checkRows(st)
                    }}}
                    //th {textContent = "Index"}
                    th {textContent = "Selector"}
                    th {textContent = "Style Text"}
                    th {textContent = "Styles"}
                }
            }
            that.tbl = body {

            }
        }.onReady {
            val sel = that.sheets?.selectedValue
            if(sel != null) {
                renderSheet(sel)
            }
        }

    }

    fun renderSheet(sel:String) {
        tbl?.removeChildren()
        val that = this
        document.styleSheets.forEach {
            if(it.href==sel) {
                it.cssRules.forEach {
                    val rule = it
                    tbl?.tr {
                        td {
                            checkbox {}
                        }
                        td {
                            textContent = rule.selectorText
                        }
                        td {
                            if(rule.`type` == 1) {
                                val style = rule as CSSStyleRule
                                textContent = style.style.cssText
                            }
                        }
                        td {
                            val td = this
                            if(rule.`type` == 1) {
                                val style = rule as CSSStyleRule
                                td.table {
                                    val t =this
                                    t.header {
                                        val h= this
                                        h.tr {
                                        th {textContent = "Name"}
                                        th {textContent = "Value"}
                                        }
                                    }
                                    t.body {
                                        val b = this
                                        style.style.forEach {
                                            (n,v) -> b.tr {
                                            td {textContent = n}
                                            td {textContent = v}
                                        }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    fun createSheet() {
        val sel = sheets!!.selectedValue
        var sheet : StyleSheet? = null
        if(sel!=null)  {
            document.styleSheets.forEach {
                if(it.href==sel) {
                    sheet = it
                }
            }
        }
        if(sheet==null) return
        val e = EntityTemplate(gs.descriptor)
        e["name"] = sname!!.value
        gs.createEntity(e)
    }

    fun checkRows(st:Boolean) {
        tbl?.rows?.forEach {
            val r = it
                val c = r.cells[0]
                c.root.childNodes.forEach {
                    val h = it as HTMLElement
                    if(h.nodeType ==  1.toShort() && h.getAttribute("type")=="checkbox") {
                        val inp = h as HTMLInputElement
                        inp.checked = st
                    }
                }

        }
    }

    fun export(sheet:Entity) {
        tbl?.rows?.forEach {
            val r = it
            val c = r.cells[0]
            var chk = false
            c.root.childNodes.forEach {
                val h = it as HTMLElement
                if(h.nodeType ==  1.toShort() && h.getAttribute("type")=="checkbox") {
                    val inp = h as HTMLInputElement
                    chk = inp.checked
                }
            }
            if(chk) {
                val selector = r.cells[1].textContent
                val css = r.cells[2].textContent
                gs.call(sheet.id, "addRule", array(selector, css), {})
                r.remove()
            }
        }
    }
}
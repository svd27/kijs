package ch.passenger.kinterest.kijs.style

import ch.passenger.kinterest.kijs.model.Entity
import ch.passenger.kinterest.kijs.model.ALL
import ch.passenger.kinterest.kijs.APP
import java.util.HashMap
import ch.passenger.kinterest.kijs.model.InterestOrderEvent
import ch.passenger.kinterest.kijs.forEach
import ch.passenger.kinterest.kijs.dom.*
import ch.passenger.kinterest.kijs.model.InterestUpdateEvent
import ch.passenger.kinterest.kijs.ui.BaseComponent
import ch.passenger.kinterest.kijs.ui.Component
import ch.passenger.kinterest.kijs.ui.InterestTable
import ch.passenger.kinterest.kijs.map
import ch.passenger.kinterest.kijs.makeString
import ch.passenger.kinterest.kijs.ui.ActionListRenderer
import ch.passenger.kinterest.kijs.ui.ActionComponent
import org.w3c.dom.events.Event
import ch.passenger.kinterest.kijs.ui.EntityEditor
import ch.passenger.kinterest.kijs.ui.GenericEntityEditor
import ch.passenger.kinterest.kijs.ui.TextInput
import ch.passenger.kinterest.kijs.firstThat
import ch.passenger.kinterest.kijs.model.Interest
import ch.passenger.kinterest.kijs.model.EntityState
import ch.passenger.kinterest.kijs.ui.Label
import ch.passenger.kinterest.kijs.ui.CompleterRenderEdit
import ch.passenger.kinterest.kijs.ui.CustomCompleter
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.css.CSSStyleRule
import org.w3c.dom.css.CSSStyleSheet
import org.w3c.dom.css.StyleSheet
import kotlin.browser.document

/**
 * Created by svd on 19/01/2014.
 */
class StyleManager(val esheet:Entity) {
    private val gsheet = ALL.galaxies["CSSStylesheet"]!!
    private val grule = ALL.galaxies["CSSStyleRule"]!!
    private val ruleMap : MutableMap<String,Int> = HashMap();
    private var islive = false
    private var sheet : CSSStyleSheet? = null
    private var interest : Interest? = null;

   init {
        val that = this
        ALL.galaxies["CSSProperty"]!!.create("${esheet["name"]}properties") {
            interest = it
            it.eager = true
            it.on {
                console.log("StyleManager ${it}")
                val ev = it
                if (islive) {
                    when(ev) {
                        is InterestOrderEvent -> {
                            ev.order.forEach {
                                that.gsheet.call(that.esheet.id, "getRules", arrayOf(arrayOf(it)), { it:String ->
                                    console.log("#####GET RULE#####")
                                    console.log(it)
                                    if(that.islive)
                                    that.updateRules(it)
                                })
                            }
                        }
                        is InterestUpdateEvent -> {
                            that.gsheet.call(that.esheet.id, "getRules", arrayOf(arrayOf(ev.entity.id)), {
                                it:String ->
                                console.log("#####GET RULE#####")
                                console.log(it)
                                if(that.islive)
                                that.updateRules(it)
                            })
                        }
                    }
                }
            }
            val f = APP!!.filterParser!!.parse<Json>("CSSStyleRule.properties <- CSSStylesheet.rules <- id = ${that.esheet.id}")!!
            it.filterJson(f)
        }
    }

    fun live() {
        if(islive) return
        val style = document.createElement("style")!!
        console.log(document.getElementsByTagName("head"))
        document.getElementsByTagName("head").forEach {
            it.appendChild(style)
        }
        sheet = document.styleSheets.item(document.styleSheets.length-1) as CSSStyleSheet
        console.log("new sheet")
        console.log(sheet?:"???")

        gsheet.call(esheet.id, "getRules", arrayOf(interest?.order), {
            it:String ->
            console.log("#####GET RULE#####")
            console.log(it)
            updateRules(it)
        })
        islive = true
    }

    fun updateRules(msg:String) {
        val js = JSON.parse<Json>(msg)
        if(js.get("response")!="ok") {
            console.error("error")
            console.error(js)
        }
        val la = js.get("result") as Array<String>
        la.forEach {
            val rid = it
            grule.call(rid, "getCSS", arrayOf(), {
                console.log("$rid: $it")
                val res = JSON.parse<Json>(it)
                updateRule(rid, res.get("result").toString())
            })
        }
    }

    fun updateRule(rid:String, css:String) {
        if(!ruleMap.containsKey(rid)) {
            createRule(rid, css)
        } else {
            val idx = ruleMap[rid]!!
            val rule = grule.get(rid)
            if(rule!=null && rule.state==EntityState.LOADED && !rule.dirty) {
              sheet!!.deleteRule(idx)
              val nidx = sheet!!.insertRule(css, idx)
                console.log("inserted $css was $idx is $nidx")
            }
        }
    }

    fun createRule(rid:String, css:String) {
        val s = sheet
        if(s==null) return
        console.log("CREATE RULE $css")
        console.log(s)
        val idx = s.insertRule(css, s.cssRules.length)
        val sr = s.cssRules.item(idx) as CSSStyleRule
        console.log("created rule")
        console.log(sr)
        ruleMap[rid] = idx
    }
}

val KIStyles : MutableMap<String,StyleManager> = HashMap()

class StyleManagerView(id:String=BaseComponent.id()) : Component<HTMLDivElement>(id) {
    private var sheetsTable : InterestTable? = null
    private var rulesTable : InterestTable? = null
    private var propertiesTable : InterestTable? = null
    private var managedProperties : ManagedProperties? = null

    override fun initialise(n: HTMLDivElement) {
        super<Component>.initialise(n)
        val that = this
        ALL.galaxies["CSSStylesheet"]!!.create("stylemanager") {
            it.buffer(0, 5)
            that.sheetsTable = InterestTable(it)
            val stbl = that.sheetsTable!!
            stbl.colorder.addAll(listOf("name", "rules"))
            stbl.label("name", "Name")
            stbl.label("rules", "Rules")

            stbl.addActions {
                val al = ActionListRenderer(it)
                al.addAction(object : ActionComponent(it) {
                    init {
                        root.textContent = "+"
                        addClass("manage")
                    }
                    override fun invoke(e: Event) {
                        val e = entity
                        if(e==null) return
                        val n = e["name"] as String
                        if(!KIStyles.containsKey(e.id)) {
                            console.log("ADDING SM ${e["name"]}")
                            KIStyles[e.id] = StyleManager(e)
                        }
                    }
                })
                al.addAction(object : ActionComponent(it) {
                    init {
                        addClass("live")
                        root.textContent = "!"
                    }
                    override fun invoke(e: Event) {
                        val e = entity
                        if(e==null) return
                        if(KIStyles.containsKey(e.id)) {
                            KIStyles[e.id]?.live()
                        }
                    }
                })
                al.addAction(object : ActionComponent(it) {
                    init {
                        addClass("properties")
                        root.textContent = "@"
                    }
                    override fun invoke(event: Event) {
                        val e = entity
                        if(e==null) return
                        managedProperties!!.focus(e)
                    }
                })

                al
            }

            stbl.createColumns()
            val f = APP!!.filterParser!!.parse<Json>("id >= 0")!!
            it.filterJson(f)

            that.plus(stbl)
            that.div {
                val sel = input {}
                anchor {
                    textContent = "Add"
                    click {
                        if(that.sheetsTable!!.selected.size()>0) {
                            val eid = that.sheetsTable!!.selected.firstThat { true }!!
                            val tsel = sel.value
                            if(!tsel.trim().isEmpty()) {
                                ALL.galaxies["CSSStylesheet"]!!.call(eid, "addRule", arrayOf(tsel, ""), {})
                            }
                        }
                    }
                }
            }
            ALL.galaxies["CSSProperty"]!!.create("managedproperties") {
                that.managedProperties = ManagedProperties(it)
                that.plus(that.managedProperties!!)
            }
            stbl.onReady {
                initRules()
            }

        }
    }

    fun initRules() {
        val that = this
        ALL.galaxies["CSSStyleRule"]!!.create("stylemanager") {
            it.buffer(0, 5)
            that.rulesTable = InterestTable(it)
            that.rulesTable!!.colorder.addAll(listOf("selector","properties"))
            that.rulesTable!!.createColumns()
            that.rulesTable!!.label("selector", "Selector")
            that.rulesTable!!.label("properties", "Properties")

            val stbl = that.sheetsTable!!
            stbl.onSelection {
                var fs = "id < 0"
                if(it.iterator().hasNext() )
                    fs = it.map { "(id = $it)" }.joinToString(" or ", "CSSStylesheet.rules <- (", ")")

                val f = APP!!.filterParser!!.parse<Json>(fs)!!
                that.rulesTable!!.interest.filterJson(f)
            }
            that.plus(that.rulesTable!!)
            that.div {
                val iname = input {}
                val ivalue = input {}
                anchor {
                    textContent = "Add"
                    click {
                        val eid = that.rulesTable?.selected?.firstThat { true }
                        if (eid!=null) {
                            val n = iname.value
                            val v = ivalue.value
                            if(!n.isEmpty() && !v.isEmpty()) {
                                ALL.galaxies["CSSStyleRule"]!!.call(eid, "addProperty", arrayOf(n,v), {
                                    console.log("created property: ${JSON.stringify(it)}")
                                })
                            }
                        }
                    }
                }
            }
            that.rulesTable!!.onReady {
                ALL.galaxies["CSSProperty"]!!.create("stylemanager") {
                    it.buffer(0, 5)
                    that.propertiesTable = InterestTable(it)
                    that.propertiesTable!!.colorder.addAll(listOf("role", "name","value"))
                    that.propertiesTable!!.createColumns()
                    that.propertiesTable!!.label("role", "Role")
                    that.propertiesTable!!.label("name", "Name")
                    that.propertiesTable!!.label("value", "Value")
                    that.propertiesTable!!.committer = true
                    that.propertiesTable!!.addActions {
                        val al = ActionListRenderer(that.propertiesTable!!.interest)

                        al.addAction(object : ActionComponent(that.propertiesTable!!.interest) {
                            init {addClass("remove"); root.textContent = "-"}

                            override fun invoke(e: Event) {
                                if(entity==null) return
                                val rt = that.rulesTable!!
                                if(rt.selected.size()!=1) return
                                val rule = that.rulesTable?.selected?.firstThat { true }
                                if(rule!=null) {
                                    val r = rt.interest.entity(rule)
                                    rt.interest.galaxy.removeRelation(r, "properties", entity!!.id)
                                }
                            }
                        })

                        al
                    }

                    val stbl = that.rulesTable!!
                    stbl.onSelection {
                        var fs = "id < 0"
                        if(it.iterator().hasNext()) {
                            fs = it.map { "(id = $it)" }.joinToString(" or ", "CSSStyleRule.properties <- (", ")")
                        }
                        console.log(fs)
                        val f = APP!!.filterParser!!.parse<Json>(fs)!!
                        that.propertiesTable!!.interest.filterJson(f)
                    }

                    that.plus(that.propertiesTable!!)
                    ALL.galaxies["CSSProperty"]!!.create("stylemanager") {
                        that+PropertyBag(it, that.rulesTable!!)
                    }
                }
            }
        }
    }
}

class PropertyBag(val interest : Interest, val master:InterestTable, id:String=BaseComponent.id()) : Component<HTMLDivElement>(id) {
    val table : InterestTable = InterestTable(interest)
    val completer : CustomCompleter = CustomCompleter(ALL.galaxies["CSSProperty"]!!, arrayOf("name", "role"), "role");

    init {
        val that = this
        table.colorder.addAll(listOf("role", "name","value"))
        table.createColumns()
        table.label("role", "Role")
        table.label("name", "Name")
        table.label("value", "Value")
        table.committer = true
        table.addActions {
            val al = ActionListRenderer(interest)

            al.addAction(object : ActionComponent(interest) {
                init {
                    addClass("add")
                    root.textContent = "+"
                }

                override fun invoke(e: Event) {
                    val  m = that.master
                    val e = entity
                    val sel = m.selected
                    if(e!=null && sel.size()>0) {
                        sel.forEach {
                            val rule = m.interest.entity(it)
                            m.interest.galaxy.addRelation(rule, "properties", e.id)
                        }
                    }
                }
            })

            al
        }

        this+table;
        this+completer
        completer.on { console.log("!!!complete: " + it?.id); if(it!=null) interest.addEntity(it) }
    }
}

class ManagedProperties(val interest:Interest, id:String=BaseComponent.id()) : Component<HTMLDivElement>(id) {
    val tbl = InterestTable(interest);
    val lbl = Label();

    init {
        this+lbl
        this+tbl
        tbl.colorder.addAll(listOf("name","value"))
        tbl.createColumns()
        tbl.label("name", "Name")
        tbl.label("value", "Value")
    }

    fun focus(sheet:Entity) {
        val f = APP!!.filterParser!!.parse<Json>("CSSStyleRule.properties <- CSSStylesheet.rules <- id = ${sheet.id}")!!
        lbl.textContent = sheet["name"].toString()
        interest.filterJson(f)
    }
}
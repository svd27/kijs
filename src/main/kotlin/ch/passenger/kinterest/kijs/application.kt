package ch.passenger.kinterest.kijs

import ch.passenger.kinterest.kijs.dom.Ajax
import ch.passenger.kinterest.kijs.dom.SocketObservable
import ch.passenger.kinterest.kijs.dom.*
import ch.passenger.kinterest.kijs.model.EntityDescriptor
import ch.passenger.kinterest.kijs.model.Galaxy
import ch.passenger.kinterest.kijs.model.ALL
import ch.passenger.kinterest.kijs.model.PropertyFilter
import ch.passenger.kinterest.kijs.ui.UniverseMenu
import js.dom.html.document
import js.dom.html.window
import ch.passenger.kinterest.kijs.ui.Div
import ch.passenger.kinterest.kijs.model.ServerEvent
import ch.passenger.kinterest.kijs.model.ServerInterestOrderEvent
import ch.passenger.kinterest.kijs.dom.MessageEvent
import ch.passenger.kinterest.kijs.model.ServerInterestEvent
import java.util.HashMap
import rx.js.Disposable
import java.util.HashSet
import ch.passenger.kinterest.kijs.model.JsonEntity
import ch.passenger.kinterest.kijs.model.Entity
import ch.passenger.kinterest.kijs.model.ServerEntityEvent
import ch.passenger.kinterest.kijs.ui.Tabber
import ch.passenger.kinterest.kijs.ui.Tab
import ch.passenger.kinterest.kijs.ui.Span
import ch.passenger.kinterest.kijs.diaries.OverviewPanel
import ch.passenger.kinterest.kijs.style.CSSExporter
import ch.passenger.kinterest.kijs.style.StyleManagerView

/**
 * Created by svd on 07/01/2014.
 */
open class Application(val base: String) : Disposable {
    var appname: String = ""
    var events : SocketObservable? = null
    var entities : SocketObservable? = null
    val APPKEY = "ki-application";
    val disposables : MutableSet<Disposable> = HashSet();
    var filterParser : PegParser? = null

    {

        setKIData(APPKEY, this)
        console.log(document.baseURI)
    }

    fun setKIData(name:String, value:Any) {
        if(document.kidata==null) document.kidata = HashMap()
        document.kidata?.set(name, value)
    }

    fun getKIData(name:String) : Any? = document?.kidata?.get(name)

    var session: Int = -1
    {
        val req = Ajax("http://$base")
        req.asObservabe().subscribe {
            val json = JSON.parse<Json>(it)
            appname = json.get("application").toString()
            session = safeParseInt(json.get("session").toString())!!
            console.log("app ${appname} session: ${session}")
            events = SocketObservable("ws://$base/events")
            entities = SocketObservable("ws://$base/entities");
            events?.subject?.subscribe {
                //console.log(it);
                if (it.`type` == "message") {
                    val ea = JSON.parse<Array<ServerEvent>>(it.data)
                    ea.forEach {
                        handleEvent(it)
                    }
                }
            }
            entities?.subject?.subscribe {
                //console.log(it); console.log(it.data)
                if (it.`type` == "message") {
                    val ja = JSON.parse<Array<JsonEntity>>(it.data)
                    ja.forEach {
                        val galaxy = ALL.galaxies[it.entity]!!

                        val e  : Entity = galaxy.heaven[it.id]?:Entity(galaxy.descriptor, it.id)
                        e.merge(it.values)
                        galaxy.retrieved(listOf(e))
                    }
                }

            }

            val pinger: () -> Unit = { console.log("ping"); events?.send("ping"); entities?.send("ping") }
            val ping = window.setInterval(pinger, 30*1000)
            disposables.add(object : Disposable {
                override fun dispose() {
                    window.clearInterval(ping)
                }
            })
            val sm = json.get("starmap") as Array<Json>
            sm.forEach {
                ALL.galaxies[it.get("entity") as String] = Galaxy(EntityDescriptor(it))
            }
            val getGrammer = Ajax("http://$base/static/filter.grammar")
            getGrammer.asObservabe().subscribe {
                filterParser = PEG.buildParser(it)
                val test = filterParser?.parse<Json>("(state = \"ONLINE\") AND (strength>1)")
                console.log(test?:"BAD")
            }
            getGrammer.start()
            start()
        }
        req.start()
    }


    override fun dispose() {
        disposables.forEach { it.dispose() }
        document.kidata?.remove(APPKEY)
    }

    private val interestevents: Set<String> = setOf("INTEREST", "ORDER", "ADD", "REMOVE")
    private val galaxyevents: Set<String> = setOf("UPDATE", "CREATE", "DELETE")

    fun handleEvent(ev: ServerEvent) {
        if (ev.kind in interestevents) {
            val oe = ev as ServerInterestEvent;
            ALL.galaxies[oe.sourceType]?.consume(oe)
        }
        if(ev.kind in galaxyevents) {
            val ee = ev as ServerEntityEvent
            ALL.galaxies[ev.sourceType]?.onEntity(ee)
        }
    }

    open fun start() {

    }

    open fun close() {
        APP = null
        events?.close()
        entities?.close()
        dispose()
    }
}

class DiariesApp(base:String) : Application(base) {
    {
        APP = this
    }
    override fun start() {
        console.log("START")
        /*
        ALL.galaxies["DiaryOwner"]!!.create("some") {
            console.log(it)
            it.filter(PropertyFilter(it.galaxy.descriptor, "GTE", "id", 0))
        }
        */
        val menu = UniverseMenu(ALL)
        val bl = document.getElementsByTagName("body")
        val root = Div(appname)
        val tabber = Tabber("click")
        val ex = OverviewPanel()

        tabber.addTab(Tab(Span{textContent="Universe"}, menu))
        tabber.addTab(Tab(Span{textContent="Example"}, ex))
        tabber.addTab(Tab(Span{textContent="CSS Sheets"}, StyleManagerView()))
        tabber.addTab(Tab(Span{textContent="CSS"}, CSSExporter()))
        root + tabber
        bl.item(0).appendChild(root.root)
    }
}

public var APP: Application? = null
package ch.passenger.kinterest.kijs

import ch.passenger.kinterest.kijs.diaries.OverviewPanel
import ch.passenger.kinterest.kijs.dom.*
import ch.passenger.kinterest.kijs.model.*
import ch.passenger.kinterest.kijs.style.CSSExporter
import ch.passenger.kinterest.kijs.style.StyleManagerView
import ch.passenger.kinterest.kijs.ui.*
import rx.js.Disposable
import java.util.*
import kotlin.browser.document
import kotlin.browser.window



/**
 * Created by svd on 07/01/2014.
 */
open class Application(val base: String, val ssl:Boolean) : Disposable {
    var appname: String = ""
    var events : SocketObservable? = null
    var entities : SocketObservable? = null
    val APPKEY = "ki-application";
    val disposables : MutableSet<Disposable> = HashSet();
    var filterParser : PegParser? = null
    val HTTP : String
    val WS : String

    init {

        setKIData(APPKEY, this)
        console.log(document.baseURI)
    }

    init {
        APP = this
        HTTP = if(ssl) "https://" else "http://"
        WS = if(ssl) "wss://" else "ws://"
    }

    fun setKIData(name:String, value:Any) {
        if(document.kidata==null) document.kidata = HashMap()
        document.kidata?.set(name, value)
    }

    fun getKIData(name:String) : Any? = document.kidata?.get(name)

    var session: Int = -1
    init {
        val req = Ajax("${APP!!.HTTP}$base")
        req.asObservabe().subscribe {
            val json = JSON.parse<Json>(it)
            appname = json.get("application").toString()
            session = safeParseInt(json.get("session").toString())!!
            console.log("app ${appname} session: ${session}")
            events = SocketObservable("${APP!!.WS}$base/events")
            entities = SocketObservable("${APP!!.WS}$base/entities");
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

                    val ja = JSON.parse<Array<Json>>(it.data).map { object : JsonEntity {
                        override val entity: String = it["entity"].toString()

                        override val id: Long = safeParseInt(it["id"].toString())!!.toLong()

                        override val values: Json = it["values"] as Json

                    } }
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
            val getGrammer = Ajax("${APP!!.HTTP}$base/static/filter.grammar")
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

class DiariesApp(base:String, ssl:Boolean) : Application(base, ssl) {

    override fun start() {
        console.log("START")
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
        bl.item(0)!!.appendChild(root.root)
    }
}

public var APP: Application? = null
package ch.passenger.kinterest.kijs

import js.dom.html.*
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.EventListener
import rx.js.Rx
import ch.passenger.kinterest.kijs.dom.*
import ch.passenger.kinterest.kijs.model.*
import ch.passenger.kinterest.kijs.ui.Div

/**
 * Created by svd on 07/01/2014.
 */


native fun String.replace(suborreg:String, repl:String) : String = js.noImpl

fun main(args: Array<String>) {
    window.onload = {
        console.log("gogo")

        (1..3).forEach {
            val sa = setOf(1, 2, 3, 4)
            val sb = setOf(2, 4)
            val sc = sa-sb
            console.log(sc)
            sc.forEach { console.log(it) }
        }

        var base = document.baseURI
        val ssl = base.startsWith("https")
        base = base.replace("http://", "");
        base = base.replace("https://", "");
        base = base.replace("/static/index.html", "")
        console.log("BASE: $base")
        val app = DiariesApp(base, ssl)
    }

}

/*
fun test() {
    Rx.Observable.fromArray(Array(5) { it }).subscribe { console.log(it * 2) }


    val req = Ajax("${APP!!.HTTP}localhost/diaries")
    req.asObservabe().subscribe {
        val body = document.getElementsByTagName("body").item(0)
        if (it is String) {
            val json = JSON.parse<Json>(it)
            console.log(json)
            val sm = json.get("starmap") as Array<Json>
            sm.forEach {
                ALL.galaxies[it.get("entity") as String] = Galaxy(EntityDescriptor(it))
            }
            val div = Div("content")
            ALL.galaxies.values().forEach {
                div.div {
                    span { +it.descriptor.entity }
                    dl {
                        val that = this
                        it.descriptor.properties.values().forEach {
                            that.dt { +it.property }
                            that.dd {
                                dl {
                                    dt { +"Type" }
                                    dd { +it.datatype }
                                    dt { +"Relation" }
                                    dd { +"${it.relation}" }
                                }
                            }
                        }
                    }
                }

            }
            body.appendChild(div.root)
        }
    }
    req.start()
}
*/
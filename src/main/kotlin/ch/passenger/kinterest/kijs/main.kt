package ch.passenger.kinterest.kijs

import kotlin.browser.document
import kotlin.browser.window

/**
 * Created by svd on 07/01/2014.
 */


//@native fun String.replace(suborreg:String, repl:String) : String = noImpl

fun main(args: Array<String>) {
    window.onload = {
        console.log("gogo")

        var base = document.baseURI!!
        val ssl = base.startsWith("https")
        base = base.replace("http://", "");
        base = base.replace("https://", "");
        base = base.replace("/static/index.html", "")
        console.log("BASE: $base")
        DiariesApp(base, ssl)
    }

}

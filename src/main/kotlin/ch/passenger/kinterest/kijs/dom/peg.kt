package ch.passenger.kinterest.kijs.dom

/**
 * Created by svd on 11/01/2014.
 */
native trait PegParser {
    fun parse(s:String) : Any?
}

native trait Pegger {
    fun buildParser(g:String) : PegParser
}

native val PEG : Pegger = js.noImpl
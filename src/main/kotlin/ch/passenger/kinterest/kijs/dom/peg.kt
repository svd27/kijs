package ch.passenger.kinterest.kijs.dom

/**
 * Created by svd on 11/01/2014.
 */
@native interface PegParser {
    fun <T> parse(s:String) : T?
}

@native interface Pegger {
    fun buildParser(g:String) : PegParser
}

@native val PEG : Pegger = noImpl
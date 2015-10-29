package ch.passenger.kinterest.kijs.dom

import rx.js.Observable
import rx.js.Subject
import rx.js.RxFactory
import rx.js.Rx
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.EventListener
import ch.passenger.kinterest.kijs.dom.*
import rx.js.AsyncSubject

/**
 * Created by svd on 07/01/2014.
 */

@native public class XMLHttpRequest() {
    public fun open(method : String, url : String, async : Boolean = true, user : String? = null, password : String? = null  ) : Unit = noImpl

    public @native var onreadystatechange : () -> Unit = noImpl
    public @native var responseText : String = noImpl
    public @native var statusText : String = noImpl
    public @native var readyState : Short = noImpl
    public @native fun send() : Unit = noImpl
    public @native fun send(msg : String) : Unit = noImpl
    public @native fun setRequestHeader(name : String, value : String) :Unit = noImpl
}

class Ajax(val url:String, val method:String="GET") {
    val req = XMLHttpRequest()

    fun asObservabe() : Observable<String> {
        val s : Subject<String> = Subject()
        //console.log(s)
        val target = req as EventSource
        target.addEventListener("load") {
            console.log("RESP: ${req.responseText}")
            s.onNext(req.responseText)
        };

        target.addEventListener("abort") {
            s.onError(Exception(req.statusText))
        };

        target.addEventListener("error") {
            s.onError(Exception(req.statusText))
        };
        //start()
        return s
    }



    fun open() {if(method=="GET") req.open(method, url+"?cache="+Date().getTime()) else req.open(method, url)}
    fun start() {
        open()
        req.send()
    }

    fun start(msg:String) {
        open()
        req.send(msg)
    }
}

@native class ProgressEvent : Event("progress") {
    @native val lengthComputable : Boolean = noImpl
    @native val loaded : Long = noImpl
    @native val total : Long = noImpl
}

public @native class WebSocket(val url : String) {
    val readyState : Int = noImpl
    var onclose : (e : MessageEvent) -> Unit = noImpl
    var onerror : (e : MessageEvent) -> Unit = noImpl
    var onmessage : (e : MessageEvent) -> Unit = noImpl
    var onopen : (e : MessageEvent) -> Unit = noImpl

    fun close(code : Long?, reason : String?) : Unit = noImpl
    fun send(msg : String) : Unit = noImpl
}

public class SocketObservable(val url:String, val subject : Subject<MessageEvent> = Subject())  {
    val ws : WebSocket = WebSocket(url);
    init {
        console.log("init socket $url")
        console.log(ws)
      ws.onclose = { subject.onNext(it); subject.onCompleted()}
      ws.onerror = {subject.onNext(it); subject.onError(Exception())}
      ws.onopen = {subject.onNext(it)}
      ws.onmessage = {subject.onNext(it);}
    }


    fun close() {ws.close(0, "")}
    fun send(msg:String) = ws.send(msg)
}
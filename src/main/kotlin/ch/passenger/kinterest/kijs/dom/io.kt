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

native public public class XMLHttpRequest() {
    public fun open(method : String, url : String, async : Boolean = true, user : String? = null, password : String? = null  ) : Unit = js.noImpl

    public native var onreadystatechange : () -> Unit = js.noImpl
    public native var responseText : String = js.noImpl
    public native var statusText : String = js.noImpl
    public native var readyState : Short = js.noImpl
    public native fun send() : Unit = js.noImpl
    public native fun send(msg : String) : Unit = js.noImpl
    public native fun setRequestHeader(name : String, value : String) :Unit = js.noImpl
}

class Ajax(val url:String, val method:String="GET") {
    val req = XMLHttpRequest()

    fun asObservabe() : Observable<String> {
        val s : Subject<String> = Subject()
        console.log(s)
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

native trait ProgressEvent : Event {
    native val lengthComputable : Boolean
    native val loaded : Long
    native val total : Long
}

public native class WebSocket(val url : String) {
    val readyState : Int = js.noImpl
    var onclose : (e : MessageEvent) -> Unit = js.noImpl
    var onerror : (e : MessageEvent) -> Unit = js.noImpl
    var onmessage : (e : MessageEvent) -> Unit = js.noImpl
    var onopen : (e : MessageEvent) -> Unit = js.noImpl

    fun close(code : Long?, reason : String?) = js.noImpl
    fun send(msg : String) = js.noImpl
}

public class SocketObservable(val url:String, val subject : Subject<MessageEvent> = Subject())  {
    var ws : WebSocket = WebSocket(url);
    {
        console.log("init socket $url")
        console.log(ws!!)
      ws.onclose = { subject.onNext(it); subject.onCompleted()}
      ws.onerror = {subject.onNext(it); subject.onError(Exception())}
      ws.onopen = {subject.onNext(it)}
      ws.onmessage = {subject.onNext(it);}
    }


    fun close() {ws?.close(0, "")}
    fun send(msg:String) = ws?.send(msg)
}
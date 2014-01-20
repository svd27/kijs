package rx.js

import org.w3c.dom.events.Event
import ch.passenger.kinterest.kijs.dom.EventSource

/**
 * Created by svd on 07/01/2014.
 */


native val Rx : RxFactory = js.noImpl

native class RxFactory {
    public native val Observable : ObservableFactory = js.noImpl
    public native val Observer : ObserverFactory = js.noImpl
    public native fun<T> Subject() : Subject<T> = js.noImpl
    //public native fun<T> AsyncSubject() : Subject<T> = js.noImpl
}

native class ObserverFactory {
    fun<T> create(n:(T)->Unit, err:(Exception)->Unit, c:()->Unit) : Observer<T> = js.noImpl
}


public open native("Rx.Subject") class Subject<T>() : Observer<T>, Observable<T> {
    override fun subscribe(obs: Observer<T>): Disposable = js.noImpl
    override fun subscribe(cb: (T) -> Unit): Disposable = js.noImpl
    fun onNext(t: T) : Unit = js.noImpl
    fun onCompleted() : Unit = js.noImpl
    fun onError(error: Exception) : Unit  = js.noImpl
    override fun<U> select(cb:(T)->U) : Observable<U> = js.noImpl
    override fun where(cb:(T)->Boolean) : Observable<T> = js.noImpl
    override fun throttle(ms:Long) : Observable<T> = js.noImpl
    override fun distinctUntilChanged() : Observable<T> = js.noImpl
    override fun bufferWithTimeOrCount(timeSpan: Long, count: Int): Observable<Array<T>> = js.noImpl
}

public open native("Rx.AsyncSubject") class AsyncSubject<T>() : Subject<T>()

native class ObservableFactory {
    native public fun<T> fromArray(array:Array<T>) : Observable<T> = js.noImpl
    native public fun<T:Event> fromEvent(source:EventSource,event:String) : Observable<T> = js.noImpl
    public native fun<T> merge(vararg obs:Observable<T>) : Observable<T> = js.noImpl
}

trait Disposable {
    fun dispose()
}

native trait Observable<T> {
    fun subscribe(obs:Observer<T>) : Disposable
    fun subscribe(cb:(T)->Unit) : Disposable
    fun<U> select(cb:(T)->U) : Observable<U>
    fun where(cb:(T)->Boolean) : Observable<T>
    fun throttle(ms:Long) : Observable<T>
    fun distinctUntilChanged() : Observable<T>
    fun bufferWithTimeOrCount(timeSpan:Long, count:Int) : Observable<Array<T>>
    fun subscribe(cb:(T)->Unit, err:(Exception)->Unit, complete:()->Unit={}) : Disposable {
        return subscribe(Rx.Observer.create(cb, err, complete))
    }
}

native trait Observer<T> {
}


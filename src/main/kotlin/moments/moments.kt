package moments

/**
 * Created by svd on 07/01/2014.
 */


public native trait Moment {
    fun format(f:String):String
}

public native fun moment() : Moment = js.noImpl
public native fun moment(value:String, format:String) : Moment = js.noImpl
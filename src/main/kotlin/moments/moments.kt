package moments

/**
 * Created by svd on 07/01/2014.
 */


public @native interface Moment {
    fun format(f:String):String = noImpl
}

public @native fun moment() : Moment = noImpl
public @native fun moment(value:String, format:String) : Moment = noImpl
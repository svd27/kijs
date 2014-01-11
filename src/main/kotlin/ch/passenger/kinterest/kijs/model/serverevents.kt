package ch.passenger.kinterest.kijs.model

/**
 * Created by svd on 08/01/2014.
 */
public trait ServerEvent {
    val kind : String
}

public trait ServerEntityEvent : ServerEvent {
    val sourceType : String
}

public trait ServerInterestEvent : ServerEntityEvent {
    val interest : Int
}

public trait ServerInterestOrderEvent : ServerInterestEvent {
    val order : Array<Long>
}

public trait ServerInterestConfigEvent : ServerInterestEvent {
    val estimatedsize : Int
    val offset : Int
    val limit : Int
    val orderBy : Array<Json>
}

public trait ServerEntityUpdateEvent : ServerEntityEvent {
    val property : String
    val id:Long
    val value:Any?
    val old:Any?
}

public trait JsonEntity {
    val entity : String
    val id : Long
    val values : Json
}
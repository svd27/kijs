package ch.passenger.kinterest.kijs.model

/**
 * Created by svd on 08/01/2014.
 */
public interface ServerEvent {
    val kind : String
}

public interface ServerEntityEvent : ServerEvent {
    val sourceType : String
}

public interface ServerInterestEvent : ServerEntityEvent {
    val interest : Int
}

public interface ServerInterestOrderEvent : ServerInterestEvent {
    val order : Array<Long>
}

public interface ServerInterestAddEvent : ServerInterestEvent {
    val id : Long
}

public interface ServerInterestRemoveEvent : ServerInterestEvent {
    val id : Long
}

public interface ServerInterestConfigEvent : ServerInterestEvent {
    val estimatedsize : Int
    val currentsize : Int
    val offset : Int
    val limit : Int
    val orderBy : Array<Json>
}

public interface ServerEntityUpdateEvent : ServerEntityEvent {
    val property : String
    val id:Long
    val value:Any?
    val old:Any?
}

public interface JsonEntity {
    val entity : String
    val id : Long
    val values : Json
}
package ch.passenger.kinterest.kijs.model

import java.util.HashMap
import ch.passenger.kinterest.kijs.mapOf
import ch.passenger.kinterest.kijs.map
import java.util.Collections.emptyList
import ch.passenger.kinterest.kijs.Tuple2
import ch.passenger.kinterest.kijs.APP
import ch.passenger.kinterest.kijs.forEach
import ch.passenger.kinterest.kijs.reduce
import ch.passenger.kinterest.kijs.any
import java.util.ArrayList
import ch.passenger.kinterest.kijs.dom.Ajax
import ch.passenger.kinterest.kijs.setOf
import ch.passenger.kinterest.kijs.dom.console
import rx.js.Subject
import rx.js.Observable
import ch.passenger.kinterest.kijs.filter
import ch.passenger.kinterest.kijs.listOf
import rx.js.Disposable
import moments.Moment
import ch.passenger.kinterest.kijs.all

/**
 * Created by svd on 07/01/2014.
 */

public class EntityDescriptor(json:Json) {
    val entity : String = json.get("entity") as String
    val properties : Map<String, PropertyDescriptor> = mapOf((json.get("properties") as Array<Json>).map { Tuple2(it.get("property") as String, PropertyDescriptor(it)) })
}

public class PropertyDescriptor(json:Json) {
    val property : String = json.get("property") as String
    val enum : Boolean = json.get("enum") as Boolean
    val enumvalues : Array<String> = if(!enum) Array<String>(0) {""} else json.get("enumvalues") as Array<String>
    val unique : Boolean = json.get("unique") as Boolean
    val readonly : Boolean = json.get("readonly") as Boolean
    val relation : Boolean = json.get("relation") as Boolean
    val nullable : Boolean = json.get("nullable") as Boolean
    val label : Boolean = json.get("label") as Boolean
    val datatype : String = if(relation) "" else json.get("type") as String
    val entity : String = if(relation) json.get("entity") as String else ""
    val scalars : Set<String> = setOf("long", "int",  "short", "byte", "char")
    val floats : Set<String> = setOf("double", "float")
    val dates : Set<String> = setOf("java.util.Date", "org.joda.DateTime", "org.joda.time.LocalDate")
    val dateformat : String = "YYYYMMDDHHmmssSSS"

    val scalar : Boolean get() = scalars.contains(datatype)
    val floaty : Boolean get() = floats.contains(datatype)
    val daty : Boolean get() = dates.contains(datatype)

    fun cast(v:Any?) : Any? {
        if(!nullable && v == null) throw Exception("$property is not nullabel")
        if(v==null) return null
        if(relation) {
            return toInt(v)
        }

        if(scalars.contains(datatype)) return toInt(v)
        if(floats.contains(datatype)) return  toFloat(v)
        if(dates.contains(datatype)) {
            if(v is String) return moments.moment(v, dateformat)
            if(v is Number) return moments.moment(v.toString(), dateformat)
        }

        return v
    }

    fun serialise(v:Any?) : Any? {
        if(v==null) return v
        if(dates.contains(datatype)) {
            val m = v as Moment
            return v.format(dateformat)
        }
        return v
    }

    fun toInt(v:Any) : Int {
        return safeParseInt(v.toString())!!
    }

    fun toFloat(v:Any) : Double {
        return safeParseDouble(v.toString())!!
    }
}



public open class Entity(public val descriptor : EntityDescriptor, public val id : Long ) {
    protected val values : MutableMap<String,Any?> = HashMap()
    protected val written : MutableMap<String,Any?> = HashMap()
    protected val wroteOn : MutableMap<String,Any?> = HashMap()
    public var updateHook : (Entity)->Unit = {}

    public fun merge(json:Json) {
        descriptor.properties.values().forEach {
            val v = json.get(it.property)
            values[it.property] = it.cast(v)
        }
    }

    public fun update(ue:ServerEntityUpdateEvent) {
        values[ue.property] = descriptor.properties[ue.property]!!.cast(ue.value)
        updateHook(this)
    }

    public fun get(p:String) : Any? {
        if(written.containsKey(p)) return written[p]
        return values[p]
    }

    public fun set(p:String, v:Any?) {
        wroteOn[p] = values[p]
        written[p] = v
        ALL.galaxies[descriptor.entity]?.updated(this, p, wroteOn[p])
        updateHook(this)
    }

    public fun realValue(p:String) : Any? {
        return values[p]
    }

    public fun conflicted(p:String) : Boolean {
        if(written.containsKey(p)) {
            return wroteOn[p] != values[p]
        }
        return false
    }
    public val hasConflict : Boolean get() = descriptor.properties.keySet().any { conflicted(it) }
    public fun revert() {wroteOn.clear(); written.clear(); descriptor.properties.values().forEach {
        ALL.galaxies[descriptor.entity]?.updated(this, it.property, values[it.property])
        updateHook(this)
    }}
    public fun isDirty(p:String) : Boolean {
        return written.containsKey(p)
    }
    open val dirty : Boolean get() = descriptor.properties.keySet().any { isDirty(it) }

    fun collect() : Json {
        val jv = JSON.parse<Json>("{}")
        written.keySet().forEach {
            jv.set(it, descriptor.properties[it]?.serialise(written[it]))
        }
        val je = JSON.parse<Json>("{}")
        if(id>=0) je.set("id", id)
        je.set("entity", descriptor.entity)
        je.set("values", jv)
        return je
    }

    fun save() {
        ALL.galaxies[descriptor.entity]!!.save(this)
    }

    public fun equals(o:Any?) : Boolean {
        if(o is Entity) return o.id == id
        return false
    }

    public fun hashCode() : Int = id.hashCode()
}

public open class EntityTemplate(descriptor:EntityDescriptor) : Entity(descriptor, -1) {
    override val dirty: Boolean get() {
        return descriptor.properties.values().all {
            (!it.readonly && it.nullable)
            || ((it.readonly || !it.nullable) &&  written.containsKey(it.property))

        }
    }
}



public val ALL : Universe = Universe()

public class Universe() {
    val galaxies : MutableMap<String,Galaxy> = HashMap()
    fun event(msg:Json) {}
    fun entity(msg:Json) {}
}

public class Galaxy(public val descriptor : EntityDescriptor) {
    val heaven : MutableMap<Long,Entity> = HashMap()
    val interests : MutableMap<Int,Interest> = HashMap()
    val NOTLOADED : Entity = Entity(descriptor, -1)

    fun retrieve(ids:Iterable<Long>) {
        val req = Ajax("http://${APP!!.base}/${descriptor.entity}/retrieve", "POST")

        req.start(JSON.stringify(ids))
    }

    fun retrieved(entities:Iterable<Entity>) {
        entities.forEach { heaven[it.id] = it }
        interests.values().forEach { it.retrieved(entities) }
    }

    fun create(name:String, cb:(Interest)->Unit) {
        val req = Ajax("http://${APP!!.base}/${descriptor.entity}/create/$name", "GET")
        req.asObservabe().subscribe {
            console.log(it);
            val json = JSON.parse<Json>(it)
            val id = safeParseInt(json.get("interest").toString())!!;
            interests[id] = Interest(id, name, this); cb(interests[id]!!)
        }
        req.start()
    }

    fun save(e:Entity) {
        if(e.id<0) {
            createEntity(e)
            e.revert()
            return
        }
        val req = Ajax("http://${APP!!.base}/${descriptor.entity}/save", "POST")

        val json = e.collect()
        e.revert()
        req.start(JSON.stringify(json))
    }

    fun createEntity(e:Entity) {
        val req = Ajax("http://${APP!!.base}/${descriptor.entity}/createEntity", "POST")

        val json = e.collect()
        e.revert()
        req.start(JSON.stringify(json))
    }

    fun get(id:Long) : Entity = if(id in heaven.keySet()) heaven[id]!! else NOTLOADED
    fun contains(id:Long) = id in heaven.keySet()

    fun consume(ev:ServerInterestEvent) {
        interests[ev.interest]?.consume(ev)
    }

    fun onEntity(e:ServerEntityEvent) {
        if(e.kind=="UPDATE") {
            val ue = e as ServerEntityUpdateEvent
            val e = heaven[ue.id]
            if(e is Entity) {
                e.update(ue)
                interests.values().forEach {
                    it.updated(e, ue.property, ue.old)
                }
            }
        }
    }

    fun updated(entity:Entity, property:String, old:Any?) {
        interests.values().forEach {
            it.updated(entity, property, old)
        }
    }

    val relations : MutableMap<String,Interest> = HashMap()

    public fun goodRelations(entity:String, rid:Long) {
        val that = this
        if(!relations.containsKey(entity)) {
            ALL.galaxies[entity]!!.create("${descriptor.entity}-relations-${entity}") {
                relations[entity] = it
                it.eager = true
                keepRelations(it)
                it+rid
            }
        }
    }

    private fun keepRelations(i:Interest, pd:PropertyDescriptor) {
        i.on {
            when(it) {
              is InterestLoadEvent -> {
                  val le = it as InterestLoadEvent
                  heaven.values().forEach {
                      val entity = it
                      it.descriptor.properties.values().filter {
                          it.relation && it.entity==i.galaxy.descriptor.entity
                      }.forEach {
                          val old = entity[it.property]
                          entity[it.property] = le.entity.id
                          updated(entity, it.property, old)
                      }
                  }
              }
            }
        }
    }
}



public open class GalaxyEvent(val galaxy:Galaxy)
public class GalaxyLoadEvent(galaxy:Galaxy, val entities:Iterable<Entity>) : GalaxyEvent(galaxy)

public open class InterestEvent(val interest:Interest)
public class InterestConfigEvent(interest:Interest) : InterestEvent(interest)
public open class InterestLoadEvent(interest:Interest, val entity:Entity, val idx:Int) : InterestEvent(interest)
public open class InterestRemoveEvent(interest:Interest, val ids:Array<Long>) : InterestEvent(interest)
public open class InterestOrderEvent(interest:Interest, val order:Array<Long>) : InterestEvent(interest)
public open class InterestUpdateEvent(interest:Interest, val entity:Entity, val idx:Int, val property:String, val old:Any?) : InterestEvent(interest)

public enum class SortDirection {ASC DESC}
public class SortKey(val property:String, var direction:SortDirection) {fun toggle() {if(direction==SortDirection.ASC) direction=SortDirection.DESC else direction=SortDirection.ASC} }

public class Interest(val id:Int, val name:String, val galaxy:Galaxy, private val subject:Subject<InterestEvent> = Subject()) {
    var eager : Boolean = false
    val order : MutableList<Long> = ArrayList()
    var offset : Int = 0
    var limit : Int = 0
    var size : Int = 0
    val orderBy : MutableList<SortKey> = ArrayList()

    fun sort(keys:Array<SortKey>) {
        val req = Ajax("http://${APP!!.base}/${galaxy.descriptor.entity}/$id/orderBy", "POST")
        val js = Array<Json>(keys.size) {
            val k = keys[it]
            val j = JSON.parse<Json>("{}")
            j.set("property", k.property)
            j.set("direction", k.direction.name())
            j
        }

        req.start(JSON.stringify(js))
    }

    fun cfg(cfg:ServerInterestConfigEvent) {
       size = cfg.estimatedsize
       offset = cfg.offset
       limit  = cfg.limit
       val oa = cfg.orderBy
        orderBy.clear()
        oa.forEach {
            console.debug(it)
            orderBy.add(SortKey(it.get("property")!!.toString(), SortDirection.valueOf(it.get("direction")!!.toString())))
        }
        subject.onNext(InterestConfigEvent(this))
    }

    fun filter(f:Filter) {
        filterJson(f.json)
    }

    fun filterJson(f:Json) {
        val req = Ajax("http://${APP!!.base}/${galaxy.descriptor.entity}/filter/$id", "POST")
        req.start(JSON.stringify(f))
    }

    fun buffer(offset:Int, limit:Int) {
        val req = Ajax("http://${APP!!.base}/${galaxy.descriptor.entity}/$id/offset/$offset/limit/$limit", "GET")
        req.start()
    }

    fun addEntity(e:Entity) {
        plus(e.id)
    }

    fun plus(aid:Long) {
        val req = Ajax("http://${APP!!.base}/${galaxy.descriptor.entity}/$id/add/${aid}", "GET")
        req.start()
    }

    fun remove(e:Entity) {
        val req = Ajax("http://${APP!!.base}/${galaxy.descriptor.entity}/$id/remove/${e.id}", "GET")
        req.start()
    }

    fun clear() {
        val req = Ajax("http://${APP!!.base}/${galaxy.descriptor.entity}/$id/clear", "GET")
        req.start()
    }

    fun get(idx:Int) : Entity? {
        val eid = order[idx]
        if(eid !in galaxy && !eager) galaxy.retrieve(listOf(eid))
        return galaxy[eid]
    }

    fun entity(eid:Long) = galaxy[eid]

    fun onOrder(nwo:Array<Long>) {
        val ol = Array<Long>(order.size()) {order[it]}

        order.clear()
        subject.onNext(InterestRemoveEvent(this, ol));
        for(it in nwo) order.add(it)
        if(eager) galaxy.retrieve(order)
        subject.onNext(InterestOrderEvent(this, order.copyToArray()))
    }

    fun retrieved(ids:Iterable<Entity>) {
        ids.filter { it.id in order }.map { Tuple2(order.indexOf(it.id), it) }.forEach { subject.onNext(InterestLoadEvent(this, it.second, it.first)) }
    }

    fun consume(ev:ServerInterestEvent) {
        when (ev.kind) {
            "ORDER" -> onOrder((ev as ServerInterestOrderEvent).order)
            "INTEREST" -> cfg((ev as ServerInterestConfigEvent))
        }
    }

    fun on(cb:(InterestEvent)->Unit): Disposable {
        return subject.subscribe { cb(it) }
    }

    fun updated(entity:Entity, property:String, old:Any?) {
        if(order.contains(entity.id)) {
            subject.onNext(InterestUpdateEvent(this, entity, order.indexOf(entity.id), property, old))
        }
    }

    fun first() {
        buffer(0, limit)
    }

    fun previous() {
        if(offset>0) {
            buffer(Math.max(offset-limit, 0), limit)
        }
    }

    fun next() {
        if(offset+limit<size)
        buffer(offset+limit, limit)
    }
}

abstract class Filter() {
    val json : Json get() = serialise()

    abstract fun serialise() : Json
}

abstract class EntityFilter(val descriptor:EntityDescriptor) : Filter() {

    open override fun serialise(): Json {
        val json = JSON.parse<Json>("{}")
        json.set("entity", descriptor.entity)

        return json
    }
}

class PropertyFilter(descriptor:EntityDescriptor, val op:String, val property:String, val value:Any?) : EntityFilter(descriptor) {
    override fun serialise(): Json {
        val json = super<EntityFilter>.serialise()
        json.set("relation", op)
        json.set("property", property)
        if(property!="id" ) json.set("value", descriptor.properties[property]!!.cast(value)) else json.set("value", value)
        if(op=="LIKE" || op=="NOTLIKE") json.set("value", "$value.*")
        return json
    }
}

class RelationFilter(descriptor:EntityDescriptor, val op:String, val operands:Array<out EntityFilter>) : EntityFilter(descriptor) {

    override fun serialise(): Json {
        val json = super<EntityFilter>.serialise()
        json.set("relation", op)
        json.set("operands", Array<Json>(operands.size) {operands[it].serialise()})
        return json
    }
}
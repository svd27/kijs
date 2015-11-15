package ch.passenger.kinterest.kijs.model

import ch.passenger.kinterest.kijs.*
import ch.passenger.kinterest.kijs.dom.Ajax
import moments.Moment
import rx.js.Disposable
import rx.js.Rx
import rx.js.Subject
import java.util.*
import kotlin.js.Json

/**
 * Created by svd on 07/01/2014.
 */

public class EntityDescriptor(json:Json) {
    val entity : String = json.get("entity") as String
    val properties : Map<String, PropertyDescriptor> = mapOf((json.get("properties") as Array<Json>).map { Pair(it.get("property") as String, PropertyDescriptor(it)) })
}

public class PropertyDescriptor(json:Json) {
    val property : String = json.get("property") as String
    val enum : Boolean = json.get("enum") as Boolean
    val enumvalues : Array<String> = if(!enum) Array<String>(0) {""} else json.get("enumvalues") as Array<String>
    val unique : Boolean = json.get("unique") as Boolean
    val readonly : Boolean = json.get("readonly") as Boolean
    val relation : Boolean = json.get("relation") as Boolean
    val oneToMany : Boolean = json.get("oneToMany") as Boolean
    val nullable : Boolean = json.get("nullable") as Boolean
    val label : Boolean = json.get("label") as Boolean
    val datatype : String = if(relation || oneToMany) "" else json.get("type") as String
    val entity : String = if(relation || oneToMany) json.get("entity") as String else ""
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

        if(relation || oneToMany) {
            return toInt(v)
        }

        if(scalars.contains(datatype)) return toInt(v)
        if(floats.contains(datatype)) return  toFloat(v)
        if(dates.contains(datatype)) {
            return moments.moment(v.toString(), dateformat)
        }

        return v
    }

    fun serialise(v:Any?) : Any? {
        if(v==null) return v
        if(dates.contains(datatype)) {
            val m = v as Moment
            return m.format(dateformat)
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

public enum class EntityState {CREATED, LOADED, DELETED, REMOVED}

public open class Entity(public val descriptor : EntityDescriptor, public val id : String ) {
    protected val values : MutableMap<String,Any?> = HashMap()
    protected val written : MutableMap<String,Any?> = HashMap()
    protected val wroteOn : MutableMap<String,Any?> = HashMap()
    public var updateHook : (Entity)->Unit = {}
    private var _state : EntityState = EntityState.CREATED
    public val state : EntityState get() = _state

    public fun merge(json:Json) {
        descriptor.properties.values.forEach {
            val v = json.get(it.property)
            val cast = it.cast(v)
            values[it.property] = cast
            if(it.relation && cast != null) {
                ALL.galaxies[descriptor.entity]!!.goodRelations(it.entity, cast.toString())
            }
        }
        _state = EntityState.LOADED
    }

    public fun update(ue:ServerEntityUpdateEvent) {
        values[ue.property] = descriptor.properties[ue.property]!!.cast(ue.value)
        updateHook(this)
    }

    public fun mergeProperty(p:String, v:Any?) {
        values[p] = v
        updateHook(this)
    }

    public operator fun get(p:String) : Any? {
        if(written.containsKey(p)) return written[p]
        return values[p]
    }

    public operator fun set(p:String, v:Any?) {
        wroteOn[p] = values[p]
        written[p] = v
        val pd = descriptor.properties[p]!!
        if(pd.relation && v != null) {
            ALL.galaxies[descriptor.entity]!!.goodRelations(pd.entity, v.toString())
        }
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
    open val dirty : Boolean get() = descriptor.properties.keys.any { isDirty(it) }

    fun collect() : Json {
        val jv = JSON.parse<Json>("{}")
        written.keySet().forEach {
            jv.set(it, descriptor.properties[it]?.serialise(written[it]))
        }
        val je = JSON.parse<Json>("{}")
        if(!id.isEmpty()) je.set("id", id)
        je.set("entity", descriptor.entity)
        je.set("values", jv)
        console.log(je)
        return je
    }

    fun save() {
        ALL.galaxies[descriptor.entity]!!.save(this)
    }

    fun removed() {
        _state = EntityState.REMOVED
    }

    fun deleted() {
        _state = EntityState.DELETED
    }

    fun related(p:String) {
        val pd = descriptor.properties[p]
        if(!(pd?.oneToMany?:false)) throw IllegalArgumentException()
        val c = values[p] as Int?
        values[p] = c?:0+1
        ALL.galaxies[descriptor.entity]?.updated(this, p, values[p])
        updateHook(this)
    }

    fun unrelated(p:String) {
        val pd = descriptor.properties[p]
        if(!(pd?.oneToMany?:false)) throw IllegalArgumentException()
        val c = values[p] as Int?
        values[p] = c?:0-1
        ALL.galaxies[descriptor.entity]?.updated(this, p, values[p])
        updateHook(this)
    }

    fun hasProperty(p:String) : Boolean = descriptor.properties.containsKey(p)

    public override fun equals(o:Any?) : Boolean {
        if(o is Entity) return o.id == id
        return false
    }

    public override fun hashCode() : Int = id.hashCode()
}

public open class EntityTemplate(descriptor:EntityDescriptor) : Entity(descriptor, "") {
    override val dirty: Boolean get() {
        return descriptor.properties.values().all {
            (!it.readonly && it.nullable) || (it.oneToMany)
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
    val heaven : MutableMap<String,Entity> = HashMap()
    val interests : MutableMap<Int,Interest> = HashMap()
    val NOTLOADED : Entity = Entity(descriptor, "")
    val retrieving : MutableList<String> = ArrayList()
    val subRetriever : Subject<String> = Subject();

    init {
        subRetriever.bufferWithTimeOrCount(400, 100).subscribe { dretrieve(it) }
    }

    fun retrieve(ids:Iterable<String>) = ids.filter { it !in heaven || heaven[it]?.state!=EntityState.LOADED }.forEach { subRetriever.onNext(it) }

    fun dretrieve(ids:Array<String>) {
        val list = ArrayList<String>()
        for(id in ids) {
            if(id !in retrieving) {
                retrieving.add(id)
                if(id !in list) list.add(id)
            }
        }
        //ids.filter { !retrieving.contains(it) }.forEach { retrieving.add(it); if(!list.contains(it)) list.add(it) }

        if(list.size>0) {
        val req = Ajax("${APP!!.HTTP}${APP!!.base}/${descriptor.entity}/retrieve", "POST")

        req.start(JSON.stringify(list))
        }
    }

    fun retrieved(entities:Iterable<Entity>) {
        entities.forEach { heaven[it.id] = it; retrieving.remove(it) }
        interests.values().forEach { it.retrieved(entities) }
    }

    fun create(name:String, cb:(Interest)->Unit) {
        val req = Ajax("${APP!!.HTTP}${APP!!.base}/${descriptor.entity}/create/$name", "GET")
        req.asObservabe().subscribe {
            console.log(it);
            val json = JSON.parse<Json>(it)
            val id = safeParseInt(json.get("interest").toString())!!;
            interests[id] = Interest(id, name, this); cb(interests[id]!!)
        }
        req.start()
    }

    fun save(e:Entity) {
        if(e.id.isEmpty()) {
            createEntity(e)
            e.revert()
            return
        }
        val req = Ajax("${APP!!.HTTP}${APP!!.base}/${descriptor.entity}/save", "POST")

        val json = e.collect()
        e.revert()
        req.start(JSON.stringify(json))
    }

    fun createEntity(e: Entity, cb: (String?) -> Unit = { }) {
        val req = Ajax("${APP!!.HTTP}${APP!!.base}/${descriptor.entity}/createEntity", "POST")
        req.asObservabe().subscribe({
            val js = JSON.parse<Json>(it)
            val ok = js.get("response")
            if ("ok" == ok) {
                cb(js.get("id") as String?)
            } else cb(null)
        },
                {
                    cb(null)
                })
        val json = e.collect()
        e.revert()
        req.start(JSON.stringify(json))
    }

    fun call(target:String, action:String, args:Array<Any?>, cb:(String)->Unit, err:(Exception)->Unit={console.error(it)})  {
        console.log("CALL: $target.$action")
        val pars = JSON.stringify(args)
        val req = Ajax("${APP!!.HTTP}${APP!!.base}/${descriptor.entity}/entity/${target}/action/$action", "POST")
        req.asObservabe().subscribe(Rx.Observer.create(cb, err, {}))
        req.start(pars)
    }

    fun get(id:String) : Entity = if(id in heaven) heaven[id]!! else {subRetriever.onNext(id); NOTLOADED; }
    fun contains(id:String) = id in heaven

    fun consume(ev:ServerInterestEvent) {
        if(ev is ServerInterestOrderEvent) {
            ev.order.filter { it !in heaven }.forEach { heaven[it] = Entity(descriptor, it) }
        }
        interests[ev.interest]?.consume(ev)
    }

    fun onEntity(e:ServerEntityEvent) {
        if(e.kind=="UPDATE") {
            val ue = e as ServerEntityUpdateEvent
            val e = heaven[ue.id]
            if(e is Entity) {
                val pd = descriptor.properties[ue.property]
                if(pd!=null && pd.oneToMany) {
                    console.log("RELATION UPDATE ${pd.property}:${pd.entity}")
                    if(ue.value!=null) ALL.galaxies[pd.entity]!!.relationAdded(e, ue.property, ue.value.toString())
                    else ALL.galaxies[pd.entity]!!.relationRemoved(e, ue.property, ue.old.toString())
                } else {
                    e.update(ue)
                    interests.values().forEach {
                        it.updated(e, ue.property, ue.old)
                    }
                }
            }
        }
    }

    fun updated(entity:Entity, property:String, old:Any?) {
        interests.values.forEach {
            it.updated(entity, property, old)
        }
    }

    fun relationAdded(source:Entity, relation:String, added:String) {
        source.related(relation)
    }

    fun relationRemoved(source:Entity, relation:String, added:String) {
        source.unrelated(relation)
    }

    fun addRelation(source:Entity, property:String, add:String) {
        val req = Ajax("${APP!!.HTTP}${APP!!.base}/${descriptor.entity}/entity/${source.id}/$property/add/$add", "GET")
        req.start()
    }

    fun removeRelation(source:Entity, property:String, rem:String) {
        val req = Ajax("${APP!!.HTTP}${APP!!.base}/${descriptor.entity}/entity/${source.id}/$property/remove/$rem", "GET")
        req.start()
    }

    val relations : MutableMap<String,Interest> = HashMap()

    public fun goodRelations(entity:String, rid:String) {
        val that = this
        //TODO: can we optimise this not to use an interest for relations V -> V?
        if(entity !in relations) {
            ALL.galaxies[entity]!!.create("${descriptor.entity}relations${entity}") {
                console.log("Good Vibrations ${it.name} ${it.id}")
                that.relations[entity] = it
                it.eager = true
                keepRelations(it)
                it.plus(rid)
            }
        } else {
            relations[entity]!!.plus(rid)
        }
    }

    private fun keepRelations(i:Interest) {
        val that = this
        i.on {
              if(it is InterestEntityEvent)  {
                  val le = it
                  that.descriptor.properties.values.filter {
                      it.relation && (it.entity == le.entity.descriptor.entity)
                  }.forEach {
                      val pd = it
                      that.heaven.values.filter {
                          it[pd.property] == le.entity.id
                      }.forEach {
                          //console.log("vibe from ${le.entity.descriptor.entity}.${le.entity.id} -${pd.property}-> ${it.descriptor.entity}.${it.id}");
                          updated(it, pd.property, it[pd.property])
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
public open class InterestRemoveEvent(interest:Interest, val ids:Array<String>) : InterestEvent(interest)
public open class InterestOrderEvent(interest:Interest, val order:Array<String>) : InterestEvent(interest)
public open class InterestEntityEvent(interest:Interest, val entity:Entity) : InterestEvent(interest)
public open class InterestLoadEvent(interest:Interest, entity:Entity, val idx:Int) : InterestEntityEvent(interest, entity)
public open class InterestUpdateEvent(interest:Interest, entity:Entity, val idx:Int, val property:String, val old:Any?) : InterestEntityEvent(interest, entity)

public enum class SortDirection {ASC, DESC}
public class SortKey(val property:String, var direction:SortDirection) {fun toggle() {if(direction==SortDirection.ASC) direction=SortDirection.DESC else direction=SortDirection.ASC} }

public class Interest(val id:Int, val name:String, val galaxy:Galaxy, private val subject:Subject<InterestEvent> = Subject()) {
    var eager : Boolean = false
    val order : MutableList<String> = ArrayList()
    var offset : Int = 0
    var limit : Int = 0
    var size : Int = 0
    var estimated : Int = 0
    val orderBy : MutableList<SortKey> = ArrayList()


    fun sort(keys:Array<SortKey>) {
        val req = Ajax("${APP!!.HTTP}${APP!!.base}/${galaxy.descriptor.entity}/$id/orderBy", "POST")
        val js = Array(keys.size) {
            val k = keys[it]
            val j = JSON.parse<Json>("{}")
            j.set("property", k.property)
            j.set("direction", k.direction.name())
            j
        }

        req.start(JSON.stringify(js))
    }

    fun cfg(cfg:ServerInterestConfigEvent) {
       size = cfg.currentsize
       estimated = cfg.estimatedsize
       console.log("${galaxy.descriptor.entity}.$id: current: $size estimated: $estimated offset: $offset limit: $limit")
       offset = cfg.offset
       limit  = cfg.limit
       val oa = cfg.orderBy
        orderBy.clear()
        oa.forEach {
            //console.debug(it)
            orderBy.add(SortKey(it.get("property")!!.toString(), SortDirection.valueOf(it.get("direction")!!.toString())))
        }
        subject.onNext(InterestConfigEvent(this))
    }

    fun filter(f:Filter) {
        filterJson(f.json)
    }

    fun refresh() {
        val req = Ajax("${APP!!.HTTP}${APP!!.base}/${galaxy.descriptor.entity}/$id/refresh", "GET")
        req.start()
    }

    fun filterJson(f:Json) {
        val req = Ajax("${APP!!.HTTP}${APP!!.base}/${galaxy.descriptor.entity}/filter/$id", "POST")
        req.start(JSON.stringify(f))
    }

    fun buffer(offset:Int, limit:Int) {
        val req = Ajax("${APP!!.HTTP}${APP!!.base}/${galaxy.descriptor.entity}/$id/offset/$offset/limit/$limit", "GET")
        req.start()
    }

    fun addEntity(e:Entity) {
        plus(e.id)
    }

    fun plus(aid:String) {
        val req = Ajax("${APP!!.HTTP}${APP!!.base}/${galaxy.descriptor.entity}/$id/add/${aid}", "GET")
        req.start()
        if(eager) {
            galaxy.retrieve(listOf(aid))
        }
    }

    fun remove(e:Entity) {
        val req = Ajax("${APP!!.HTTP}${APP!!.base}/${galaxy.descriptor.entity}/$id/remove/${e.id}", "GET")
        req.start()
    }

    fun clear() {
        val req = Ajax("${APP!!.HTTP}${APP!!.base}/${galaxy.descriptor.entity}/$id/clear", "GET")
        req.start()
    }

    fun get(idx:Int) : Entity? {
        val eid = order[idx]
        if(eid !in galaxy && !eager) galaxy.retrieve(listOf(eid))
        return galaxy[eid]
    }

    fun entity(eid:String) = galaxy[eid]

    fun onOrder(nwo:Array<String>) {
        val ol = Array<String>(order.size()) {order[it]}

        order.clear()
        //subject.onNext(InterestRemoveEvent(this, ol));
        for(it in nwo) order.add(it)
        if(eager) galaxy.retrieve(order)
        //console.warn("${galaxy.descriptor.entity}.${id} order now")
        //console.log(order)
        subject.onNext(InterestOrderEvent(this, order.toTypedArray()))
    }

    fun retrieved(ids:Iterable<Entity>) {
        //console.log("$name RETRIEVED $ids")
        ids.filter { val aid = it.id;  order.any {it==aid}  }.map { Pair(order.indexOf(it.id), it) }.forEach {
            //console.log("ILOADEVT ${it.first}")
            subject.onNext(InterestLoadEvent(this, it.second, it.first))
        }
    }

    fun consume(ev:ServerInterestEvent) {
        when (ev.kind) {
            "ORDER" -> onOrder((ev as ServerInterestOrderEvent).order)
            "INTEREST" -> cfg((ev as ServerInterestConfigEvent))
            "ADD" -> {
                val ae = ev as ServerInterestAddEvent
                if(order.indexOf(ae.id)<0) {
                  val no = ArrayList<String>()
                  no.addAll(order)
                  no.add(ae.id)
                  onOrder(no.toTypedArray())
                }
            }
            "REMOVE" -> {
                val ae = ev as ServerInterestRemoveEvent
                if(order.indexOf(ae.id)>=0) {
                val no = ArrayList<String>()
                no.addAll(order)
                no.remove(ae.id)
                onOrder(no.toTypedArray())
                }
            }
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
        if(offset+limit<size) {
            console.log("$offset -> ${offset+limit}")
            buffer(offset+limit, limit)
        }
    }
}

abstract class Filter() {
    val json : Json get() = serialise()

    abstract fun serialise() : Json
}

abstract class EntityFilter(val descriptor:EntityDescriptor) : Filter() {

    override fun serialise(): Json {
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
        if(op=="LIKE" || op=="NOTLIKE") json.set("value", "$value")
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
package ch.passenger.kinterest.kijs.ui

import ch.passenger.kinterest.kijs.model.*
import ch.passenger.kinterest.kijs.*
import moments.*
import org.w3c.dom.*
import rx.js.*
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.Event
import java.util.ArrayList

/**
 * Created by svd on 10/01/2014.
 */
abstract class EntityRendererEditor<T:HTMLElement>(val interest:Interest, val creator:Boolean=false, id:String=BaseComponent.id(), name:String="div") : Component<T>(id, name), CellRendererEditor {
    private var eid : Long? = null
    var create : Entity? = null
    var entity: Entity?
        get() = if(creator) create else interest.galaxy.heaven[eid]
        set(v) {
            if(creator) create = v
            eid = v?.id; update()
        }
    open fun wants(ev: InterestUpdateEvent): Boolean = entity?.id == ev.entity.id


}

class ActionListRenderer(ai:Interest, id:String=BaseComponent.id()) : EntityRendererEditor<HTMLDivElement>(ai, false, id) {
    val actions : MutableList<ActionComponent> = ArrayList()
    override fun wants(ev: InterestUpdateEvent): Boolean = entity?.id == ev.entity.id

    override fun initialise(n: HTMLDivElement) {
        val d = this
        d.addClass("actions")
    }

    override fun update() {
        actions.forEach { it.entity = entity }
    }

    fun addAction(a:ActionComponent) {
        console.log("adding action")
        console.log(a)
        actions.add(a)
        plus(a)
    }
}

abstract class ActionComponent(interest:Interest, id:String=BaseComponent.id()) : EntityRendererEditor<HTMLButtonElement>(interest, false, id, "button") {
    private var eid : Long? = null


    override fun initialise(n: HTMLButtonElement) {
        root.`type` = "button"
        this.addClass("action")
        this.enabled(entity!=null)
        on("click") {
            it.preventDefault()
            invoke(it)
        }
        init()
    }


    override fun update() {
        this.enabled(entity!=null)
    }
    open protected fun init() {

    }

    abstract protected fun invoke(e:Event)
}

class RemoveEntityAction(interest:Interest, id:String=BaseComponent.id()) : ActionComponent(interest, id) {
    override fun update() {
        enabled(entity!=null)
    }


    override fun init() {
        addClass("remove")
    }
    override fun invoke(e: Event) {
        val e = entity
        if(e!=null) interest.remove(e)
    }
    override fun wants(ev: InterestUpdateEvent): Boolean = ev.entity.id == entity?.id
}

class CommitRenderer(interest:Interest, creator: Boolean = false, id:String=BaseComponent.id()) : EntityRendererEditor<HTMLDivElement>(interest, creator, id) {
    val subject: Subject<Boolean> = Subject()

    var save: Anchor? = null
    var cancel: Anchor? = null
    var alwaysCancel : Boolean = false
      get() = field || creator


    override fun wants(ev: InterestUpdateEvent): Boolean = entity?.id == ev.entity.id


    override fun update() {
        if (entity?.dirty?:false) {
            save?.show()
            cancel?.show()
        } else {
            save?.hide()
            if(!alwaysCancel) cancel?.hide()
        }
    }
    override fun initialise(n: HTMLDivElement) {
        val d = this
        d.addClass("committer")
        val that = this
        save = d.anchor {
            +"Save"
            addClass("commit")
            on("click") {
                that.entity?.save()
                that.subject.onNext(true)
            }
            hide()
        }
        cancel = d.anchor {
            +"Cancel"
            addClass("revert")
            on("click") {
                that.entity?.revert()
                that.subject.onNext(false)
            }
            if (!that.alwaysCancel) hide()
        }
    }
}

open class PropertyRendererEditor(val property: String, intererst: Interest, creator: Boolean = false, id:String=BaseComponent.id()) : EntityRendererEditor<HTMLDivElement>(intererst, creator, id) {
    var container: PropertyRendererEditor? = null
    val pd: PropertyDescriptor = intererst.galaxy.descriptor.properties[property]!!
    var rendererOnly: Boolean = false
        get()  {
            return if(creator) {
                field
            } else {
                field || pd.readonly
            }
        }
        set(v) {
            field = v
        }
    var editorOnly: Boolean = false
        get() = field && !rendererOnly
    open var renderer: Tag<*> = Span() { }
    private var theeditor :Tag<*> = TextInput()
    open fun editor(): Tag<*> = theeditor
    open val conflict: Tag<*> = Span() { }
    open val conflictIndicator: Tag<*> = Span() { val sp = this; addClass("conflict"); on("mouseenter") { sp.addClass("fa-spin") }; on("mouseleave") { sp.removeClass("fa-spin") } }
    val canEdit : Boolean = (creator&&(pd.readonly||!pd.nullable)) || (!creator&&!pd.readonly)

    override fun initialise(n: HTMLDivElement) {
        val d = this
        container = d
        d.addClass(property)
        d.addClass("proprenderer")
        if(canEdit) d.addClass("editable")
        if(pd.nullable) d.addClass("nullable")
        d.data("property", property)

        if(pd.nullable && !pd.readonly) {
            d.anchor {
                val that= this
                addClasses("nullifier", "fa")
                click {
                    val e = d.entity
                    console.log("nullify ${e?.id}:${d.property}")
                    if(e!=null) e[d.property] = null
                }
                on("mouseenter") {
                    that.addClass("fa-spin")
                }
                on("mouseleave") {
                    that.removeClass("fa-spin")
                }
            }
        }

        if (rendererOnly || !editorOnly) {
            hideEditor()
            showRenderer()
        }
        if (editorOnly) {
            showEditor()
            hideRenderer()
        }

        if (!pd.readonly || creator) {
            editGesture()
        }

        hideConflictIndicator()
        hideConflict()
        conflictGesture()


        /*
        intererst.on {
            when(it) {
                is InterestUpdateEvent -> if(it.entity.id==entity?.id) update()
            }
        }
        */

        d + renderer
        d + editor()
        d + conflictIndicator
        d + conflict

        renderer.addClass("renderer")
        editor().addClass("editor")
        conflictIndicator.addClass("conflict")
        conflict.addClass("conflictrenderer")

        init()
    }

    open fun init() {}

    override fun update() {
        updateRenderer()
        updateEditor()
        updateConflict()
        if(entity?.descriptor?.properties?.containsKey(property)?:false) {
        if (entity?.conflicted(property)?:false) {
            container?.addClass("conflicted")
            showConflictIndicator()
        } else container?.removeClass("conflicted")
        if (entity?.isDirty(property)?:false) {
            container?.addClass("dirty")
        } else container?.removeClass("dirty")
        if (entity?.conflicted(property)?:false) showConflictIndicator()
        }
    }

    open fun updateRenderer() {
        (renderer as Span).root.textContent = str()
    }

    open fun updateEditor() {
        (editor() as Input).value = str()
    }

    open fun updateConflict() {
        (conflict as Span).root.textContent = conflictvalue()?.toString()?:"---"
    }

    open fun showConflictIndicator() = conflictIndicator.show()
    open fun hideConflictIndicator() = conflictIndicator.hide()

    open fun showConflict() = conflict.show()
    open fun hideConflict() = conflict.hide()


    open fun conflictGesture() {
        conflictIndicator.on("mouseenter") {
            showConflict()
        }
        conflictIndicator.on("mouseleave") {
            hideConflict()
        }
    }

    open fun editGesture() {
        renderer.on("dblclick") {
            hideRenderer()
            showEditor()
            editor().root.focus()
        }
        editor().on("change") {
            onChange()
        }
        editor().on("blur") {
            hideEditor()
            showRenderer()
        }
    }

    open fun value(v: Any?) {
        val e = entity;
        console.log("$e $property $v ${e?.hasProperty(property)}")
        if (e != null && e.hasProperty(property)) {
            if (v == null && !pd.nullable) throw IllegalArgumentException(property)
            e[property] = pd.cast(v)
        }
    }

    open fun onChange() {
        val ti = editor() as Input
        console.log("setting value: ${ti.value}")
        value(ti.value)
    }

    open fun showEditor() {
        addClass("editing")
        editor().show()
    }

    open fun hideEditor() {
        removeClass("editing")
        editor().hide()
    }

    open fun showRenderer() {
        renderer.show()
    }

    open fun hideRenderer() {
        renderer.hide()
    }

    open fun conflictvalue(): Any? = entity?.realValue(property)

    protected open fun str(): String {
        if(!(entity?.hasProperty(property)?:false)) return ""
        return entity?.get(property)?.toString()?:"---"
    }


    override fun wants(ev: InterestUpdateEvent): Boolean = ev.property == property

    companion  object {
        fun bestFor(interest: Interest, property: String, creator:Boolean=false): PropertyRendererEditor {
            console.log("BEST $property")
            val pd = interest.galaxy.descriptor.properties[property]!!
            if (pd.scalar) return IntegerRenderEdit(property, interest, creator)
            if (pd.floaty) return NumberRenderEdit(property, interest, creator)
            if (pd.daty) return DateRenderEdit(property, interest, creator)
            if(pd.enum) return  EnumRenderEdit(property, interest, creator)
            if(pd.relation) return CompleterRenderEdit(property, interest, creator)
            return PropertyRendererEditor(property, interest, creator)
        }
    }
}


open class DateRenderEdit(property: String, interest: Interest, creator:Boolean=false) : PropertyRendererEditor(property, interest, creator) {
    val format = "DD.MM.YYYY"


    override fun updateEditor() {
        val v: Any? = if (entity == null) null else entity!![property]
        val d = v as Moment?
        (editor() as TextInput).value = if (d == null) "" else d.format(format)
    }

    override fun value(v: Any?) {
        val e = entity;
        if (e != null) {
            if (v == null && !pd.nullable) throw IllegalArgumentException(property)
            e[property] = moments.moment(v.toString(), format)
        }
    }


    override fun str(): String {
        val e = entity
        if (e != null) {
            val m = e[property] as Moment?
            return m?.format(format)?:""
        }
        return ""
    }
}


open class NumberRenderEdit(property: String, interest: Interest, creator:Boolean=false) : PropertyRendererEditor(property, interest, creator) {
    private val ani : NumberInput = NumberInput()
    override fun editor(): Tag<out HTMLElement> = ani

    override fun value(v: Any?) {
        val e = entity
        if (e == null) return

        if (!pd.nullable && v == null) throw IllegalStateException(property)
        e[property] = if (v == null) v else safeParseDouble(v.toString())!!
    }


    override fun updateEditor() {
        var v: Number? = null
        val e = ani
        val ae = entity
        if (ae != null) {
            v = ae[property] as Number?
        }
        e.valuesAsNumber = v
    }
}

class IntegerRenderEdit(property: String, interest: Interest, creator:Boolean=false) : NumberRenderEdit(property, interest, creator) {
    private val ani = IntegerInput()
    override fun editor(): Tag<out HTMLElement> = ani

    override fun value(v: Any?) {
        val e = entity
        if (e == null) return

        if (!pd.nullable && v == null) throw IllegalStateException(property)
        e[property] = if (v == null) v else safeParseInt(v.toString())!!
    }
}

class EnumRenderEdit(property: String, interest: Interest, creator:Boolean=false) : PropertyRendererEditor(property, interest, creator) {
    private val asel : SelectOne = SelectOne()
    override fun editor(): Tag<out HTMLElement> = asel


    override fun init() {
        super<PropertyRendererEditor>.init()

        pd.enumvalues.forEach {
            asel.option(it, it) {}
        }
        if(pd.nullable) asel.option("", "---") {}
    }


    override fun onChange() {
        val sel = asel.selectedValue
        value(sel)
    }
    override fun value(v: Any?) {
        val e = entity
        if(e==null) return
        if(v==null || v.toString().isEmpty()) {
            if(!pd.nullable) throw IllegalArgumentException()
        }
        e[property] = v.toString()
    }
}

class TextAreaEdit(property: String, interest: Interest, creator:Boolean=false) : PropertyRendererEditor(property, interest, creator) {
    private val ta = TextArea()
    override fun editor(): Tag<out HTMLElement> = ta

    init {
        editorOnly = true
    }

    override fun onChange() {
        val ti = editor() as TextArea
        console.log("setting value: ${ti.root.value}")
        value(ti.root.value)
    }

    override fun editGesture() {
        editor().on("input") {
            onChange()
        }
    }


    override fun updateEditor() {
        ta.root.value = str()
    }


}


class CompleterRenderEdit(property: String, interest: Interest, creator: Boolean = false) : PropertyRendererEditor(property, interest, creator) {
    private var targetEntity : String = interest.galaxy.descriptor.properties[property]!!.entity
    private var targetGalaxy : Galaxy = ALL.galaxies[targetEntity]!!
    var completer : CustomCompleter = CustomCompleter(targetGalaxy, createProjections(), findLabel())
    private var adiv : Div = Div();
    init {adiv.plus(completer); completer.silent = true}
    override fun editor() : Tag<out HTMLElement> = adiv

    fun createProjections(): Array<String> {
        val pit = targetGalaxy.descriptor.properties.values().filter { !it.nullable && it.datatype.endsWith("String") }.map { it.property }
        val c = pit.count()
        val ait = pit.iterator()
        return Array<String>(c) { ait.next() }
    }

    fun findLabel(): String {
        var lbl = targetGalaxy.descriptor.properties.values().firstThat { it.label }?.property
        if(lbl==null) {
            lbl = targetGalaxy.descriptor.properties.values().firstThat { it.datatype.endsWith("String") && it.unique }?.property
        }
        if(lbl==null) lbl = "id"
        return lbl!!
    }


    val label : String

    init {
        label = targetGalaxy.descriptor.properties.values().firstThat { it.label }?.property?:"";
        console.log("label $label")
    }


    override fun editGesture() {
        editor()
        renderer.on("dblclick") {
            hideRenderer()
            showEditor()
            console.log("start completing on ${editor().id}")
            editor().root.focus()
        }


        /*
        completer.onBlur {
            console.log("BLURRRRR")

            hideEditor()
            showRenderer()
        }
        */

        completer.on {
            hideEditor()
            showRenderer()
            value(it?.id)
            console.log("completer select: ")
            console.log(it?:"none")
        }
    }


    override fun showEditor() {
        addClass("editing")
        console.log("completer $id show: list#${completer.list.id}")
        editor().show()
        completer.silent =false
    }


    override fun hideEditor() {
        removeClass("editing")
        editor().hide()
        completer.silent =true
    }
    override fun str(): String {
        val e = entity
        if(e==null) return ""
        val v = e[property]
        if(v==null) return ""
        val re = targetGalaxy.heaven[v]
        return re?.get(label)?.toString()?:"---"
    }
}



class HeaderRenderer(val interest: Interest, val property: String, var label: String = property, val sortable: Boolean = true, id: String = BaseComponent.id()) : Component<HTMLDivElement>(id) {

    override fun initialise(n: HTMLDivElement) {
        val d = this
        val that = this
        d.on("click") {
            val ev = it as MouseEvent
            if (!ev.metaKey) {
                if (that.sortable) {
                    val ex = that.interest.orderBy.firstThat { it.property == that.property }
                    if(ex is SortKey) {
                        ex.toggle()
                        that.interest.sort(arrayOf(ex))
                    } else {
                        that.interest.sort(arrayOf(SortKey(that.property, SortDirection.ASC)))
                    }
                }
            } else {
                val ex = that.interest.orderBy.firstThat { it.property == that.property }
                if (ex is SortKey) {
                    ex.toggle()
                } else {
                    that.interest.orderBy.add(SortKey(that.property, SortDirection.ASC))
                }
                that.interest.sort(that.interest.orderBy.toTypedArray())
            }
        }

        d.span { +that.label; addClass(that.property) }
        interest.on {
            when(it) {
                is InterestConfigEvent -> {
                    val ob = that.interest.orderBy
                    val sk = ob.firstThat { it.property == that.property }
                    val cna = "sortasc"
                    val cnd = "sortdesc"
                    d.removeClass(cna)
                    d.removeClass(cnd)
                    if (sk != null) {
                        d.addClass("sort" + sk.direction.name().toLowerCase())
                    }
                }
            }
        }
    }
}
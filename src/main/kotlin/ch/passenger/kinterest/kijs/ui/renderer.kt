package ch.passenger.kinterest.kijs.ui

import ch.passenger.kinterest.kijs.model.*
import ch.passenger.kinterest.kijs.dom.*
import ch.passenger.kinterest.kijs.*
import moments.*
import js.dom.html.*
import rx.js.*
import org.w3c.dom.events.MouseEvent

/**
 * Created by svd on 10/01/2014.
 */
trait EntityRendererEditor : CellRendererEditor {
    var entity: Entity?
    fun wants(ev: InterestUpdateEvent): Boolean
}

class CommitRenderer(val creator: Boolean = false) : Component<HTMLDivElement>(), EntityRendererEditor {
    val subject: Subject<Boolean> = Subject()
    override var entity: Entity? = null
    var save: Anchor? = null
    var cancel: Anchor? = null

    override fun wants(ev: InterestUpdateEvent): Boolean = entity?.id == ev.entity.id


    override fun update() {
        if (entity?.dirty?:false) {
            save?.show()
            cancel?.show()
        } else {
            save?.hide()
            if(!creator) cancel?.hide()
        }
    }
    override fun node(): HTMLDivElement {
        val d = Div()
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
            if (!that.creator) hide()
        }

        return d.root
    }
}

open class PropertyRendererEditor(val property: String, val intererst: Interest, var creator: Boolean = false) : Component<HTMLDivElement>(), EntityRendererEditor {
    var container: Div? = null
    override var entity: Entity? = null
        set(v) {
            $entity = v;update()
        }
    val pd: PropertyDescriptor = intererst.galaxy.descriptor.properties[property]!!
    var rendererOnly: Boolean = false
        get()  {
            return if(creator) {
                $rendererOnly
            } else {
                $rendererOnly || pd.readonly
            }
        }
        set(v) {
            $rendererOnly = v
        }
    var editorOnly: Boolean = false
        get() = $editorOnly && !rendererOnly
    open var renderer: Tag<*> = Span() { }
    private var theeditor :Tag<*> = TextInput()
    open fun editor(): Tag<*> = theeditor
    open val conflict: Tag<*> = Span() { }
    open val conflictIndicator: Tag<*> = Span() { val sp = this; addClass("conflict"); on("mouseenter") { sp.addClass("fa-spin") }; on("mouseleave") { sp.removeClass("fa-spin") } }
    val canEdit : Boolean = (creator&&(pd.readonly||!pd.nullable)) || (!creator&&!pd.readonly)

    override fun node(): HTMLDivElement {
        val d = Div()
        container = d
        d.addClass(property)
        d.addClass("proprenderer")
        if(canEdit) d.addClass("editable")
        d.data("property", property)


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

        return d.root
    }

    open fun init() {}

    override fun update() {
        updateRenderer()
        updateEditor()
        updateConflict()
        if (entity?.conflicted(property)?:false) {
            container?.addClass("conflicted")
            showConflictIndicator()
        } else container?.removeClass("conflicted")
        if (entity?.isDirty(property)?:false) {
            container?.addClass("dirty")
        } else container?.removeClass("dirty")
        if (entity?.conflicted(property)?:false) showConflictIndicator()
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
        if (e != null) {
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
        editor().show()
    }

    open fun hideEditor() {
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
        return entity?.get(property)?.toString()?:"---"
    }


    override fun wants(ev: InterestUpdateEvent): Boolean = ev.property == property

    class object {
        fun bestFor(interest: Interest, property: String, creator:Boolean=false): PropertyRendererEditor {
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

class CompleterRenderEdit(property: String, interest: Interest, creator: Boolean = false) : PropertyRendererEditor(property, interest, creator) {
    var completer : CustomCompleter? = null
    private var adiv : Div = initEditor()
    override fun editor() : Tag<out HTMLElement> = adiv

    fun initEditor(): Div {
        val g = ALL.galaxies[pd.entity]!!
        val pit = g.descriptor.properties.values().filter { !it.nullable && it.datatype.endsWith("String") }.map { it.property }
        val c = pit.count()
        val ait = pit.iterator()
        val pa = Array<String>(c) { ait.next() }
        var lbl = g.descriptor.properties.values().firstThat { it.label }?.property
        if(lbl==null) {
            lbl = g.descriptor.properties.values().firstThat { it.datatype.endsWith("String") && it.unique }?.property
        }
        if(lbl==null) lbl = "id"
        val root = Div()
        completer = CustomCompleter(g, pa, lbl!!)

        root + completer!!
        console.log("completer ready")
        return root
    }

    val label : String;

    {
        label = ALL.galaxies[pd.entity]!!.descriptor.properties.values().firstThat { it.label }?.property?:"";
        console.log("label $label")
    }


    override fun editGesture() {
        editor()
        renderer.on("dblclick") {
            hideRenderer()
            showEditor()
            renderer.root.focus()
        }


        completer!!.onBlur {
            console.log("BLURRRRR")
            hideEditor()
            showRenderer()
        }

        completer!!.on { value(it?.id) }
    }

    override fun str(): String {
        if(completer==null) return ""
        return "${completer?.selected?.get(label)}"
    }
}



class HeaderRenderer(val interest: Interest, val property: String, var label: String = property, val sortable: Boolean = true, id: String = BaseComponent.id()) : Component<HTMLDivElement>(id) {

    override fun node(): HTMLDivElement {
        val d = Div()
        val that = this
        d.on("click") {
            val ev = it as MouseEvent
            if (!ev.shiftKey) {
                if (that.sortable) {
                    val ex = that.interest.orderBy.firstThat { it.property == that.property }
                    if(ex is SortKey) {
                        ex.toggle()
                        that.interest.sort(array(ex))
                    } else {
                        that.interest.sort(array(SortKey(that.property, SortDirection.ASC)))
                    }
                }
            } else {
                val ex = that.interest.orderBy.firstThat { it.property == that.property }
                if (ex is SortKey) {
                    ex.toggle()
                } else {
                    that.interest.orderBy.add(SortKey(that.property, SortDirection.ASC))
                }
                that.interest.sort(that.interest.orderBy.copyToArray())
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
        return d.root
    }
}
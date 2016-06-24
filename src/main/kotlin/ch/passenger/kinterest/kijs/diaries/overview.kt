package ch.passenger.kinterest.kijs.diaries

import ch.passenger.kinterest.kijs.APP
import ch.passenger.kinterest.kijs.model.*
import ch.passenger.kinterest.kijs.ui.*
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.Event
import java.util.*


/**
 * Created by svd on 13/01/2014.
 */
class OwnerView(id: String = BaseComponent.id()) : Component<HTMLDivElement>(id) {
    val cols = listOf("email", "nick", "birthdate", "editor", "buddies", "state")
    val labels = mapOf("state" to "ONLINE", "email" to "Email", "nick" to "Nickname", "birthdate" to "Born", "editor" to "Editor", "buddies" to "Friends")
    var interest: Interest? = null
    var table: InterestTable? = null
    var buddies: InterestTable? = null
    var detail: GenericEntityEditor? = null
    var currentFocus: Entity? = null
    val focusHeader: Span = Span() { textContent = "---" }


    override fun readyState(): Boolean = false

    override fun initialise(n: HTMLDivElement) {
        val d = this
        val that = this


        ALL.galaxies["DiaryOwner"]!!.create("owneroverview") {
            that.table = InterestTable(it)
            that.interest = it
            that.iAmReady()
            that.table?.colorder?.clear()
            that.table!!.colorder.addAll(that.cols)
            that.table!!.createColumns()
            that.labels.forEach { entry -> that.table?.label(entry.key as String, entry.value as String) }

            that.table?.addActions {
                val r = ActionListRenderer(it)

                r.addAction(object : ActionComponent(it) {

                    override fun wants(ev: InterestUpdateEvent): Boolean = entity?.id == ev.entity.id

                    override fun init() {
                        addClasses("detail")
                    }
                    override fun invoke(e: Event) {
                        console.log("invoke detail")
                        console.log(that.detail?:"???")
                        entity?.let {
                            that.detail?.entity = it
                        }
                        that.detail?.show()
                    }
                })
                r.addAction(object : ActionComponent(it) {
                    override fun wants(ev: InterestUpdateEvent): Boolean = entity?.id == ev.entity.id

                    override fun init() {
                        addClasses("focus")
                    }
                    override fun invoke(e: Event) {
                        that.currentFocus = entity
                        console.log("focusing on ${entity?.id}")
                        update()
                    }


                    override fun update() {
                        super<ActionComponent>.update()
                        if (that.currentFocus == null) {
                            that.focusHeader.textContent = "---"
                            that.table?.tableBody!!.rows.forEach {
                                it.removeClass("focus")
                            }
                            return
                        }
                        if (that.currentFocus?.id != entity?.id) return
                        that.focusHeader.textContent = that.currentFocus!!["nick"]?.toString()?:"???"
                        that.table?.tableBody!!.rows.forEach {
                            it.removeClass("focus")
                            if (that.currentFocus != null && it.data("entity") == "${that.currentFocus?.id}") {
                                it.addClass("focus")
                            }
                        }
                        val bt = that.buddies
                        if (bt != null) {
                            if (that.currentFocus != null) {
                                val fs = "DiaryOwner.buddies <- id = ${that.currentFocus!!.id}"
                                console.log("buddy filter: fs")
                                bt.interest.filterJson(APP!!.filterParser!!.parse<Json>(fs)!!)
                            } else {
                                val fs = "id < 0"
                                bt.interest.filterJson(APP!!.filterParser!!.parse<Json>(fs)!!)
                            }
                        }
                    }
                })

                r.addAction(object : ActionComponent(it) {
                    override fun wants(ev: InterestUpdateEvent): Boolean = entity?.id == ev.entity.id

                    override fun init() {
                        addClasses("friend")
                    }
                    override fun invoke(e: Event) {
                        if (that.currentFocus != null && entity != null) {
                            val en = that.currentFocus!!.descriptor.entity
                            ALL.galaxies[en]!!.addRelation(that.currentFocus!!, "buddies", entity!!.id)
                        }
                    }
                })

                r.addAction(object : ActionComponent(it) {
                    override fun wants(ev: InterestUpdateEvent): Boolean = entity?.id == ev.entity.id

                    override fun init() {
                        addClasses("unfriend")
                    }
                    override fun invoke(e: Event) {
                        if (that.currentFocus != null && entity != null) {
                            val en = that.currentFocus!!.descriptor.entity
                            ALL.galaxies[en]!!.removeRelation(that.currentFocus!!, "buddies", entity!!.id)
                        }
                    }
                })
                r
            }
            that.table?.committer = true
            that.table?.label("committer", "Commit")
            d.plus(that.table!!)
            that.detail = GenericEntityEditor(it)
            that.detail?.hide()
            that.detail?.commitRenderer?.alwaysCancel = true
            that.detail?.commitRenderer?.subject?.subscribe { that.detail?.hide() }
            d.plus(that.detail!!)

            it.sort(arrayOf(SortKey("nick", SortDirection.ASC)))
            it.buffer(0, 5)
            it.filterJson(APP!!.filterParser!!.parse<Json>("id >= 0")!!)
            ALL.galaxies["DiaryOwner"]!!.create("buddies") {
                that.buddies = InterestTable(it)

                that.buddies!!.colorder.addAll(listOf("nick", "email", "birthdate", "state"))
                that.buddies!!.createColumns()
                that.labels.keys.forEach {
                    that.table?.label(it, that.labels[it]!!)
                }
                d + that.focusHeader
                d + that.buddies!!
                it.buffer(0, 5)
                it.sort(arrayOf(SortKey("nick", SortDirection.ASC)))
            }
        }


    }
}

class Diaries(id: String = BaseComponent.id()) : Component<HTMLDivElement>(id) {
    val cols = listOf("title", "owner", "created")
    val labels = mapOf("title" to "Diary", "owner" to "Owner", "created" to "Created")
    var interest: Interest? = null
    var table: InterestTable? = null
    var creator: GenericEntityEditor? = null


    override fun readyState(): Boolean = false

    override fun initialise(n: HTMLDivElement) {
        val d = this
        val that = this

        ALL.galaxies["Diary"]!!.create("diaries") {
            that.table = InterestTable(it)
            that.interest = it
            that.iAmReady()
            that.table?.colorder?.clear()
            that.table!!.colorder.addAll(that.cols)
            that.table!!.createColumns()
            that.labels.keys.forEach {
                that.table?.label(it, that.labels[it]!!)
            }
            that.table?.committer = true
            that.table?.label("committer", "Commit")

            that.table?.addActions {
                val r = ActionListRenderer(it)

                r.addAction(object : ActionComponent(it) {

                    override fun wants(ev: InterestUpdateEvent): Boolean = entity?.id == ev.entity.id

                    override fun init() {
                        addClasses("create")
                        enabled(true)
                    }


                    override fun update() {
                        super<ActionComponent>.update()
                        enabled(true)
                    }

                    override fun invoke(e: Event) {
                        console.log("invoke create entry")
                        if(entity!=null) {
                            val te = that.creator?.entity
                            if (te != null) te["diary"] = entity!!.id
                            that.creator?.show()
                        }
                        /*
                        val s = that.table?.selected?.firstThat { true }
                        if (s != null) {
                            val e = that.interest!!.entity(s)
                            val te = that.creator?.entity
                            if (te != null) te["diary"] = e.id
                        }
                        */


                    }
                })
                r
            }
            ALL.galaxies["DiaryEntry"]!!.create("creator") {
                that.creator = GenericEntityEditor(it, true)
                that.creator?.hide()
                that.creator?.commitRenderer?.alwaysCancel = true
                that.creator?.commitRenderer?.subject?.subscribe { that.creator?.hide() }

                d.plus(that.table!!)
                d.plus(that.creator!!)
                it.sort(arrayOf(SortKey("created", SortDirection.ASC)))
                it.buffer(0, 5)
            }
        }
    }
}

class EntryPane(id: String = BaseComponent.id()) : Component<HTMLDivElement>(id) {
    val cols = listOf("title", "dated", "created")
    val labels = mapOf("title" to "Title", "dated" to "Date", "created" to "Created")
    var table: InterestTable? = null
    var editor: EntryEditor? = null

    override fun initialise(n: HTMLDivElement) {
        val that = this

        ALL.galaxies["DiaryEntry"]!!.create("entries") {
            that.table = InterestTable(it)
            that.editor = EntryEditor(it)
            that.editor?.hide()
            that.table?.colorder?.clear()
            that.table!!.colorder.addAll(that.cols)
            that.table!!.createColumns()
            that.labels.keys.forEach {
                that.table?.label(it, that.labels[it]!!)
            }
            that.table?.committer = true
            that.table?.label("committer", "Commit")

            that.table?.addActions {
                actionList(it, that.editor!!)
            }

            that.plus(that.table!!)
            that.plus(that.editor!!)
            it.sort(arrayOf(SortKey("created", SortDirection.ASC)))
            it.buffer(0, 5)
        }
    }

    fun actionList(ai: Interest, editor: EntryEditor): ActionListRenderer {
        val that = this
        val al = ActionListRenderer(ai)
        al.addAction(object : ActionComponent(ai) {

            override fun init() {
                super<ActionComponent>.init()
                addClass("edit")
            }
            override fun invoke(e: Event) {
                console.log("showing editor for ${entity!!["title"]} on ${that.editor?.id}")
                editor.select(entity)
                editor.show()
            }
        })

        return al
    }
}

class EntryEditor(interest: Interest, id: String = BaseComponent.id()) : EntityEditor<HTMLDivElement>(interest, id) {
    val tae = TextAreaEdit("content", interest)
    val cr = CommitRenderer(interest, false)

    override fun initialise(n: HTMLDivElement) {
        val that = this
        tae.editorOnly = true
        tae.att("cols", "160")
        tae.att("rows", "5")
        this + tae

        cr.alwaysCancel = true
        disposables.add(cr.subject.subscribe { that.hide() })
        this + cr
    }

    fun select(e: Entity?) {
        if (e != null) console.log("$id Content: ${e["content"]}")
        this.entity = e
        update()
    }


    override fun update() {
        console.log("Entry UPDATE $entity")
        tae.entity = this.entity
        cr.entity = entity

        tae.update()
        cr.update()
    }
}

class OverviewPanel(id: String = BaseComponent.id()) : Component<HTMLDivElement>(id) {
    var diaries: Boolean = false
    var entries: Boolean = false
    val lastDiaries: MutableList<String> = ArrayList()
    override fun initialise(n: HTMLDivElement) {
        val d = this
        val ownerView = OwnerView()
        d + ownerView
        ownerView.onReady {
            if (!d.diaries) {
                d.diaries = true
                console.log("!!!!diaries ready!!!!")
                val diaries = Diaries()

                val entryPane = EntryPane()

                diaries.onReady {
                    if (!d.entries) {
                        d.entries = true

                        diaries.table!!.onSelection {
                            val sel = it
                            console.log("selected diary $sel")
                            val chg = d.lastDiaries.size==0 || d.lastDiaries.any { !sel.contains(it) } || sel.any { !d.lastDiaries.contains(it) }
                            console.log("chg: $chg")
                            if (chg) {
                                d.lastDiaries.clear()
                                d.lastDiaries.addAll(sel)
                                val f = it.map { " id = $it" }.joinToString(" or ", "diary -> (", ")")
                                console.log("Filtering Entries ... $f")
                                if (it.count() > 0) {
                                    val fj = APP!!.filterParser!!.parse<Json>(f)
                                    fj?.set("entity", "DiaryEntry")
                                    val i = entryPane.table!!.interest
                                    if (fj != null) {
                                        i.filterJson(fj)
                                    }
                                }
                            }
                        }
                    }
                }
                d + diaries
                d + entryPane

                ownerView.table!!.onSelection {
                    diaries.table?.setSelection(listOf())
                    val nofilter = "id < 0"
                    val nf = APP!!.filterParser!!.parse<Json>(nofilter)!!
                    nf.set("entity", "DiaryEntry")
                    entryPane.table?.interest?.filterJson(nf)

                    if (it.count() > 0) {
                        val f = it.map { " id = ${it}" }.joinToString(" or ", "owner -> (", ")")
                        console.log("Diary filter: $f")
                        val fj = APP!!.filterParser!!.parse<Json>(f)
                        fj?.set("entity", "Diary")
                        val i = diaries.table?.interest
                        if (fj != null && i != null) i.filterJson(fj)
                    }

                }
            }
        }
    }
}

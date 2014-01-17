package ch.passenger.kinterest.kijs.diaries

import ch.passenger.kinterest.kijs.ui.BaseComponent
import ch.passenger.kinterest.kijs.listOf
import ch.passenger.kinterest.kijs.model.ALL
import ch.passenger.kinterest.kijs.APP
import ch.passenger.kinterest.kijs.dom.console
import ch.passenger.kinterest.kijs.ui.Component
import js.dom.html.HTMLDivElement
import ch.passenger.kinterest.kijs.ui.InterestTable
import ch.passenger.kinterest.kijs.model.Interest
import ch.passenger.kinterest.kijs.ui.Div
import rx.js.Subject
import ch.passenger.kinterest.kijs.mapOf
import ch.passenger.kinterest.kijs.to
import ch.passenger.kinterest.kijs.forEach
import ch.passenger.kinterest.kijs.model.SortKey
import ch.passenger.kinterest.kijs.model.SortDirection
import java.util.ArrayList
import ch.passenger.kinterest.kijs.makeString
import ch.passenger.kinterest.kijs.map
import ch.passenger.kinterest.kijs.ui.ActionListRenderer
import ch.passenger.kinterest.kijs.ui.ActionComponent
import ch.passenger.kinterest.kijs.model.InterestUpdateEvent
import ch.passenger.kinterest.kijs.ui.EntityRendererEditor
import ch.passenger.kinterest.kijs.ui.GenericEntityEditor
import org.w3c.dom.events.Event
import ch.passenger.kinterest.kijs.any
import ch.passenger.kinterest.kijs.firstThat
import ch.passenger.kinterest.kijs.ui.TextArea
import ch.passenger.kinterest.kijs.ui.TextAreaEdit
import ch.passenger.kinterest.kijs.ui.CommitRenderer
import ch.passenger.kinterest.kijs.model.Entity
import ch.passenger.kinterest.kijs.ui.EntityEditor

/**
 * Created by svd on 13/01/2014.
 */
class OwnerView(id:String=BaseComponent.id()) : Component<HTMLDivElement>(id) {
    val cols = listOf("email", "nick", "birthdate", "editor", "buddies")
    val labels = mapOf("email" to "Email", "nick" to "Nickname", "birthdate" to "Born", "editor" to "Editor", "buddies" to "Friends")
    var interest : Interest? = null
    var table : InterestTable? = null
    var detail : GenericEntityEditor? = null
    var currentFocus : Entity? = null


    override fun readyState(): Boolean = false

    override fun initialise(n: HTMLDivElement) {
        val d = this
        val that = this


        ALL.galaxies["DiaryOwner"]!!.create("owneroverview") {
            that.table=InterestTable(it)
            that.interest = it
            that.iAmReady()
            that.table?.colorder?.clear()
            that.table!!.colorder.addAll(that.cols)
            that.table!!.createColumns()
            that.labels.keySet().forEach {
                that.table?.label(it, that.labels[it]!!)
            }

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
                        that.detail?.entity = entity
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
                        that.table?.tableBody!!.rows.forEach {
                            it.removeClass("focus")
                            if(that.currentFocus!=null && it.data("entity") == "${that.currentFocus?.id}") {
                                it.addClass("focus")
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
                        if(that.currentFocus!=null && entity!=null) {
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
                        if(that.currentFocus!=null && entity!=null) {
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

            it.sort(array(SortKey("nick", SortDirection.ASC)))
            it.buffer(0, 5)
            it.filterJson(APP!!.filterParser!!.parse<Json>("id >= 0")!!)
            it
        }
    }
}

class Diaries(id:String= BaseComponent.id()) : Component<HTMLDivElement>(id) {
    val cols = listOf("title", "owner", "created")
    val labels = mapOf("title" to "Name", "owner" to "Owner", "created" to "Created")
    var interest : Interest? = null
    var table : InterestTable? = null
    var creator : GenericEntityEditor? = null


    override fun readyState(): Boolean = false

    override fun initialise(n: HTMLDivElement) {
        val d = this
        val that = this

        ALL.galaxies["Diary"]!!.create("diaries") {
            that.table=InterestTable(it)
            that.interest = it
            that.iAmReady()
            that.table?.colorder?.clear()
            that.table!!.colorder.addAll(that.cols)
            that.table!!.createColumns()
            that.labels.keySet().forEach {
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
                        val s = that.table?.selected?.firstThat { true }
                        if(s!=null) {
                            val e = that.interest!!.entity(s)
                            val te = that.creator?.entity
                            if(te!=null) te["diary"] = e.id
                        }

                        that.creator?.show()
                    }
                })
                r
            }
            ALL.galaxies["DiaryEntry"]!!.create("creator") {
                that.creator = GenericEntityEditor(it, true)
                that.creator?.hide()
                that.creator?.commitRenderer?.alwaysCancel=true
                that.creator?.commitRenderer?.subject?.subscribe { that.creator?.hide() }

                d.plus(that.table!!)
                d.plus(that.creator!!)
                it.sort(array(SortKey("created", SortDirection.ASC)))
                it.buffer(0, 5)
            }
        }
    }
}

class EntryPane(id:String=BaseComponent.id()) : Component<HTMLDivElement>(id) {
    val cols = listOf("title", "dated", "created")
    val labels = mapOf("title" to "Title", "dated" to "Date", "created" to "Created")
    var table : InterestTable? = null
    var editor : EntryEditor? = null

    override fun initialise(n: HTMLDivElement) {
        val that = this

        ALL.galaxies["DiaryEntry"]!!.create("entries") {
            that.table=InterestTable(it)
            that.editor = EntryEditor(it)
            that.editor?.hide()
            that.table?.colorder?.clear()
            that.table!!.colorder.addAll(that.cols)
            that.table!!.createColumns()
            that.labels.keySet().forEach {
                that.table?.label(it, that.labels[it]!!)
            }
            that.table?.committer = true
            that.table?.label("committer", "Commit")

            that.table?.addActions {
                actionList(it, that.editor!!)
            }

            that.plus(that.table!!)
            that.plus(that.editor!!)
            it.sort(array(SortKey("created", SortDirection.ASC)))
            it.buffer(0, 5)
        }
    }

    fun actionList(ai:Interest, editor:EntryEditor) : ActionListRenderer {
        val that = this
        val al = ActionListRenderer(ai)
        al.addAction(object : ActionComponent(ai) {

            override fun init() {
                super<ActionComponent>.init()
                addClass("edit")
            }
            override fun invoke(e: Event) {
                console.log("showing editor for ${entity!!["title"]} on ${that.editor?.id}")
                editor?.select(entity)
                editor?.show()
            }
        })

        return al
    }
}

class EntryEditor(interest:Interest, id:String=BaseComponent.id()) : EntityEditor<HTMLDivElement>(interest, id) {
    val tae = TextAreaEdit("content", interest)
    val cr = CommitRenderer(interest, false)

    override fun initialise(n: HTMLDivElement) {
        val that = this
        tae.editorOnly = true
        tae.att("cols", "60")
        tae.att("rows", "5")
        this+tae

        cr.alwaysCancel = true
        disposables.add(cr.subject.subscribe { that.hide() })
        this+ cr
    }

    fun select(e:Entity?) {
        if(e!=null) console.log("$id Content: ${e["content"]}")
        entity = e
        if(entity?.id==e?.id)
          update()
    }


    override fun update() {
        console.log("Entry UPDATE")
        tae.entity = entity
        cr.entity = entity

        tae.update()
        cr.update()
    }
}

class OverviewPanel(id:String=BaseComponent.id()) : Component<HTMLDivElement>(id) {
    var diaries : Boolean = false
    var entries : Boolean = false
    override fun initialise(n: HTMLDivElement) {
        val d = this
        val ownerView = OwnerView()
        d+ ownerView
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
                            val f = it.map { "diary -> id = $it" }.makeString(" or ")
                            if(f.size>0) {
                                val fj = APP!!.filterParser!!.parse<Json>(f)
                                fj?.set("entity", "DiaryEntry")
                                val i = entryPane.table!!.interest
                                if(fj!=null) {
                                    console.log("Filtering Entries ... $f")
                                    i.filterJson(fj)
                                }
                            }
                        }
                    }
                }
                d+ diaries
                d+entryPane

                ownerView.table!!.onSelection {
                    diaries.table?.setSelection(listOf())
                    val nofilter = "id < 0"
                    val nf = APP!!.filterParser!!.parse<Json>(nofilter)!!
                    nf.set("entity", "DiaryEntry")
                    entryPane.table?.interest?.filterJson(nf)
                    val f = it.map{ "(owner -> id = ${it})" }.makeString(" or ")
                    console.log("Diary filter: $f")
                    if (f.size>0) {
                        val fj = APP!!.filterParser!!.parse<Json>(f)
                        fj?.set("entity", "Diary")
                        val i = diaries.table?.interest
                        if(fj!=null && i!=null) i.filterJson(fj)
                    }
                }
            }

        }
    }
}


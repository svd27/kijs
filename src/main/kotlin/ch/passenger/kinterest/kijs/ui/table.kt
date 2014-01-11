package ch.passenger.kinterest.kijs.ui

import js.dom.html.HTMLTableElement
import js.dom.html.*
import java.util.ArrayList
import ch.passenger.kinterest.kijs.dom.console

/**
 * Created by svd on 08/01/2014.
 */
open class Table(id:String=BaseComponent.id()) : Tag<HTMLTableElement>("table", id) {
    var tbody : TableBody? = null

    public fun header(id:String=BaseComponent.id(), init:TableHeader.()->Unit) : TableHeader {
        val th = TableHeader(id)
        th.init()
        plus(th)
        return th
    }

    public fun footer(id:String=BaseComponent.id(), init:TableFooter.()->Unit) : TableFooter {
        val th = TableFooter(id)
        th.init()
        plus(th)
        return th
    }

    public fun body(id:String=BaseComponent.id(), init:TableBody.()->Unit) : TableBody {
        val th = TableBody(id)
        th.init()
        plus(th)
        tbody = th
        return th
    }
}


open class TableHeader(id:String=BaseComponent.id()) : Tag<HTMLTableSectionElement>("thead", id) {
    fun tr(id:String=BaseComponent.id(), init:TableRow.()->Unit) : TableRow {
        val el = TableRow(id)
        return add(el, init)
    }
}

open class TableFooter(id:String=BaseComponent.id()) : FlowContainer<HTMLTableSectionElement>("tfoot", id)

open class TableBody(id:String=BaseComponent.id()) : Tag<HTMLTableSectionElement>("tbody", id) {
    val rows : MutableList<TableRow> = ArrayList()

    fun tr(id:String=BaseComponent.id(), init:TableRow.()->Unit) : TableRow {
        val el = TableRow(id)
        rows.add(el)
        return add(el, init)
    }

    fun remove(idx:Int): TableRow? {
        if(idx>=rows.size()) {
            console.warn("row remove: $idx>=${rows.size()}")
            return null
        }
        val c = rows[idx]
        console.log("removing row")
        console.log(c)
        rows.remove(c)
        c.remove()
        c.dispose()
        return c
    }
}

open class TableRow(id:String=BaseComponent.id()) : Tag<HTMLTableRowElement>("tr", id) {
    val cells : MutableList<AbstractTableCell> = ArrayList()

    fun td(id:String=BaseComponent.id(), init:TableCell.()->Unit) : TableCell {
        val el = TableCell(id)
        cells.add(el)
        return add(el, init)
    }

    fun th(id:String=BaseComponent.id(), init:TableHeaderCell.()->Unit) : TableHeaderCell {
        val el = TableHeaderCell(id)
        cells.add(el)
        return add(el, init)
    }

    fun remove(idx:Int) {
        val c = cells.remove(idx)
        c.remove()
        c.dispose()
    }
}

abstract class AbstractTableCell(name:String, id:String=BaseComponent.id()) : FlowContainer<HTMLTableCellElement>(name, id) {
    var renderer:CellRendererEditor? = null
    set(v) {
        val r = $renderer
        if(r is BaseComponent<*>) {
            r.remove()
            r.dispose()
        }
        $renderer = v
        if(v is BaseComponent<*>) {
            plus(v)
        }
    }
    fun update() = renderer?.update()
}

open class TableCell(id:String=BaseComponent.id()) : AbstractTableCell("td", id)
open class TableHeaderCell(id:String=BaseComponent.id()) : AbstractTableCell("th", id)

trait CellRendererEditor {
    fun update()
}
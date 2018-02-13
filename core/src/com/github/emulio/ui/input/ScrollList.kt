package com.github.emulio.ui.input

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane

class ScrollList(scroll: Scrollable, itemsList: Indexable)
{
    private val list = itemsList
    private val scroll = scroll

    val selectedIndex: Int get() = list.selectedIndex

    fun scroll(amount: Int) {
        val nextIndex = (this.list.selectedIndex + amount)

        list.selectedIndex = nextIndex.coerceIn(0 until  list.size.coerceAtLeast(1))

        val scrollTopIndex = calcScrollTopIndex(list.selectedIndex,scroll.size)
        scroll.setScrollTop(scrollTopIndex)
    }

    private fun calcScrollTopIndex(selectionIndex: Int, scrollSize: Int): Int {
        val scrollTopMax = (list.size - scrollSize).coerceAtLeast(0)

        val scrollCenterOffset = (scrollSize/2).coerceAtLeast(0)
        val scrollCenterMax =  (list.size - scrollCenterOffset).coerceAtLeast(0)

        if (selectionIndex < scrollCenterOffset) {
            return 0
        }

        if (selectionIndex > scrollCenterMax) {
            return scrollTopMax
        }

        return (selectionIndex - scrollCenterOffset).coerceAtLeast(0)
    }
}

interface Indexable {
    var selectedIndex: Int
    val size: Int
}

class ListWrapper(listView: com.badlogic.gdx.scenes.scene2d.ui.List<String>): Indexable {
    val list = listView

    override val size: Int
        get() = list.items.size

    override var selectedIndex: Int
        get() = list.selectedIndex
        set(value) {
            list.selectedIndex = value
        }
}

interface Scrollable {
    val size: Int
    fun setScrollTop(top: Int)
}

class ScrollWrapper(listView: com.badlogic.gdx.scenes.scene2d.ui.List<*>,scrollView: ScrollPane): Scrollable {
    private val list = listView
    val scroll = scrollView

    override val size: Int
        get() = (scroll.height/list.itemHeight).toInt()

    override fun setScrollTop(top: Int){
        scroll.scrollY = top * list.itemHeight
    }
}


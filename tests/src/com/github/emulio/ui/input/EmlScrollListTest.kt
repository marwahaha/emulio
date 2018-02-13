package com.github.emulio.ui.input

import org.junit.jupiter.api.Test
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever


internal class ScrollListTest {

    @Test
    fun scroll_by_zero_should_stay_at_same_index() {
        val (scrollList, _, mockList) = createScrollList(scrollSize = 3, listSize = 7, selectedIndex = 5)

        scrollList.scroll(0)

        verify(mockList).selectedIndex = 5
    }

    @Test
    fun selecting_beyond_list_start_stops_at_start() {
        val (scrollList, _, mockList) = createScrollList(scrollSize = 3, listSize = 7, selectedIndex = 5)

        scrollList.scroll(-20)

        verify(mockList).selectedIndex = 0
    }

    @Test
    fun selecting_beyond_list_end_stops_at_end() {
        val (scrollList, _, mockList) = createScrollList(scrollSize = 3, listSize = 7, selectedIndex = 5)

        scrollList.scroll(20)

        verify(mockList).selectedIndex = 6
    }

    @Test
    fun scrolling_by_less_than_half_the_does_not_move_the_scroll() {
        val (scrollList, mockScroll, _) = createScrollList(scrollSize = 3, listSize = 7, selectedIndex = 1)

        scrollList.scroll(1)

        verify(mockScroll).setScrollTop(0)
    }

    @Test
    fun scrolling_by_more_than_half_the_scroll_size_moves_scroll() {
        val (scrollList, mockScroll, _) = createScrollList(scrollSize = 3, listSize = 7, selectedIndex = 2)

        scrollList.scroll(2)

        verify(mockScroll).setScrollTop(1)
    }

    @Test
    fun scrolling_to_the_middle_moves_the_scroll() {
        val (scrollList, mockScroll, _) = createScrollList(scrollSize = 3, listSize = 7, selectedIndex = 4)

        scrollList.scroll(4)

        verify(mockScroll).setScrollTop(3)
    }

    @Test
    fun scrolling_to_the_end_stops_the_scroll_at_the_end() {
        val (scrollList, mockScroll, _) = createScrollList(scrollSize = 3, listSize = 7, selectedIndex = 7)

        scrollList.scroll(7)

        verify(mockScroll).setScrollTop(4)
    }

    @Test
    fun empty_list_does_not_scroll() {
        val (scrollList, mockScroll, _) = createScrollList(scrollSize = 11, listSize = 10, selectedIndex = 8)

        scrollList.scroll(7)

        verify(mockScroll).setScrollTop(0)
    }

    @Test
    fun scroll_greater_than_list_does_not_scroll() {
        val (scrollList, mockScroll, _) = createScrollList(scrollSize = 11, listSize = 10, selectedIndex = 7)

        scrollList.scroll(7)

        verify(mockScroll).setScrollTop(0)
    }


    @Test
    fun scrolling_beyond_list_start_stops_at_start() {
        val (scrollList, mockScroll, _) = createScrollList(scrollSize = 3, listSize = 10, selectedIndex = -20)

        scrollList.scroll(-20)

        verify(mockScroll).setScrollTop(0)
    }

    @Test
    fun scrolling_beyond_list_end_stops_at_end() {
        val (scrollList, mockScroll, _) = createScrollList(scrollSize = 3, listSize = 10, selectedIndex = 8)

        scrollList.scroll(20)

        verify(mockScroll).setScrollTop(7)
    }

    private fun createScrollList(scrollSize: Int, listSize: Int, selectedIndex: Int): ClassUnderTestAndStubs<String> {
        val list: EmlList = mock()
        val scroll: EmlScroll = mock()

        whenever(list.size).thenReturn(listSize)
        whenever(list.selectedIndex).thenReturn(selectedIndex)
        whenever(scroll.size).thenReturn(scrollSize)

        val scrollList = ScrollList<String>(scroll, list)

        return ClassUnderTestAndStubs(scrollList, scroll, list)
    }

    data class ClassUnderTestAndStubs<T>(val scrollList: ScrollList<T>, val scroll: EmlScroll, val list: EmlList)
}
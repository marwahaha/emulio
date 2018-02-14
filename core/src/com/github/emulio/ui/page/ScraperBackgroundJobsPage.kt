package com.github.emulio.ui.page

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.ui.List
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.czyzby.lml.annotation.LmlActor
import com.github.czyzby.lml.parser.impl.AbstractLmlView
import com.github.emulio.ui.input.*
import com.github.emulio.ui.screens.createColorTexture

class ScraperBackgroundJobsPage(stage: Stage, skin: Skin):  AbstractLmlView(stage) {
    @LmlActor("backgroundJobsTable") lateinit var table: Table
    @LmlActor("backgroundJobsScroll") lateinit var scroll: ScrollPane
    @LmlActor("backgroundJobsList") lateinit var list: List<String>

    val scrollList: ScrollList

    init {
        val parser = getLMLParser(skin)
        val template = Gdx.files.internal("templates/ScraperBackgroundJobsPage.lml")

        parser.createView(this, template)

        val scrollWrapper = ScrollWrapper(list, scroll)
        val listWrapper = ListWrapper(list)
        scrollList = ScrollList(scrollWrapper, listWrapper)

        scroll.apply {
            setFlickScroll(true)
            setScrollBarPositions(false, true)
            setScrollingDisabled(true, false)
            setSmoothScrolling(true)

            isTransform = true
        }

        list.style.apply {
            font= skin.getFont("mainFont")
            fontColorSelected = Color.WHITE
            fontColorUnselected = Color(0x878787FF.toInt())
            val selectorTexture = createColorTexture(0x878787FF.toInt())
            selection = TextureRegionDrawable(TextureRegion(selectorTexture))
        }
    }

    override fun getViewId(): String {
        return  "scraperJobsId"
    }
}
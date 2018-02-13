package com.github.emulio.ui.page

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.ui.List
import com.github.czyzby.lml.annotation.LmlActor
import com.github.czyzby.lml.parser.impl.AbstractLmlView
import com.github.czyzby.lml.vis.util.VisLml
import com.github.emulio.ui.input.*
import com.kotcrab.vis.ui.VisUI

class ScraperBackgroundJobsPage(stage: Stage, skin: Skin) {
    val view: ScraperBackgroundJobsView
    val scrollList: ScrollList

    init {

        view = ScraperBackgroundJobsView(stage,skin)

        val scrollWrapper = ScrollWrapper(view.list, view.scroll)
        val listWrapper = ListWrapper(view.list)
        scrollList = ScrollList(scrollWrapper, listWrapper)
    }

    fun show() {
        view.table.color.a = 1F
    }

    fun hide() {
        view.table.color.a = 0F
    }
}

class ScraperBackgroundJobsView(stage: Stage, skin: Skin): AbstractLmlView(stage) {
    @LmlActor("backgroundJobsTable") lateinit var table: Table
    @LmlActor("backgroundJobsScroll") lateinit var scroll: ScrollPane
    @LmlActor("backgroundJobsList") lateinit var list: List<String>

    init {
        if(!VisUI.isLoaded()){
            VisUI.load()
        }

        val parser = VisLml.parser().skin(skin).build()
        val template = Gdx.files.internal("templates/ScraperBackgroundJobsPage.lml")

        parser.createView(this, template)
    }

    override fun getViewId(): String {
        return  "scraperJobsId"
    }
}
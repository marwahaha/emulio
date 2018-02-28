package com.github.emulio.ui.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.ui.List
import com.github.czyzby.lml.annotation.LmlActor
import com.github.czyzby.lml.parser.impl.AbstractLmlView
import com.github.emulio.Emulio
import com.github.emulio.model.theme.Theme

class ScraperWindow(stage: Stage, emulio: Emulio): AbstractLmlView(stage) {
    @LmlActor("scraperPlatformImage") lateinit var platformImage: Image
    @LmlActor("scrapperWindow") lateinit var window: Window
    @LmlActor("scraperTable") lateinit var table: Table
    @LmlActor("scraperScroll") lateinit var scroll: ScrollPane
    @LmlActor("scraperList") lateinit var list: List<String>

    val scrollList: ScrollList
    init {
        val platformNames: Array<String> = emulio.platforms.map { platform -> platform.name }.toTypedArray()

        val parser = getLMLParserBuilder(emulio.skin)
                .argument("platformsNames", platformNames)
                .build()

        val template = Gdx.files.internal("templates/ScraperWindow.lml")
        parser.createView(this, template)

        val scrollWrapper = ScrollWrapper(list, scroll)
        val listWrapper = ListWrapper(list)
        scrollList = ScrollList(scrollWrapper, listWrapper)
    }

    override fun getViewId(): String {
        return  "scraperViewId"
    }

    fun updatePlatformTheme(theme: Theme){
        platformImage.name = theme.platform?.platformName
        platformImage.drawable = theme.getDrawableFromPlatformTheme()
        window.titleLabel.setText(theme.platform?.name)
    }
}
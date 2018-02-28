package com.github.emulio.ui.input

import com.badlogic.gdx.scenes.scene2d.ui.List
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.github.czyzby.lml.parser.LmlParser
import com.github.czyzby.lml.parser.tag.LmlAttribute
import com.github.czyzby.lml.parser.tag.LmlTag
import com.github.czyzby.lml.util.LmlParserBuilder
import com.github.czyzby.lml.vis.util.VisLml
import com.kotcrab.vis.ui.VisUI

fun getLMLParserBuilder(skin: Skin): LmlParserBuilder {
    if(!VisUI.isLoaded()){
        VisUI.load()
    }

    val parser = VisLml.parser()
            .skin(skin)
            .attribute(Transparent(), "transparent")
            .attribute(ListFont(), "font")

    return  parser
}

class Transparent: CustomAttribute<Widget>(Widget::class.java) {
    override fun process(parser: LmlParser, tag: LmlTag, widget: Widget, rawAttributeData: String) {
        widget.color.a = if(rawAttributeData == "true") 0F else 1F
    }
}

class ListFont: CustomAttribute<List<*>>(List::class.java) {
    override fun process(parser: LmlParser, tag: LmlTag, list: List<*>, rawAttributeData: String) {
        list.style.font = parser.data.defaultSkin.getFont(rawAttributeData)
    }
}

abstract class CustomAttribute<T>(clazz: Class<T>) : LmlAttribute<T>{
    private val clazz = clazz
    override fun getHandledType(): Class<T> {
        return clazz
    }
}
package com.github.emulio.ui.input

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.github.czyzby.lml.parser.LmlParser
import com.github.czyzby.lml.parser.tag.LmlAttribute
import com.github.czyzby.lml.parser.tag.LmlTag
import com.github.czyzby.lml.vis.util.VisLml
import com.kotcrab.vis.ui.VisUI

fun getLMLParser(skin: Skin): LmlParser {
    if(!VisUI.isLoaded()){
        VisUI.load()
    }

    val parser = VisLml.parser()
            .skin(skin)
            .attribute(Transparent(), "transparent")
            .build()

    return  parser
}

class Transparent: LmlAttribute<Actor> {
    override fun getHandledType(): Class<Actor> {
        return Actor::class.java
    }

    override fun process(parser: LmlParser, tag: LmlTag, actor: Actor, rawAttributeData: String) {
        actor.color.a = 0F
    }
}
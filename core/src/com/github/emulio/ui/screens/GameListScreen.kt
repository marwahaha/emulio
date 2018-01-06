package com.github.emulio.ui.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.ui.List
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.Timer
import com.github.emulio.Emulio
import com.github.emulio.model.Game
import com.github.emulio.model.Platform
import com.github.emulio.model.theme.*
import com.github.emulio.process.ProcessLauncher
import com.github.emulio.ui.input.InputListener
import com.github.emulio.ui.input.InputManager
import com.github.emulio.utils.DateHelper
import mu.KotlinLogging
import java.io.File

class GameListScreen(emulio: Emulio, val platform: Platform) : EmulioScreen(emulio), InputListener {

    val logger = KotlinLogging.logger { }

	private val inputController: InputManager = InputManager(this, emulio.config, stage)
	private val interpolation = Interpolation.fade
    private var games: kotlin.collections.List<Game>

    private var selectedGame: Game? = null

    private lateinit var listView: List<String>
    private lateinit var listScrollPane: ScrollPane
    private lateinit var descriptionScrollPane: ScrollPane
    private lateinit var gameImage: Image
    private lateinit var gameReleaseDate: TextField
    private lateinit var gameRating: TextField
    private lateinit var gameDescription: Label
    private lateinit var gamePlayCount: TextField
    private lateinit var gameLastPlayed: TextField
    private lateinit var gamePlayers: TextField
    private lateinit var gameGenre: TextField
    private lateinit var gamePublisher: TextField
    private lateinit var gameDeveloper: TextField
    private lateinit var root: Group
    private lateinit var logo: Image
    private lateinit var imageView: ViewImage

    private var lastTimer: Timer.Task? = null
    private var lastSequenceAction: SequenceAction? = null

	init {
		Gdx.input.inputProcessor = inputController

		games = emulio.games!![platform]?.toList() ?: emptyList<Game>()
        games = games.toList().sortedBy { it.name }

		initGUI()
	}

    private fun isBasicViewOnly(): Boolean {
        return games.none { it.id != null || it.description != null || it.image != null }
    }

    private var guiready: Boolean = false

    private fun initGUI() {
		val theme = emulio.theme[platform]!!

        if (isBasicViewOnly()) {
            buildBasicView(theme.findView("basic")!!)
        } else {
            buildDetailedView(theme.findView("detailed")!!)
        }
	}

    override fun onScreenLoad() {
        guiready = true
    }

    private fun buildBasicView(basicView: View) {
		buildCommonComponents(basicView)

		val gamelistView = basicView.findViewItem("gamelist") as TextList
        listView = buildListView(gamelistView)

        listScrollPane = ScrollPane(listView, ScrollPane.ScrollPaneStyle().apply {

        }).apply {

            setFlickScroll(true)
            setScrollBarPositions(false, true)

            setSmoothScrolling(true)

            isTransform = true

            setSize(gamelistView)
            setPosition(gamelistView)
        }

        stage.addActor(listScrollPane)

	}

	private fun buildCommonComponents(view: View) {
		val backgroundView = view.findViewItem("background") as ViewImage?
		if (backgroundView != null) {
			stage.addActor(buildImage(backgroundView).apply {
				setScaling(Scaling.stretch)
				setPosition(0f, 0f)
				setSize(screenWidth, screenHeight)
			})
		} else {
			val lightGrayTexture = createColorTexture(0xc5c6c7FF.toInt())
			stage.addActor(Image(lightGrayTexture).apply {
				setFillParent(true)
			})
		}

		val footer = view.findViewItem("footer") as ViewImage?
		if (footer != null) {
			stage.addActor(buildImage(footer, Scaling.stretch))
		}

		val header = view.findViewItem("header") as ViewImage?
		if (header != null) {
			stage.addActor(buildImage(header, Scaling.stretch))
		}

		initRoot()
		initLogoSmall()

		val systemName1 = view.findViewItem("system_name_1")?.let { it as Text }
		if (systemName1 != null) {
			stage.addActor(buildTextField(systemName1))
		}

		val systemName2 = view.findViewItem("system_name_2")?.let { it as Text }
		if (systemName2 != null) {
			stage.addActor(buildTextField(systemName2))
		}

		val logo = view.findViewItem("logo") as ViewImage?
		if (logo != null) {
			stage.addActor(buildImage(logo))
		}
	}

    private fun buildDetailedView(detailedView: View) {
        buildCommonComponents(detailedView)

        val descriptionView = detailedView.findViewItem("md_description") as Text?
        if (descriptionView != null) {

            gameDescription = buildLabel(descriptionView)
            gameDescription.setWrap(true)

            descriptionScrollPane = ScrollPane(gameDescription, ScrollPane.ScrollPaneStyle()).apply {
                setFlickScroll(true)
                setScrollBarPositions(false, true)

                setSmoothScrolling(true)
                setForceScroll(false, true)

                isTransform = true

                setSize(descriptionView)
                setPosition(descriptionView)
            }

            stage.addActor(descriptionScrollPane)
        }

        val gamelistView = detailedView.findViewItem("gamelist") as TextList
        listView = buildListView(gamelistView)

        listScrollPane = ScrollPane(listView, ScrollPane.ScrollPaneStyle()).apply {
            setFlickScroll(true)
            setScrollBarPositions(false, true)

            setSmoothScrolling(true)
            setForceScroll(false, true)

            isTransform = true

            setSize(gamelistView)
            setPosition(gamelistView)
        }
        stage.addActor(listScrollPane)


        val imageView = detailedView.findViewItem("md_image") as ViewImage?
        if (imageView != null) {
            gameImage = buildImage(imageView)
            stage.addActor(gameImage)
            this.imageView = imageView
        }

        buildLabel(detailedView, "md_lbl_rating", "Rating:")
        buildLabel(detailedView, "md_lbl_releasedate", "Released:")
        buildLabel(detailedView, "md_lbl_developer", "Developer:")
        buildLabel(detailedView, "md_lbl_publisher", "Publisher:")
        buildLabel(detailedView, "md_lbl_genre", "Genre:")
        buildLabel(detailedView, "md_lbl_players", "Players:")
        buildLabel(detailedView, "md_lbl_lastplayed", "Last played:")
        buildLabel(detailedView, "md_lbl_playcount", "Times played:")

        val playCountView = detailedView.findViewItem("md_playcount") as Text?
        if (playCountView != null) {
            gamePlayCount = buildTextField(playCountView)
            stage.addActor(gamePlayCount)
        }

        val lastPlayedView = detailedView.findViewItem("md_lastplayed") as Text?
        if (lastPlayedView != null) {
            gameLastPlayed = buildTextField(lastPlayedView)
            stage.addActor(gameLastPlayed)
        }

        val playersView = detailedView.findViewItem("md_players") as Text?
        if (playersView != null) {
            gamePlayers = buildTextField(playersView)
            stage.addActor(gamePlayers)
        }

        val genreView = detailedView.findViewItem("md_genre") as Text?
        if (genreView != null) {
            gameGenre = buildTextField(genreView)
            stage.addActor(gameGenre)
        }

        val publisherView = detailedView.findViewItem("md_publisher") as Text?
        if (publisherView != null) {
            gamePublisher = buildTextField(publisherView)
            stage.addActor(gamePublisher)
        }

        val developerView = detailedView.findViewItem("md_developer") as Text?
        if (developerView != null) {
            gameDeveloper = buildTextField(developerView)
            stage.addActor(gameDeveloper)
        }

        val releaseDateView = detailedView.findViewItem("md_releasedate") as Text?
        if (releaseDateView != null) {
            gameReleaseDate = buildTextField(releaseDateView)
            stage.addActor(gameReleaseDate)
        }

        val ratingView = detailedView.findViewItem("md_rating") as Text?
        if (ratingView != null) {
            gameRating = buildTextField(ratingView)
            stage.addActor(gameRating)
        }

	}

    private fun buildLabel(detailedView: View, viewName: String, viewText: String) {
        val lbView = detailedView.findViewItem(viewName) as Text?
        if (lbView != null) {
            stage.addActor(buildLabel(lbView).apply {

                setText(viewText)
            })
        }
    }

    private fun buildImage(image: ViewImage, scaling: Scaling = Scaling.fit, imagePath: File? = image.path): Image {
        val texture = if (imagePath != null) {
            Texture(FileHandle(imagePath), true)
        } else {
            val size = getSize(image)
            createColorTexture(0xFFCC00FF.toInt(), size.first.toInt(), size.second.toInt())
        }

		texture.setFilter(Texture.TextureFilter.MipMap, Texture.TextureFilter.MipMap)

		return Image(texture).apply {
			setScaling(scaling)

			setSize(image)
			setPosition(image)

			setOrigin(image)


            isVisible = imagePath != null
		}
	}

	private fun buildTextField(textView: Text): TextField {

		val text = if (textView.forceUpperCase) {
			textView.text?.toUpperCase() ?: ""
		} else {
			textView.text ?: ""
		}

        val color = getColor(textView.textColor ?: textView.color)

		return TextField(text, TextField.TextFieldStyle().apply {
            font = getFont(getFontPath(textView), getFontSize(textView.fontSize), color)
            fontColor = color
        }).apply {
			setAlignment(when(textView.alignment) {
				TextAlignment.LEFT -> Align.left
				TextAlignment.RIGHT -> Align.right
				TextAlignment.CENTER -> Align.center
				TextAlignment.JUSTIFY -> Align.left //TODO
			})
			setSize(textView)
			setPosition(textView)
		}
	}

    private fun buildLabel(textView: Text): Label {
        val text = if (textView.forceUpperCase) {
            textView.text?.toUpperCase() ?: ""
        } else {
            textView.text ?: ""
        }

        val color = getColor(textView.textColor ?: textView.color)
        val font = getFont(getFontPath(textView), getFontSize(textView.fontSize), color)

        return Label(text, Label.LabelStyle(font, color)).apply {
            setAlignment(Align.topLeft)
            setSize(textView)
            setPosition(textView)
        }
    }

	private fun buildListView(gamelistView: TextList): List<String> {

		return List<String>(List.ListStyle().apply {
            fontColorUnselected = getColor(gamelistView.primaryColor)
            fontColorSelected = getColor(gamelistView.selectedColor)

            /**
             * color font should not be defined, since it will use the
             * fontColorUnselected and fontColorSelected definitions above
             */
            font = getFont(getFontPath(gamelistView), getFontSize(gamelistView.fontSize))

			val selectorTexture = createColorTexture(Integer.parseInt(gamelistView.selectorColor + "FF", 16))
			selection = TextureRegionDrawable(TextureRegion(selectorTexture))

            // TODO: alignment of list items?
            // TODO: horizontal marquee on lists? it is implemented?
            // of not, how to do? It is possible?

		}).apply {
            setSize(gamelistView)

            gamelistView.forceUpperCase

//            games.forEach { game ->
//                items.add(game.name ?: game.path.name)
//            }

            games.forEachIndexed() { idx, game ->
                items.add("$idx ${game.name ?: game.path.name}")
            }
		}

	}

	private fun Widget.setOrigin(viewItem: ViewItem) {
		if (viewItem.originX != null && viewItem.originY != null) {
			val originX = viewItem.originX!!
			val originY = viewItem.originY!!

			val offsetX = if (originX == 0f) {
				0f
			} else {
				width * originX
			}

			val offsetY = when (originY) {
                0f -> 0f
                1f -> height
                else -> height * (1f - viewItem.originY!!)
            }

			setOrigin(offsetX, offsetY)

			x += offsetX
			y += offsetY
		}
	}

    private fun getSize(viewItem: ViewItem): Pair<Float, Float> {
        var width = if (viewItem.sizeX != null) {
            screenWidth * viewItem.sizeX!!
        } else {
            200f
        }

        var height = if (viewItem.sizeY != null) {
            screenHeight * viewItem.sizeY!!
        } else {
            200f
        }

        if (viewItem.maxSizeX != null) {
            width = Math.max(width, screenWidth * viewItem.maxSizeX!!)
        }
        if (viewItem.maxSizeY != null) {
            height = Math.max(height, screenHeight * viewItem.maxSizeY!!)
        }
        return Pair(width, height)
    }

	private fun Actor.setSize(viewItem: ViewItem) {
		var width = if (viewItem.sizeX != null) {
			screenWidth * viewItem.sizeX!!
		} else {
			this.width
		}

		var height = if (viewItem.sizeY != null) {
			screenHeight * viewItem.sizeY!!
		} else {
			this.height
		}

		if (viewItem.maxSizeX != null) {
			width = Math.min(width, screenWidth * viewItem.maxSizeX!!)
		}
		if (viewItem.maxSizeY != null) {
			height = Math.min(height, screenHeight * viewItem.maxSizeY!!)
		}
		setSize(width, height)
	}

	private fun Actor.setPosition(view: ViewItem) {
		val x = screenWidth * view.positionX!!
		val y = (screenHeight * (1f - view.positionY!!)) - height

		setPosition(x, y)
	}

	private fun getFontPath(textView: Text): FileHandle {
        return if (textView.fontPath != null) {
            FileHandle(textView.fontPath!!.absolutePath)
        } else{
            Gdx.files.internal("fonts/RopaSans-Regular.ttf")
        }
	}

	private fun getFontSize(fontSize: Float?): Int {
        return if (fontSize == null) {
            90
        } else {
            (fontSize * screenHeight).toInt()
        }
	}

	private fun initRoot() {
		root = Group().apply {
			width = screenWidth
			height = screenHeight
			x = 0f
			y = 0f
		}
		stage.addActor(root)
	}

	private fun initLogoSmall() {
		logo = Image(Texture("images/logo-small.png")).apply {
			x = screenWidth
			y = (height / 2) - 5f
			addAction(Actions.moveTo(screenWidth - width - 15f, y, 0.5f, interpolation))
		}
		root.addActor(logo)
	}

	override fun hide() {
	}

	override fun render(delta: Float) {
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
		stage.act(Math.min(Gdx.graphics.deltaTime, 1 / 30f))
		stage.draw()
		inputController.update(delta)
	}

	override fun pause() {
	}

	override fun resume() {
	}

	override fun resize(width: Int, height: Int) {
	}

	override fun dispose() {
        super.dispose()
        inputController.dispose()
	}

    private fun launchGame() {

        if (listView.selectedIndex == -1) {
            listView.selectedIndex = 0
        }

        val selectedGame = games[listView.selectedIndex]
        logger.info { "launchGame: ${selectedGame.path.name}" }

        val command = platform.runCommand.map {
            when {
                it.contains("%ROM_RAW%") ->
                    it.replace("%ROM_RAW%", selectedGame.path.absolutePath)
                it.contains("%ROM%") ->
                    it.replace("%ROM%", selectedGame.path.absolutePath) //TODO check emulationstation documentation
                it.contains("%BASENAME%") ->
                    it.replace("%BASENAME%", selectedGame.path.nameWithoutExtension)
                else -> it
            }
        }

        emulio.minimizeApplication()
        ProcessLauncher.executeProcess(command.toTypedArray())
        emulio.restoreAplication()
    }

    private fun selectNext(amount: Int = 1) {
        val nextIndex = listView.selectedIndex + amount

        if (amount < 0) {
            if (nextIndex < 0) {
                listView.selectedIndex = listView.items.size + amount
            } else {
                listView.selectedIndex = nextIndex
            }
        }

        if (amount > 0) {
            if (nextIndex >= listView.items.size) {
                listView.selectedIndex = 0
            } else {
                listView.selectedIndex = nextIndex
            }
        }

        selectedGame = games[listView.selectedIndex]

        checkVisible(nextIndex)
        updateGameSelected()

    }

    private fun updateGameSelected() {
        lastTimer?.cancel()
        lastSequenceAction?.reset()

        if (isBasicViewOnly()) {
            return
        }

        if (selectedGame == null) {
            gameImage.isVisible = false
            return
        }

        val game = selectedGame!!

        val hasImage = game.image != null && game.image.isFile
        val texture = if (hasImage) {
            Texture(FileHandle(game.image), true)
        } else {
            Texture(0, 0, Pixmap.Format.RGB888)
        }

        texture.setFilter(Texture.TextureFilter.MipMap, Texture.TextureFilter.MipMap)
        gameImage.drawable = TextureRegionDrawable(TextureRegion(texture))
        gameImage.isVisible = hasImage

        gameReleaseDate.text = safeValue(if (game.releaseDate != null) {
            DateHelper.format(game.releaseDate)
        } else {
            null
        })

        gameDeveloper.text = safeValue(game.developer)
        gameRating.text = "x"//safeValue(game.rating)

        gamePlayCount.text = "0"
        gameLastPlayed.text = "Never"
        gamePlayers.text = safeValue(game.players)
        gameGenre.text = safeValue(game.genre)
        gamePublisher.text = safeValue(game.publisher)
        gameDeveloper.text = safeValue(game.developer)

        descriptionScrollPane.scrollY = 0f
        gameDescription.setText(safeValue(game.description, ""))

        animateDescription()

    }

    private fun animateDescription() {
        lastTimer = Timer.schedule(object : Timer.Task() {


            override fun run() {

                if (gameDescription.height <= descriptionScrollPane.height) {
                    return
                }

                val scrollAmount = gameDescription.height - descriptionScrollPane.height
                val actionTime = scrollAmount * 0.05f


                val sequenceAction = SequenceAction(
                        ScrollByAction(0f, scrollAmount, actionTime),
                        Actions.delay(2f),
                        ScrollByAction(0f, -scrollAmount, actionTime)
                )

                lastSequenceAction = sequenceAction
                descriptionScrollPane.addAction(sequenceAction)
            }
        }, 2.5f)
    }

    private fun safeValue(string: String?, defaultText: String = "Unknown"): String {
        return string?.trim() ?: defaultText
    }

    private fun checkVisible(index: Int) {
        val itemHeight = listView.itemHeight

        val selectionY = index * itemHeight
        val selectionY2 = selectionY + itemHeight

        val minItemsVisible = itemHeight * 5

        if (listView.selectedIndex == games.size - 1) {
            listScrollPane.scrollY = listView.height - listScrollPane.height
            return
        }

        if (listView.selectedIndex == 0) {
            listScrollPane.scrollY = 0f
            return
        }

        if ((selectionY2 + minItemsVisible) > listScrollPane.height) {
            listScrollPane.scrollY = (selectionY2 - listScrollPane.height) + minItemsVisible
        }

        val minScrollY = Math.max(selectionY - minItemsVisible, 0f)

        if (minScrollY < listScrollPane.scrollY) {
            listScrollPane.scrollY = minScrollY
        }
    }

    override fun onConfirmButton(): Boolean {
        launchGame()
        return true
    }


    override fun onCancelButton(): Boolean {
        if (!guiready) return false

        switchScreen(PlatformsScreen(emulio, platform))
        return true
    }

    override fun onUpButton(): Boolean {
        if (!guiready) return false

        selectNext(-1)
        return true
    }

    override fun onDownButton(): Boolean {
        logger.debug { "onDownButton ${System.identityHashCode(this)} ${platform.platformName} $guiready" }
        if (!guiready) return false

        selectNext()
		return true
	}


    override fun onLeftButton(): Boolean {
        logger.debug { "onLeftButton ${System.identityHashCode(this)} ${platform.platformName} $guiready" }
        if (!guiready) return false

        val platforms = emulio.platforms
        val index = platforms.indexOf(platform)

        val previousPlatform = if (index > 0) {
            index - 1
        } else {
            platforms.size - 1
        }

        switchScreen(GameListScreen(emulio, platforms[previousPlatform]))
		return true
	}

	override fun onRightButton(): Boolean {
        logger.debug { "onRightButton ${System.identityHashCode(this)} ${platform.platformName} $guiready" }
        if (!guiready) return false

        val platforms = emulio.platforms
        val index = platforms.indexOf(platform)

        val previousPlatform = if (index > platforms.size - 2) {
            0
        } else {
            index + 1
        }
        switchScreen(GameListScreen(emulio, platforms[previousPlatform]))
		return true
	}

	override fun onFindButton(): Boolean {
        logger.debug { "onFindButton ${System.identityHashCode(this)} ${platform.platformName} $guiready" }
        if (!guiready) return false
		return true
	}
    
	override fun onOptionsButton(): Boolean {
        logger.debug { "onOptionsButton ${System.identityHashCode(this)} ${platform.platformName} $guiready" }
        if (!guiready) return false
		return true
	}

	override fun onSelectButton(): Boolean {
        logger.debug { "onSelectButton ${System.identityHashCode(this)} ${platform.platformName} $guiready" }
        if (!guiready) return false
		return true
	}

	override fun onPageUpButton(): Boolean {
        logger.debug { "onPageUpButton ${System.identityHashCode(this)} ${platform.platformName} $guiready" }
        if (!guiready) return false
        selectNext(-10)
        return true
	}

	override fun onPageDownButton(): Boolean {
        logger.debug { "onPageDownButton ${System.identityHashCode(this)} ${platform.platformName} $guiready" }
        if (!guiready) return false
        selectNext(10)
		return true
	}

	override fun onExitButton(): Boolean {
        logger.debug { "onExitButton ${System.identityHashCode(this)} ${platform.platformName} $guiready" }
        if (!guiready) return false
		return true
	}

}

class ScrollByAction(private val endScrollX: Float, private val endScrollY: Float, duration: Float) : TemporalAction(duration) {

    private lateinit var scrollPane: ScrollPane

    private var startScrollX: Float = -1f
    private var startScrollY: Float = -1f

    override fun begin() {
        scrollPane = target as ScrollPane
        startScrollX = scrollPane.scrollX
        startScrollY = scrollPane.scrollY
    }

    override fun update(percent: Float) {
        scrollPane.scrollX = startScrollX + (endScrollX - startScrollX) * percent
        scrollPane.scrollY = startScrollY + (endScrollY - startScrollY) * percent
    }
}
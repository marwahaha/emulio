package com.github.emulio.ui.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Timer
import com.github.emulio.Emulio
import com.github.emulio.model.EmulioConfig
import com.github.emulio.model.Platform
import com.github.emulio.model.theme.Theme
import com.github.emulio.runners.PlatformReader
import com.github.emulio.runners.ThemeReader
import com.github.emulio.ui.reactive.GdxScheduler
import com.github.emulio.utils.gdxutils.Subscribe
import com.github.emulio.yaml.YamlUtils
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import mu.KotlinLogging
import java.io.File


class SplashScreen(emulio: Emulio) : EmulioScreen(emulio) {

	val logger = KotlinLogging.logger { }

	lateinit var platforms: List<Platform>

	private val lbLoading: Label

	private var imgLogo: Image

    init {
		logger.debug { "create()" }

        fadeAnimation = false

		stage.addActor(Image(createColorTexture(0x6FBBDBFF)).apply {
			setFillParent(true)
			color.a = 0f
			addAction(SequenceAction(Actions.fadeIn(0.1f),
					Actions.run {
						val soundIntro = Gdx.audio.newSound(Gdx.files.internal("sounds/sms.mp3"))
						soundIntro.play()
					}))
		})

        Gdx.input.inputProcessor = stage

		imgLogo = Image(Texture("images/logo.png"))

		imgLogo.color.a = 0f


		imgLogo.x = (screenWidth - imgLogo.width) / 2
		imgLogo.y = (screenHeight - imgLogo.height) / 2

		val imgLogoPartial = Image(Texture("images/logo-partial.png")).apply {
			x = (screenWidth - width) / 2
			y = ((screenHeight - height) / 2) - height
			color.a = 0f
		}
		stage.addActor(imgLogoPartial)

		val maskGroup = Group().apply {
			width = screenWidth
			x = 0f
			y = 0f

			color.a = 0f
			addAction(Actions.fadeIn(0.2f))

			addActor(Image(createColorTexture(0x6FBBDBFF)).apply {
				x = 0f
				y = 0f
				height = ((screenHeight - imgLogoPartial.height) / 2)
				width = screenWidth
			})

		}
		stage.addActor(maskGroup)
		stage.addActor(imgLogo)

		imgLogoPartial.addAction(SequenceAction(
			Actions.delay(0.2f),
			Actions.fadeIn(0.1f),
			Actions.moveTo((screenWidth - imgLogoPartial.width) / 2, (screenHeight - imgLogoPartial.height) / 2, 0.4f),
			Actions.run {
				maskGroup.remove()
				imgLogo.addAction(SequenceAction(
						Actions.delay(0.5f),
						Actions.fadeIn(1f),
						Actions.run {
							maskGroup.remove()
						})
				)
			})
		)

		val mainFont = freeTypeFontGenerator.generateFont(FreeTypeFontGenerator.FreeTypeFontParameter().apply {
			size = 20
			color = Color(0x37424AFF)
		})

		lbLoading = Label("Initializing main interface", Label.LabelStyle().apply {
			font = mainFont
		})
		lbLoading.setPosition(10f, 5f)
		stage.addActor(lbLoading)
		lbLoading.setText("Loading configurations")

		observeConfig()

	}

    private fun observeConfig() {
		val observable: Observable<Pair<EmulioConfig, Boolean>> = Observable.create({ subscriber ->
            val configFile = File(emulio.workdir, "emulio-config.yaml")

            val createConfig = !configFile.exists()
            if (createConfig) {
                logger.info { "Configuration file not found, creating a default" }
				YamlUtils.saveEmulioConfig(configFile, initializeEmulioConfig())
			}

			subscriber.onNext(Pair(YamlUtils.parseEmulioConfig(configFile), createConfig))
			subscriber.onComplete()
		})

		observable
				.subscribeOn(Schedulers.computation())
				.observeOn(GdxScheduler)
				.subscribe({ pair ->
                    val (config, _) = pair

					emulio.config = config
                    emulio.data["lastInput"] = if (config.gamepadConfig.isEmpty()) {
                        config.keyboardConfig
                    } else {
                        config.gamepadConfig.values.first()
                    }

                    lbLoading.setText("Loading platforms")
                    observePlatforms()

				}, { onError(it) })
	}

	private fun initializeEmulioConfig(): EmulioConfig {
		return EmulioConfig().apply {
			loadDefaults()
		}
	}


	private fun observePlatforms() {
		val platformsObservable: Observable<List<Platform>> = Observable.create({ subscriber ->
			val platforms = PlatformReader(emulio).invoke()
			subscriber.onNext(platforms)
			subscriber.onComplete()
		})

		platformsObservable
				.subscribeOn(Schedulers.computation())
				.observeOn(GdxScheduler)
				.subscribe({
					onPlatformsLoaded(it)

				}, { onError(it) })
	}

	private fun onError(exception: Throwable) {
        val message = exception.message ?: "An internal error have occurred, please check your configuration files."

        lbLoading.setText(message)
		lbLoading.setPosition(10f, 20f)

		logger.error(exception, { "An internal error have occurred, please check your configuration files." })

        showErrorDialog(message)
		// Exit app on keypress?
	}

	fun onPlatformsLoaded(platforms: List<Platform>) {
		this.platforms = platforms
		emulio.platforms = platforms

		lbLoading.setText("Loading theme")

		val start = System.currentTimeMillis()

		val themesMap = mutableMapOf<Platform, Theme>()

        val themeName = emulio.config.uiConfig.themeName
        val themesFolder = File(emulio.workdir, "themes")
        val themeFolder  = File(themesFolder, themeName)

        if (themeName == "simple" && !themeFolder.isDirectory) {
            ThemeReader
                .extractSimpleTheme(themeFolder, themeName, emulio)
                .subscribeOn(Schedulers.computation())
                .observeOn(GdxScheduler)
                    .Subscribe(
                        onNext = { pair ->
                            val (percent, file) = pair
                            lbLoading.setText("Extracting initial theme. ${percent.toInt()}% completed (${file.name})")
                        },
                        onError =  { ex ->
                            onError(ex)
                        },
                        onComplete = {
                            loadTheme(platforms, themeFolder, themesMap, start)
                    })
        } else {
            if (!themeFolder.isDirectory) {
                error("Theme '$themeName' not found in the ${themesFolder.absolutePath}, please check your theme files.")
            }

            loadTheme(platforms, themeFolder, themesMap, start)
        }
	}

    private fun loadTheme(platforms: List<Platform>, themedir: File, themesMap: MutableMap<Platform, Theme>, start: Long) {
        ThemeReader
                .readTheme(platforms, themedir)
                .subscribeOn(Schedulers.computation())
                .observeOn(GdxScheduler)
                .Subscribe(
                        onNext = { theme ->
                            val platform = theme.platform!!
                            val platformName = platform.platformName
                            logger.debug { "theme read for platform '$platformName'" }
                            lbLoading.setText("Loading theme for platform $platformName")

                            themesMap.put(platform, theme)
                        },
                        onError = { ex ->
                            onError(ex)
                        },
                        onComplete = {
                            logger.debug { "theme loaded in ${System.currentTimeMillis() - start}ms " }

                            lbLoading.setText("Loading...")
                            emulio.theme = themesMap

                            //TODO improve the implementation below..
                            // here we need to detect if the animation is already done
                            // before we call the switch screen method.. In this way
                            // emulio is taking 1 second longer on loading screen.

                            Timer.schedule(object : Timer.Task() {
                                override fun run() {
                                    switchScreen(PlatformsScreen(emulio))
                                }
                            }, 2f)

                        })
    }


    override fun render(delta: Float) {
		//Gdx.gl.glClearColor(0x6F, 0xBB, 0xDB, 0xFF)
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
		stage.act(Math.min(Gdx.graphics.deltaTime, 1 / 30f))
		stage.draw()
	}

	override fun resize(width: Int, height: Int) {
		stage.viewport.update(width, height, true)
	}

    override fun release() {
    }

	override fun hide() {

	}

	override fun pause() {

	}

	override fun resume() {

	}

}

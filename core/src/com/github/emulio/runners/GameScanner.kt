package com.github.emulio.runners

import com.github.emulio.model.Game
import com.github.emulio.model.Platform
import com.github.emulio.xml.XMLReader
import io.reactivex.*
import mu.KotlinLogging
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class GameScanner(private val platforms: List<Platform>) : Function0<Flowable<Game>> {
	val logger = KotlinLogging.logger { }
	
	fun fullScan() : Flowable<Game> = invoke()
	
	override fun invoke(): Flowable<Game> {
		
		var games = Flowable.empty<Game>()
		
		platforms.forEach { platform ->
			logger.info { "Scanning games for platform ${platform.platformName}" }

			val xmlReader = XMLReader()

			val romsPath = platform.romsPath
			if (romsPath.isDirectory) {
				val gameList = File(romsPath, "gamelist.xml")
				val pathSet = mutableSetOf<String>()

				val listGamesFlowable = if (gameList.isFile) {
					logger.info { "reading [${gameList.absolutePath}]" }
					val gamesObservable = xmlReader.parseGameList(gameList, romsPath, pathSet, platform)
					
					logger.debug { "Game list read, scanning for new games"  }
					gamesObservable
				} else {
					Flowable.empty<Game>()
				}

				val filesObservable: Flowable<Game> = Flowable.create({ emitter ->
					scanFiles(romsPath, emitter, pathSet, platform)
					emitter.onComplete()
				}, BackpressureStrategy.BUFFER)

				games = games.concatWith(listGamesFlowable).concatWith(filesObservable)
			}
		}
		
		return games
	}
	

	private fun scanFiles(root: File, observableEmitter: FlowableEmitter<Game>, pathSet: MutableSet<String>, platform: Platform) {
		val extensions = platform.romsExtensions.toSet()

		Files.walk(root.toPath()).filter({ path ->
			!pathSet.contains(path.toAbsolutePath().toString()) && extensions.contains(path.extension)
		}).forEach { path ->
			observableEmitter.onNext(Game(path.toFile(), platform))
		}
	}

}

private val Path.extension: String
	get() {
		val name = fileName.toString()
		val idx = name.lastIndexOf(".")
		if (idx == -1) {
			return ""
		}
		return name.substring(idx)
	}

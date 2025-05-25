package io.vopenia

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    runBlocking {
        embeddedServer(
            Netty,
            port = 8181,
            host = "127.0.0.1"
        ) {
            configureRouting()
        }.start(wait = true)

        println("finished")
    }
}

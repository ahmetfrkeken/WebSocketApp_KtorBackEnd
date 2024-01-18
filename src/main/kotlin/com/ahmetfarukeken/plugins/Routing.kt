package com.ahmetfarukeken.plugins

import com.ahmetfarukeken.Connection
import com.ahmetfarukeken.Globals
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.*
import kotlin.collections.LinkedHashSet

fun Application.configureRouting() {
    routing {
        get ("/"){
            call.respondText("Hello World!", contentType = ContentType.Text.Plain)
        }

        get("/session/increment") {
            val session = call.sessions.get<Globals.MySession>() ?: Globals.MySession()
            call.sessions.set(session.copy(count = session.count + 1))
            call.respondText("Counter is ${session.count}. Refresh to increment.")
        }

        webSocket("/myws/echo"){
            send(Frame.Text("Hi from sever"))
            while (true){
                val frame = incoming.receive()
                if (frame is Frame.Text){
                    send(Frame.Text("client said: Ossur olm " + frame.readText()))
                }
            }
        }

        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
        webSocket("/chat") {
            println("Adding user!")
            val thisConnection = Connection(this)
            connections += thisConnection
            try {
                send("You are connected! There are ${connections.count()} users here.")
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    var receivedText = frame.readText()
                    val textWithUsername = "[${thisConnection.name}]: $receivedText"
                    connections.forEach {
                        it.session.send(textWithUsername)
                    }
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
            } finally {
                println("Removing $thisConnection!")
                connections.forEach {
                    it.session.send(thisConnection.name + "disconnected")
                }
                connections -= thisConnection
            }
        }

        get("/json/gson") {
            call.respond(mapOf("hello" to "world"))
        }
    }
}

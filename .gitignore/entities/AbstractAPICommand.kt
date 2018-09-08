package me.alexflipnote.kawaiibot.entities

import me.alexflipnote.kawaiibot.KawaiiBot
import me.alexflipnote.kawaiibot.extensions.closing
import me.alexflipnote.kawaiibot.extensions.thenException
import me.alexflipnote.kawaiibot.utils.RequestUtil
import me.aurieh.ichigo.core.CommandContext
import me.aurieh.ichigo.core.ICommand
import okhttp3.RequestBody

abstract class AbstractAPICommand : ICommand {
    abstract val path: String
    open val blankResponse = "You want to make a blank ${this.javaClass.simpleName.toLowerCase()}...?"

    open fun makeBody(ctx: CommandContext): RequestBody? {
        return RequestUtil.jsonBody("text" to ctx.argString)
    }

    inline fun failBody(block: () -> Unit): RequestBody? {
        block()
        return null
    }

    open fun checkBlank(ctx: CommandContext): Boolean {
        return ctx.argString.isEmpty()
    }

    final override fun run(ctx: CommandContext) {
        if (checkBlank(ctx))
            return ctx.send(blankResponse)

        val body = makeBody(ctx) ?: return

        RequestUtil.post("${KawaiiBot.config.getProperty("api_url")}$path", body).thenAccept {
            val body = it.closing()
            if (!it.isSuccessful) {
                KawaiiBot.LOG.error("bad response status code ${it.code()}, body ${body?.string()}")
                ctx.send("There was an error creating the image, try again later.")
            } else if (body == null) {
                ctx.send("I couldn't create the image ;-;")
            } else {
                ctx.channel.sendFile(body.byteStream(), "image.png").queue()
            }
        }.thenException {
            KawaiiBot.LOG.error("api call failed", it)
            ctx.send("I couldn't process the request, sorry ;-;")
        }
    }
}
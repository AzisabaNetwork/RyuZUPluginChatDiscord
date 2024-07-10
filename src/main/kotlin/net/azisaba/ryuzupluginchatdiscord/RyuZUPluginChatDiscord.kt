package net.azisaba.ryuzupluginchatdiscord

import dev.kord.core.Kord
import kotlinx.coroutines.runBlocking
import org.bukkit.plugin.java.JavaPlugin

class RyuZUPluginChatDiscord : JavaPlugin() {
    lateinit var client: Kord
    private val thread = BotThread(this)

    override fun onEnable() {
        dataFolder.mkdir()
        BotConfig.loadConfig(dataFolder)
        thread.start()
    }

    override fun onDisable() {
        try {
            runBlocking {
                client.shutdown()
            }
        } finally {
            thread.interrupt()
        }
    }
}

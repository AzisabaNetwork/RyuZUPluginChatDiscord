package net.azisaba.ryuzupluginchatdiscord

import com.github.ucchyocean.lc3.LunaChat
import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.interaction.ApplicationCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.NON_PRIVILEGED
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.runBlocking
import net.azisaba.ryuzupluginchat.RyuZUPluginChat
import net.azisaba.ryuzupluginchatdiscord.commands.ConnectCommand
import net.azisaba.ryuzupluginchatdiscord.commands.DisconnectCommand
import net.azisaba.ryuzupluginchatdiscord.listener.ChatListener
import net.azisaba.ryuzupluginchatdiscord.util.DatabaseManager
import net.azisaba.ryuzupluginchatdiscord.util.containsMember
import org.bukkit.Bukkit

class BotThread(private val plugin: RyuZUPluginChatDiscord) : Thread("RyuZUPluginChatDiscord-Bot-Thread") {
    private val commandHandlers = mutableMapOf(
        "connect" to ConnectCommand,
        "disconnect" to DisconnectCommand,
    )

    @OptIn(PrivilegedIntent::class)
    override fun run() {
        runBlocking {
            plugin.client = Kord(plugin.config.getString("token") ?: "")

            plugin.client.createGlobalApplicationCommands {
                commandHandlers.values.forEach { it.register(this) }
            }

            plugin.client.on<ReadyEvent> {
                plugin.slF4JLogger.info("Logged in as ${plugin.client.getSelf().username} (ID: ${plugin.client.getSelf().id})")
                Bukkit.getPluginManager().registerEvents(ChatListener(plugin), plugin)
                plugin.slF4JLogger.info("Registered event listener")
            }

            plugin.client.on<MessageCreateEvent> {
                if (message.author?.isBot != false) return@on
                val channelName = DatabaseManager.getChannelNameByChannelId(message.channelId.value.toLong()) ?: return@on
                val minecraftUuid = DatabaseManager.getMinecraftUUIDByDiscordId(message.author!!.id.value.toLong()) ?: return@on
                val channel = LunaChat.getAPI().getChannel(channelName) ?: return@on
                if (!channel.containsMember(minecraftUuid)) return@on
                var content = message.content
                if (message.attachments.isNotEmpty()) content += "\n"
                message.attachments.forEach { content += "${it.url}\n" }
                val ryuzuPlugin = RyuZUPluginChat.getPlugin(RyuZUPluginChat::class.java)
                val minecraftPlayer = Bukkit.getOfflinePlayer(minecraftUuid)
                val name = "${message.getAuthorAsMember().effectiveName}[${minecraftPlayer.name}]"
                val data = ryuzuPlugin.messageDataFactory.createChannelChatMessageDataFromDiscord(name, channelName, content)
                ryuzuPlugin.publisher.publishChannelChatMessage(data)
            }

            plugin.client.on<ApplicationCommandInteractionCreateEvent> {
                if (interaction.user.isBot) return@on // Bots cannot use commands
                commandHandlers[interaction.invokedCommandName]?.handle(interaction)
            }

            plugin.slF4JLogger.info("Logging in...")
            plugin.client.login {
                intents {
                    +Intents.NON_PRIVILEGED
                    +Intent.MessageContent
                }
            }
        }
    }
}

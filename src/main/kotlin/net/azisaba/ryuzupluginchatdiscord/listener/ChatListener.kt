@file:Suppress("SqlResolve", "SqlNoDataSourceInspection")

package net.azisaba.ryuzupluginchatdiscord.listener

import com.github.ucchyocean.lc3.LunaChat
import dev.kord.common.entity.Snowflake
import dev.kord.rest.builder.message.AllowedMentionsBuilder
import dev.kord.rest.request.RestRequestException
import kotlinx.coroutines.runBlocking
import net.azisaba.ryuzupluginchat.event.AsyncChannelMessageEvent
import net.azisaba.ryuzupluginchatdiscord.RyuZUPluginChatDiscord
import net.azisaba.ryuzupluginchatdiscord.util.DatabaseManager
import net.azisaba.ryuzupluginchatdiscord.util.containsMember
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class ChatListener(private val plugin: RyuZUPluginChatDiscord) : Listener {
    companion object {
        private val LEGACY_COMPONENT_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .extractUrls()
            .hexColors()
            .build()
        private val PLAIN_TEXT_COMPONENT_SERIALIZER = PlainTextComponentSerializer.plainText()
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onChannelChat(e: AsyncChannelMessageEvent) {
        val channel = LunaChat.getAPI().getChannel(e.message.lunaChatChannelName) ?: return
        val message = e.message.format()
        val plainText = PLAIN_TEXT_COMPONENT_SERIALIZER.serialize(LEGACY_COMPONENT_SERIALIZER.deserialize(message))
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            DatabaseManager.getWebhooksByChannelName(e.message.lunaChatChannelName).forEach { info ->
                runBlocking {
                    val uuid = DatabaseManager.getMinecraftUUIDByDiscordId(info.createdUserId)
                    if (uuid == null || !channel.containsMember(uuid)) {
                        plugin.logger.info("Removing webhook ${info.webhookId} because the user is not in the guild")
                        try {
                            plugin.client.rest.webhook
                                .deleteWebhook(Snowflake(info.webhookId.toULong()), "${info.createdUserId} was removed from the guild")
                        } catch (e: Exception) {
                            plugin.slF4JLogger.warn("Failed to delete webhook", e)
                        }
                        DatabaseManager.getWebhooksByChannelName.forget(channel.name)
                        return@runBlocking
                    }
                    // execute webhook
                    try {
                        plugin.client.rest.webhook.executeWebhook(Snowflake(info.webhookId.toULong()), info.webhookToken) {
                            content = plainText
                            allowedMentions = AllowedMentionsBuilder()
                        }
                    } catch (e: RestRequestException) {
                        if (e.status.code == 404 || e.status.code == 403) {
                            // invalid webhook url?
                            plugin.slF4JLogger.warn("Failed to execute webhook", e)
                            DatabaseManager.query("DELETE FROM `channels` WHERE `webhook_id` = ?") {
                                it.setLong(1, info.webhookId)
                                it.executeUpdate()
                            }
                            DatabaseManager.getWebhooksByChannelName.forget(channel.name)
                        }
                    }
                }
            }
        })
    }
}

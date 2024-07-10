package net.azisaba.ryuzupluginchatdiscord.util

import net.azisaba.ryuzupluginchatdiscord.BotConfig
import org.intellij.lang.annotations.Language
import java.sql.PreparedStatement
import java.util.*

@Suppress("SqlResolve", "SqlNoDataSourceInspection")
object DatabaseManager {
    val dataSource = BotConfig.instance.database.createDataSource()
    val guildChatDiscordDataSource = BotConfig.instance.guildChatDiscordDatabase.createDataSource()

    init {
        query("""
            CREATE TABLE IF NOT EXISTS `channels` (
                `lc_channel_name` VARCHAR(128) NOT NULL,
                `channel_id` BIGINT NOT NULL UNIQUE,
                `webhook_id` BIGINT NOT NULL,
                `webhook_token` VARCHAR(128) NOT NULL,
                `created_user_id` BIGINT NOT NULL,
                UNIQUE KEY `webhook` (`webhook_id`, `webhook_token`)
            )
        """.trimIndent()) { it.executeUpdate() }
    }

    val getWebhooksByChannelName = Functions.memoize<String, List<WebhookInfo>>(1000 * 60) { channelName ->
        query("SELECT `webhook_id`, `webhook_token`, `created_user_id` FROM `channels` WHERE `lc_channel_name` = ?") {
            it.setString(1, channelName)
            it.executeQuery().use { rs ->
                val list = mutableListOf<WebhookInfo>()
                while (rs.next()) {
                    list += WebhookInfo(
                        rs.getLong("webhook_id"),
                        rs.getString("webhook_token"),
                        rs.getLong("created_user_id"),
                    )
                }
                list
            }
        }
    }

    val getChannelNameByChannelId = Functions.memoize<Long, String?>(1000 * 60) { channelId ->
        query("SELECT `lc_channel_name` FROM `channels` WHERE `channel_id` = ?") {
            it.setLong(1, channelId)
            it.executeQuery().use { rs ->
                if (rs.next()) {
                    rs.getString("lc_channel_name")
                } else {
                    null
                }
            }
        }
    }

    val getMinecraftUUIDByDiscordId = Functions.memoize<Long, UUID?>(1000 * 30) { discordId ->
        queryGuildChatDiscord("SELECT `minecraft_uuid` FROM `users` WHERE `discord_id` = ?") {
            it.setLong(1, discordId)
            it.executeQuery().use { rs ->
                if (rs.next()) {
                    UUID.fromString(rs.getString("minecraft_uuid"))
                } else {
                    null
                }
            }
        }
    }

    @Suppress("SqlSourceToSinkFlow")
    inline fun <R> query(@Language("SQL") sql: String, block: (PreparedStatement) -> R): R =
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use(block)
        }

    @Suppress("SqlSourceToSinkFlow")
    inline fun <R> queryGuildChatDiscord(@Language("SQL") sql: String, block: (PreparedStatement) -> R): R =
        guildChatDiscordDataSource.connection.use { connection ->
            connection.prepareStatement(sql).use(block)
        }
}

@file:Suppress("SqlResolve", "SqlNoDataSourceInspection")

package net.azisaba.ryuzupluginchatdiscord.util

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.User
import dev.kord.core.entity.interaction.Interaction
import java.util.*

fun Interaction.optAny(name: String): Any? =
    this.data
        .data
        .options
        .value
        ?.find { it.name == name }
        ?.value
        ?.value
        ?.value

fun Interaction.optString(name: String) = optAny(name) as String?

fun Interaction.optSnowflake(name: String) = optAny(name) as Snowflake?

fun Interaction.optLong(name: String) = optAny(name) as Long?

fun User.getMinecraftIdName() =
    DatabaseManager.queryGuildChatDiscord("SELECT `minecraft_uuid`, `minecraft_name` FROM `users` WHERE `discord_id` = ?") { stmt ->
        stmt.setLong(1, id.value.toLong())
        stmt.executeQuery().use { rs ->
            if (rs.next()) {
                Pair(UUID.fromString(rs.getString("minecraft_uuid")), rs.getString("minecraft_name"))
            } else {
                null
            }
        }
    }

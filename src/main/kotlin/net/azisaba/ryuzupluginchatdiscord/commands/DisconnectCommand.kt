package net.azisaba.ryuzupluginchatdiscord.commands

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.interaction.ApplicationCommandInteraction
import dev.kord.rest.builder.interaction.GlobalMultiApplicationCommandBuilder
import net.azisaba.ryuzupluginchatdiscord.util.DatabaseManager

@Suppress("SqlResolve", "SqlNoDataSourceInspection")
object DisconnectCommand : CommandHandler {
    override suspend fun handle0(interaction: ApplicationCommandInteraction) {
        val triple = DatabaseManager.query("SELECT `lc_channel_name`, `webhook_id`, `webhook_token` FROM `channels` WHERE `channel_id` = ?") {
            it.setLong(1, interaction.channelId.value.toLong())
            it.executeQuery().use { rs ->
                if (rs.next()) {
                    Triple(
                        rs.getString("lc_channel_name"),
                        Snowflake(rs.getLong("webhook_id").toULong()),
                        rs.getString("webhook_token"),
                    )
                } else {
                    null
                }
            }
        }
        if (triple == null) {
            interaction.respondPublic { content = "このチャンネルは連携されていません。" }
            return
        }
        val (channelName, webhookId, webhookToken) = triple
        interaction
            .kord
            .rest
            .webhook
            .deleteWebhookWithToken(webhookId, webhookToken, "/disconnect command from ${interaction.user.tag}")
        DatabaseManager.query("DELETE FROM `channels` WHERE `channel_id` = ?") {
            it.setLong(1, interaction.channelId.value.toLong())
            it.executeUpdate()
        }
        interaction.respondPublic { content = "このチャンネルに連携されていたチャンネル`$channelName`の連携を解除しました。" }
    }

    override fun register(builder: GlobalMultiApplicationCommandBuilder) {
        builder.input("disconnect", "チャンネルチャットの連携を解除します") {
            dmPermission = false
            defaultMemberPermissions = Permissions(Permission.ManageWebhooks)
        }
    }
}

package net.azisaba.ryuzupluginchatdiscord

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlComment
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.Serializable
import org.mariadb.jdbc.Driver
import java.io.File

@Serializable
data class BotConfig(
    val token: String = "BOT_TOKEN_HERE",
    val database: DatabaseConfig = DatabaseConfig(),
    val guildChatDiscordDatabase: GuildChatDiscordDatabaseConfig = GuildChatDiscordDatabaseConfig(),
) {
    companion object {
        lateinit var instance: BotConfig

        fun loadConfig(dataFolder: File) {
            val configFile = File(dataFolder, "config.yml")
            if (!configFile.exists()) {
                configFile.writeText(Yaml.default.encodeToString(serializer(), BotConfig()) + "\n")
            }
            instance = Yaml.default.decodeFromStream(serializer(), configFile.inputStream())
            configFile.writeText(Yaml.default.encodeToString(serializer(), instance) + "\n")

            Driver() // register driver here, just in case it's not registered yet.
        }
    }
}

@Serializable
abstract class BaseDatabaseConfig(
    val driver: String = "net.azisaba.ryuzupluginchatdiscord.lib.org.mariadb.jdbc.Driver",
    @YamlComment("Change to jdbc:mysql if you want to use MySQL instead of MariaDB")
    val scheme: String = "jdbc:mariadb",
    val hostname: String = "localhost",
    val port: Int = 3306,
    val username: String = "ryuzupluginchatdiscord",
    val password: String = "",
    val properties: Map<String, String> = mapOf(
        "useSSL" to "false",
        "verifyServerCertificate" to "true",
        "prepStmtCacheSize" to "250",
        "prepStmtCacheSqlLimit" to "2048",
        "cachePrepStmts" to "true",
        "useServerPrepStmts" to "true",
        "socketTimeout" to "60000",
        "useLocalSessionState" to "true",
        "rewriteBatchedStatements" to "true",
        "maintainTimeStats" to "false",
    ),
) {
    abstract val name: String

    fun createDataSource(): HikariDataSource {
        val config = HikariConfig()
        val actualDriver = try {
            Class.forName(driver)
            driver
        } catch (e: ClassNotFoundException) {
            "org.mariadb.jdbc.Driver"
        }
        config.driverClassName = actualDriver
        config.jdbcUrl = "$scheme://$hostname:$port/$name"
        config.username = username
        config.password = password
        config.dataSourceProperties = properties.toProperties()
        return HikariDataSource(config)
    }
}

@Serializable
data class DatabaseConfig(
    override val name: String = "ryuzupluginchatdiscord",
) : BaseDatabaseConfig()

@Serializable
data class GuildChatDiscordDatabaseConfig(
    override val name: String = "guildchatdiscord",
) : BaseDatabaseConfig()

# Sbt-bukkit

Sbt support for Bukkit, Spigot and Bungeecord plugins.

See the [wiki](https://github.com/aki-ks/sbt-bukkit/wiki/) for installation, features and full usage of this plugin.

## Installation
- Clone/download this repository
- Run `sbt publish-local` in this directory. That publishes this plugin into your local ivy repository.
- Add `addSbtPlugin("me.aki.sbt-bukkit" % "sbt-bukkit" % "latest.any")` to `project/plugins.sbt`

## Basic Usage

Enable the `BukkitPlugin` for your module.
``` sbt
enablePlugins(BukkitPlugin)
```

Enter your sbt shell and execute `Bukkit/packagePlugin`.
Your plugin, its dependencies and a generated plugin.yml file will be packaged into a ready to use bukkit plugin jar.

Run `Bukkit/startServer` to start a server with your plugin.

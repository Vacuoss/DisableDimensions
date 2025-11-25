package me.avacuoss;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class DisableDimensions extends JavaPlugin implements Listener, TabExecutor {

    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadLocalConfig();

        Bukkit.getPluginManager().registerEvents(this, this);

        // Регистрируем команду и таб
        getCommand("dimensions").setExecutor(this);
        getCommand("dimensions").setTabCompleter(this);

        getLogger().info("[DisableDimensions] Plugin enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("[DisableDimensions] Plugin disabled.");
    }

    private void reloadLocalConfig() {
        reloadConfig();
        config = getConfig();
    }


    // events

    @EventHandler
    public void onPortal(PlayerPortalEvent e) {
        Player p = e.getPlayer();
        Environment env = e.getFrom().getWorld().getEnvironment();

        // Nether
        if (config.getBoolean("disable-nether") &&
                e.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            e.setCancelled(true);
            p.sendMessage(config.getString("messages.nether"));
            return;
        }

        // End
        if (config.getBoolean("disable-end") &&
                e.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            e.setCancelled(true);
            p.sendMessage(config.getString("messages.end"));
        }
    }


    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getTo() == null) return;

        Player p = e.getPlayer();
        World to = e.getTo().getWorld();
        if (to == null) return;

        String key = to.getKey().toString();
        List<String> disabled = config.getStringList("disabled-dimensions");

        // Модовые измерения
        if (disabled.contains(key)) {
            e.setCancelled(true);
            p.sendMessage(config.getString("messages.generic"));
            return;
        }

        // Ванильные
        if (to.getEnvironment() == Environment.NETHER &&
                config.getBoolean("disable-nether")) {

            e.setCancelled(true);
            p.sendMessage(config.getString("messages.nether"));
            return;
        }

        if (to.getEnvironment() == Environment.THE_END &&
                config.getBoolean("disable-end")) {

            e.setCancelled(true);
            p.sendMessage(config.getString("messages.end"));
        }
    }


    // commands

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {

        if (!s.hasPermission("dimensions.admin")) {
            s.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length == 0) {
            s.sendMessage("§e/dimensions list");
            s.sendMessage("§e/dimensions toggle <id>");
            s.sendMessage("§e/dimensions reload");
            return true;
        }

        // /dimensions reload
        if (args[0].equalsIgnoreCase("reload")) {
            reloadLocalConfig();
            s.sendMessage("§aConfig reloaded.");
            return true;
        }

        // /dimensions list
        if (args[0].equalsIgnoreCase("list")) {
            s.sendMessage("§6--- Loaded Dimensions ---");

            for (World w : Bukkit.getWorlds()) {
                String id = w.getKey().toString();
                boolean disabled = config.getStringList("disabled-dimensions").contains(id);

                s.sendMessage("§e" + id + " §7= " + (disabled ? "§cDISABLED" : "§aENABLED"));
            }
            return true;
        }

        // /dimensions toggle <id>
        if (args[0].equalsIgnoreCase("toggle")) {
            if (args.length < 2) {
                s.sendMessage("§cUsage: /dimensions toggle <dimension_id>");
                return true;
            }

            String id = args[1];
            List<String> disabled = config.getStringList("disabled-dimensions");

            if (disabled.contains(id)) {
                disabled.remove(id);
                s.sendMessage("§aEnabled: §e" + id);
            } else {
                disabled.add(id);
                s.sendMessage("§cDisabled: §e" + id);
            }

            config.set("disabled-dimensions", disabled);
            saveConfig();
            return true;
        }

        return true;
    }


    // TAB

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {

        if (!sender.hasPermission("dimensions.admin")) return List.of();

        // /dimensions <TAB>
        if (args.length == 1) {
            return List.of("list", "toggle", "reload")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // /dimensions toggle <TAB>
        if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {

            return Bukkit.getWorlds().stream()
                    .map(w -> w.getKey().toString())
                    .filter(id -> id.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}

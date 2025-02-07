package org.popcraft.chunky;

import io.papermc.lib.PaperLib;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitWorker;
import org.popcraft.chunky.command.ChunkyCommand;
import org.popcraft.chunky.integration.WorldBorderIntegration;
import org.popcraft.chunky.platform.BukkitConfig;
import org.popcraft.chunky.platform.BukkitPlatform;
import org.popcraft.chunky.platform.BukkitSender;
import org.popcraft.chunky.platform.Platform;
import org.popcraft.chunky.platform.Sender;
import org.popcraft.chunky.util.Limit;
import org.popcraft.chunky.util.Metrics;
import org.popcraft.chunky.util.Version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.popcraft.chunky.util.Translator.translate;

public final class ChunkyBukkit extends JavaPlugin {
    private Chunky chunky;

    @Override
    public void onEnable() {
        this.chunky = new Chunky(new BukkitPlatform(this));
        chunky.setConfig(new BukkitConfig(chunky, this));
        chunky.setLanguage(chunky.getConfig().getLanguage());
        chunky.loadCommands();
        Limit.set(chunky.getConfig());
        Version currentVersion = Version.getCurrentMinecraftVersion();
        if (Version.v1_13_2.isEqualTo(currentVersion) && !PaperLib.isPaper()) {
            this.getLogger().severe(translate("error_version_spigot"));
            this.getServer().getPluginManager().disablePlugin(this);
        } else if (Version.v1_13_2.isHigherThan(currentVersion)) {
            this.getLogger().severe(translate("error_version"));
            this.getServer().getPluginManager().disablePlugin(this);
        }
        Platform platform = chunky.getPlatform();
        if (chunky.getConfig().getContinueOnRestart()) {
            getServer().getScheduler().scheduleSyncDelayedTask(this, () -> chunky.getCommands().get("continue").execute(platform.getServer().getConsoleSender(), new String[]{}));
        }
        if (getServer().getPluginManager().getPlugin("WorldBorder") != null) {
            platform.getServer().getIntegrations().put("border", new WorldBorderIntegration());
        }
        Metrics metrics = new Metrics(this, 8211);
        if (metrics.isEnabled()) {
            metrics.addCustomChart(new Metrics.SimplePie("language", () -> chunky.getConfig().getLanguage()));
        }
    }

    @Override
    public void onDisable() {
        chunky.getConfig().saveTasks();
        chunky.getGenerationTasks().values().forEach(generationTask -> generationTask.stop(false));
        this.getServer().getScheduler().getActiveWorkers().stream()
                .filter(w -> w.getOwner() == this)
                .map(BukkitWorker::getThread)
                .forEach(Thread::interrupt);
        this.getServer().getScheduler().cancelTasks(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Sender bukkitSender = new BukkitSender(sender);
        Map<String, ChunkyCommand> commands = chunky.getCommands();
        if (args.length > 0 && commands.containsKey(args[0].toLowerCase())) {
            if (sender.hasPermission("chunky.command." + args[0].toLowerCase())) {
                commands.get(args[0].toLowerCase()).execute(bukkitSender, args);
            } else {
                bukkitSender.sendMessage("command_no_permission");
            }
        } else {
            commands.get("help").execute(bukkitSender, new String[]{});
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length < 1) {
            return Collections.emptyList();
        }
        final List<String> suggestions = new ArrayList<>();
        Map<String, ChunkyCommand> commands = chunky.getCommands();
        if (args.length == 1) {
            commands.keySet().stream().filter(name -> sender.hasPermission("chunky.command." + name)).forEach(suggestions::add);
        } else if (commands.containsKey(args[0].toLowerCase()) && sender.hasPermission("chunky.command." + args[0].toLowerCase())) {
            suggestions.addAll(commands.get(args[0].toLowerCase()).tabSuggestions(new BukkitSender(sender), args));
        }
        return suggestions.stream()
                .filter(s -> s.toLowerCase().contains(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }

    public Chunky getChunky() {
        return chunky;
    }
}

package org.atcraftmc.mvdl;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class MVDataPackLoader extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("mvdl-load-datapacks")).setExecutor(new SyncDatapackCommand(this));
    }

    @Override
    public void onDisable() {
        ServerCommandEvent.getHandlerList().unregister((Plugin) this);
    }

    @EventHandler
    public void onCommandBlockExecute(ServerCommandEvent event) {
        if (!(event.getSender() instanceof BlockCommandSender block)) {
            return;
        }

        var world = block.getBlock().getWorld();

        if (!isMVWorld(world)) {
            return;
        }


        var command = event.getCommand();

        if (command.contains("function")) {
            var data = command.replaceFirst("function ", "function %s_".formatted(world.getName().replace("-", "_")));

            System.out.println("[RMP] " + data);
            event.setCancelled(true);

            Bukkit.dispatchCommand(block, data);
        }
    }


    public boolean isMVWorld(World world) {
        var name = world.getName();
        if (name.startsWith("mp-")) {
            return true;
        }

        var worlds = getConfig().getStringList("worlds");

        return worlds.contains(name);
    }

    public Set<World> getWorlds() {
        var worlds = new HashSet<World>();

        for (String name : getConfig().getStringList("worlds")) {
            var world = Bukkit.getWorld(name);
            if (world == null) {
                continue;
            }

            worlds.add(world);
        }

        return worlds;
    }
}

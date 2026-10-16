package dev.nametags;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class NametagsPlugin extends JavaPlugin implements Listener {
    private StatsStore stats;
    private NametagManager manager;
    private Economy economy;

    @Override
    public void onEnable() {
        stats = new StatsStore(this);
        manager = new NametagManager(this);

        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) economy = rsp.getProvider();
        }
        if (economy == null) getLogger().warning("No Vault economy found, money will show as 0.");

        getCommand("nametag").setExecutor(new NametagDialog(this));
        getServer().getPluginManager().registerEvents(this, this);

        manager.start();
    }

    @Override
    public void onDisable() {
        manager.shutdown();
        stats.saveAll();
    }

    public StatsStore stats() { return stats; }
    public NametagManager manager() { return manager; }
    public Economy economy() { return economy; }

    private void later(Player p) {
        Bukkit.getScheduler().runTaskLater(this, () -> { if (p.isOnline()) manager.refresh(p); }, 2L);
    }

    @EventHandler public void onJoin(PlayerJoinEvent e) { later(e.getPlayer()); }
    @EventHandler public void onRespawn(PlayerRespawnEvent e) { later(e.getPlayer()); }
    @EventHandler public void onTeleport(PlayerTeleportEvent e) { later(e.getPlayer()); }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        manager.remove(e.getPlayer());
        stats.put(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        stats.get(victim.getUniqueId()).deaths++;
        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim)) stats.get(killer.getUniqueId()).kills++;
        manager.remove(victim);
        if (killer != null) manager.refresh(killer);
    }
}

package dev.nametags;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Every viewer picks what they see above OTHER players' heads. To do that without one entity per
 * viewer, each target gets one TextDisplay per distinct "row mask" and each viewer only sees the
 * display that matches their own settings.
 */
public class NametagManager {
    private static final TextColor WHITE      = TextColor.color(0xFFFFFF);
    private static final TextColor DARK_GREEN = TextColor.color(0x007A00); // $
    private static final TextColor DARK_RED   = TextColor.color(0x8B0000); // heart
    private static final TextColor BRIGHT_RED = TextColor.color(0xFF1A1A); // sword
    private static final TextColor BRIGHT_ORG = TextColor.color(0xFF8C00); // skull

    private static final int MONEY = 1, HEARTS = 2, KILLS = 4, DEATHS = 8;

    private final NametagsPlugin plugin;
    private final Map<UUID, Map<Integer, TextDisplay>> displays = new HashMap<>();
    private Team hiddenTeam;

    public NametagManager(NametagsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        var board = Bukkit.getScoreboardManager().getMainScoreboard();
        hiddenTeam = board.getTeam("nt_hidden");
        if (hiddenTeam == null) hiddenTeam = board.registerNewTeam("nt_hidden");
        hiddenTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, 20L, 10L);
    }

    public void shutdown() {
        for (Player p : Bukkit.getOnlinePlayers()) remove(p);
        displays.clear();
        if (hiddenTeam != null) hiddenTeam.unregister();
    }

    /** Re-renders every tag for every viewer right now. Called on every toggle. */
    public void refreshAll() {
        for (Player target : Bukkit.getOnlinePlayers()) refresh(target);
    }

    public void refresh(Player target) {
        if (!hiddenTeam.hasEntry(target.getName())) hiddenTeam.addEntry(target.getName());

        Map<Integer, TextDisplay> map = displays.computeIfAbsent(target.getUniqueId(), k -> new HashMap<>());

        if (target.isDead() || target.getGameMode() == GameMode.SPECTATOR) {
            clearDisplays(map);
            return;
        }

        // Group the viewers by the rows they want to see
        Map<Integer, List<Player>> viewersByMask = new HashMap<>();
        for (Player v : Bukkit.getOnlinePlayers()) {
            if (v.equals(target) || !v.getWorld().equals(target.getWorld())) continue;
            PlayerData vd = plugin.stats().get(v.getUniqueId());
            if (!vd.enabled) continue;
            viewersByMask.computeIfAbsent(mask(vd), k -> new ArrayList<>()).add(v);
        }

        // Drop displays nobody needs anymore
        map.entrySet().removeIf(e -> {
            TextDisplay td = e.getValue();
            if (!td.isValid() || !viewersByMask.containsKey(e.getKey())) {
                if (td.isValid()) td.remove();
                return true;
            }
            return false;
        });

        PlayerData targetData = plugin.stats().get(target.getUniqueId());

        for (Map.Entry<Integer, List<Player>> e : viewersByMask.entrySet()) {
            TextDisplay td = map.get(e.getKey());
            if (td == null) {
                td = spawn(target);
                map.put(e.getKey(), td);
            } else if (!target.equals(td.getVehicle())) {
                td.teleport(target.getLocation());
                target.addPassenger(td);
            }
            td.text(build(target, targetData, e.getKey()));
            for (Player v : e.getValue()) {
                if (!v.canSee(td)) v.showEntity(plugin, td);
            }
        }

        // A viewer who changed their settings must stop seeing their old display
        for (Map.Entry<Integer, TextDisplay> e : map.entrySet()) {
            List<Player> wanted = viewersByMask.getOrDefault(e.getKey(), List.of());
            for (Player v : Bukkit.getOnlinePlayers()) {
                if (!wanted.contains(v) && v.canSee(e.getValue())) v.hideEntity(plugin, e.getValue());
            }
        }
    }

    public void remove(Player p) {
        Map<Integer, TextDisplay> map = displays.remove(p.getUniqueId());
        if (map != null) clearDisplays(map);
        if (hiddenTeam != null) hiddenTeam.removeEntry(p.getName());
    }

    private void clearDisplays(Map<Integer, TextDisplay> map) {
        map.values().forEach(td -> { if (td.isValid()) td.remove(); });
        map.clear();
    }

    private int mask(PlayerData d) {
        return (d.showMoney ? MONEY : 0) | (d.showHearts ? HEARTS : 0)
                | (d.showKills ? KILLS : 0) | (d.showDeaths ? DEATHS : 0);
    }

    private TextDisplay spawn(Player target) {
        TextDisplay td = target.getWorld().spawn(target.getLocation(), TextDisplay.class, d -> {
            d.setVisibleByDefault(false); // only viewers we explicitly show it to can see it
            d.setBillboard(Display.Billboard.CENTER);
            d.setDefaultBackground(false);
            d.setShadowed(true);
            d.setPersistent(false);
            d.setAlignment(TextDisplay.TextAlignment.CENTER);
            d.setTransformation(new Transformation(
                    new Vector3f(0f, 0.45f, 0f), new AxisAngle4f(),
                    new Vector3f(1f, 1f, 1f), new AxisAngle4f()));
        });
        target.addPassenger(td);
        return td;
    }

    private Component build(Player target, PlayerData d, int mask) {
        List<Component> lines = new ArrayList<>();
        lines.add(plain(SmallCaps.convert(target.getName()), WHITE));

        if ((mask & MONEY) != 0) {
            Economy eco = plugin.economy();
            double bal = eco != null ? eco.getBalance(target) : 0;
            lines.add(row("$", DARK_GREEN, NumberFormatUtil.format(bal)));
        }
        if ((mask & HEARTS) != 0) {
            double hearts = Math.round(target.getHealth()) / 2.0; // half-heart steps
            lines.add(row("❤", DARK_RED, NumberFormatUtil.format(hearts)));
        }
        if ((mask & KILLS) != 0)  lines.add(row("⚔", BRIGHT_RED, String.valueOf(d.kills)));
        if ((mask & DEATHS) != 0) lines.add(row("☠", BRIGHT_ORG, String.valueOf(d.deaths)));

        return Component.join(JoinConfiguration.newlines(), lines);
    }

    private Component row(String icon, TextColor iconColor, String value) {
        return plain(icon, iconColor).append(plain(" " + value, WHITE));
    }

    private Component plain(String text, TextColor color) {
        return Component.text(text, color)
                .decoration(TextDecoration.BOLD, false)
                .decoration(TextDecoration.ITALIC, false);
    }
}

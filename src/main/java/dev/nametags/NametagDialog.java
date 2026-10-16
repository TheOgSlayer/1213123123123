package dev.nametags;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

/** /nametag opens a native dialog screen. Each button toggles instantly and reopens the dialog. */
@SuppressWarnings("UnstableApiUsage")
public class NametagDialog implements CommandExecutor {
    private final NametagsPlugin plugin;

    public NametagDialog(NametagsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player p) open(p);
        else sender.sendMessage("Players only.");
        return true;
    }

    public void open(Player p) {
        p.showDialog(build(p));
    }

    private Dialog build(Player p) {
        PlayerData d = plugin.stats().get(p.getUniqueId());

        List<ActionButton> buttons = List.of(
                toggle(p, "Nametags", d.enabled,    x -> x.enabled = !x.enabled),
                toggle(p, "Money",    d.showMoney,  x -> x.showMoney = !x.showMoney),
                toggle(p, "Hearts",   d.showHearts, x -> x.showHearts = !x.showHearts),
                toggle(p, "Kills",    d.showKills,  x -> x.showKills = !x.showKills),
                toggle(p, "Deaths",   d.showDeaths, x -> x.showDeaths = !x.showDeaths)
        );

        ActionButton close = ActionButton.create(plain("Close", NamedTextColor.WHITE), null, 150, null);

        DialogBase base = DialogBase.builder(plain("Nametag Settings", NamedTextColor.WHITE))
                .canCloseWithEscape(true)
                .body(List.of(DialogBody.plainMessage(
                        plain("Choose what you see above other players' heads.", NamedTextColor.GRAY))))
                .build();

        return Dialog.create(b -> b.empty()
                .base(base)
                .type(DialogType.multiAction(buttons, close, 1)));
    }

    private ActionButton toggle(Player p, String name, boolean on, Consumer<PlayerData> flip) {
        Component label = plain(name + ": ", NamedTextColor.WHITE)
                .append(on ? plain("ON", NamedTextColor.GREEN) : plain("OFF", NamedTextColor.RED));

        return ActionButton.create(label, plain("Click to toggle", NamedTextColor.GRAY), 150,
                DialogAction.customClick((view, audience) -> {
                    flip.accept(plugin.stats().get(p.getUniqueId()));
                    plugin.manager().refreshAll(); // instantly updates the heads you see
                    open(p);                       // redraw the dialog with the new state
                }, ClickCallback.Options.builder().build()));
    }

    private Component plain(String text, NamedTextColor color) {
        return Component.text(text, color)
                .decoration(TextDecoration.BOLD, false)
                .decoration(TextDecoration.ITALIC, false);
    }
}

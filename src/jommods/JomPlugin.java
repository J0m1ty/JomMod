package jommods;

import arc.*;
import arc.util.*;
import mindustry.mod.*;
import mindustry.mod.Mods;
import mindustry.Vars;
import mindustry.gen.*;
import mindustry.game.*;
import mindustry.net.*;
import mindustry.game.EventType.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.entities.*;
import mindustry.net.Administration.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

import java.util.HashSet;

public class JomPlugin extends Plugin {

    private static final double mapratio = 0.6;
    private final HashSet<String> mapvotes = new HashSet<>();

    @Override
    public void init() {
        Events.on(PlayerLeave.class, e -> {
            Player player = e.player;
            int mapcur = this.mapvotes.size();
            int mapreq = (int) Math.ceil(mapratio * Groups.player.size());
            if(mapvotes.contains(player.uuid())) {
                mapvotes.remove(player.uuid());
                Call.sendMessage("[cyan]MAP SKIPPER[]: [accent]" + player.name + "[accent] has disconnected, [green]" + mapcur + "[] votes, [green]" + mapreq + "[] required.");
            }
        });

        Events.on(GameOverEvent.class, e -> {
            this.mapvotes.clear();
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.<Player>register("skip", "", "Vote to Skip Map", (args, player) -> {
            mapvotes.add(player.uuid());
            int mapcur = this.mapvotes.size();
            int mapreq = (int) Math.ceil(mapratio * Groups.player.size());
            Call.sendMessage("[cyan]MAP SKIPPER[]: [accent]" + player.name + "[accent] wants to skip the map, [green]" + mapcur + "[] votes, [green]" + mapreq + "[] required.");
            if (mapcur < mapreq) {
                return;
            }

            this.mapvotes.clear();
            Call.sendMessage("[cyan]MAP SKIPPER[]: [green] vote passed, skipping the map...");
            Events.fire(new GameOverEvent(Team.derelict));
        });

        handler.<Player>register("js", "<code...>", "Execute JavaScript code.", (args, player) -> {
            if (player.admin) {
                String output = mods.getScripts().runConsole(args[0]);
                player.sendMessage("> " + (isError(output) ? "[#ff341c]" + output : output));
            } else {
                player.sendMessage("[scarlet]You must be an admin to use this command.");
            }
        });

        handler.<Player>register("whisper", "<player> <text...>", "Whisper text to another player.", (args, player) -> {
            //find player by name
            Player other = Groups.player.find(p -> p.name.equalsIgnoreCase(args[0]));

            //give error message with scarlet-colored text if player isn't found
            if(other == null){
                player.sendMessage("[scarlet]No player by that name found!");
                return;
            }

            //send the other player a message, using [lightgray] for gray text color and [] to reset color
            other.sendMessage("[lightgray](whisper) " + player.name + ":[] " + args[1]);
        });
    }

    private boolean isError(String output) {
        try {
            String errorName = output.substring(0, output.indexOf(' ') - 1);
            Class.forName("rhino." + errorName);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }
}

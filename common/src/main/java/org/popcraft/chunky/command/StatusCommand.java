package org.popcraft.chunky.command;

import org.popcraft.chunky.Chunky;
import org.popcraft.chunky.GenerationTask;
import org.popcraft.chunky.Selection;
import org.popcraft.chunky.platform.Sender;
import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.util.Formatting;
import org.popcraft.chunky.util.Input;
import org.popcraft.chunky.util.Limit;

import java.util.*;

import static org.popcraft.chunky.util.Translator.translate;

public class StatusCommand extends ChunkyCommand {
    public StatusCommand(Chunky chunky) {
        super(chunky);
    }

    public void execute(Sender sender, String[] args) {
        Set<World> worlds = chunky.getGenerationTasks().keySet();
        if (args.length == 2) {
            Optional<World> world = Input.tryWorld(chunky, args[1]);
            if (world.isPresent()) {
                worlds = new HashSet<>();
                worlds.add(world.get());
            } else {
                sender.sendMessage("help_status");
                return;
            }
        }

        Map<World, GenerationTask> tasks = chunky.getGenerationTasks();

        for (World world : worlds) {
            GenerationTask generationTask = tasks.get(world);
            if (generationTask == null) {
                sender.sendMessagePrefixed("format_status", world.getName(), 0, String.format("%.2f", 0f), String.format("%01d", 0f), String.format("%02d", 0f), String.format("%02d", 0), String.format("%.1f", 0));
                continue;
            }

            if (generationTask.isCancelled()) {
                return;
            }


            long chunkNum = generationTask.getCount() + 1;
            long totalChunks = generationTask.getChunkIterator().total();
            double percentDone = 100f * chunkNum / totalChunks;
            long chunksLeft = totalChunks - chunkNum;

            double speed = generationTask.getSpeed();
            long etaHours = 0;
            long etaMinutes = 0;
            long etaSeconds = 0;
            if (speed > 0) {
                long eta = (long) (chunksLeft / generationTask.getSpeed());
                etaHours = eta / 3600;
                etaMinutes = (eta - etaHours * 3600) / 60;
                etaSeconds = eta - etaHours * 3600 - etaMinutes * 60;
            }


            sender.sendMessagePrefixed("format_status", world.getName(), chunkNum, String.format("%.2f", percentDone), String.format("%01d", etaHours), String.format("%02d", etaMinutes), String.format("%02d", etaSeconds), String.format("%.1f", speed));

        }
    }


    @Override
    public List<String> tabSuggestions(Sender sender, String[] args) {
        if (args.length == 2) {
            List<String> suggestions = new ArrayList<>();
            chunky.getPlatform().getServer().getWorlds().forEach(world -> suggestions.add(world.getName()));
            return suggestions;
        }

        return Collections.emptyList();
    }
}

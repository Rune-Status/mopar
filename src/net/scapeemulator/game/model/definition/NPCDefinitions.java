package net.scapeemulator.game.model.definition;

import java.io.IOException;

import net.scapeemulator.cache.Archive;
import net.scapeemulator.cache.Cache;
import net.scapeemulator.cache.Container;
import net.scapeemulator.cache.ReferenceTable;
import net.scapeemulator.cache.def.NPCDefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Hadyn Richard
 */
public final class NPCDefinitions {

    private static final Logger logger = LoggerFactory.getLogger(NPCDefinitions.class);
    private static NPCDefinition[] definitions;

    public static void init(Cache cache) throws IOException {
        int count = 0;

        Container tableContainer = Container.decode(cache.getStore().read(255, 18));
        ReferenceTable table = ReferenceTable.decode(tableContainer.getData());

        int files = table.capacity();
        definitions = new NPCDefinition[files * 128];

        for (int file = 0; file < files; file++) {
            ReferenceTable.Entry entry = table.getEntry(file);
            if (entry == null) {
                continue;
            }

            Archive archive = Archive.decode(cache.read(18, file).getData(), entry.size());
            int nonSparseMember = 0;
            for (int member = 0; member < entry.capacity(); member++) {
                ReferenceTable.ChildEntry childEntry = entry.getEntry(member);
                if (childEntry == null) {
                    continue;
                }

                int id = file * 128 + member;
                NPCDefinition definition = NPCDefinition.decode(id, archive.getEntry(nonSparseMember++));
                definitions[id] = definition;
                count++;
            }
        }

        logger.info("Loaded " + count + " npc definitions.");
    }

    public static int count() {
        if (definitions != null) {
            return definitions.length;
        }
        throw new IllegalStateException("NPCDefinitions not initialized!");
    }

    public static NPCDefinition[] getDefinitions() {
        if (definitions != null) {
            return definitions;
        }
        throw new IllegalStateException("NPCDefinitions not initialized!");
    }

    public static NPCDefinition forId(int id) {
        try {
            return definitions[id];
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            return null;
        }
    }
}

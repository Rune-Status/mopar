package net.scapeemulator.cache.def;

import java.nio.ByteBuffer;

import net.scapeemulator.cache.util.ByteBufferUtils;

/**
 * @author Hadyn Richard
 * @author David Insley
 */
public final class ObjectDefinition {

    private String name;

    private String[] options;

    private int width;
    private int length;
    private int animationId;
    private int validInteractSides;

    private boolean impenetrable;
    private boolean solid;

    private int[] childIds;

    @SuppressWarnings("unused")
    public static ObjectDefinition decode(ByteBuffer buffer) {
        ObjectDefinition def = new ObjectDefinition();
        def.name = "null";
        def.width = 1;
        def.length = 1;
        def.options = new String[5];
        def.impenetrable = true;
        def.solid = true;
        while (true) {
            int opcode = buffer.get() & 0xFF;
            if (opcode == 0)
                break;
            if (opcode == 1) {
                int i = buffer.get() & 0xff;
                if (i > 0) {
                    for (int var5 = 0; var5 < i; var5++) {
                        buffer.getShort();
                        buffer.get();
                    }
                }
            } else if (opcode == 2) {
                def.name = ByteBufferUtils.getJagexString(buffer);
            } else if (opcode == 5) {
                int i = buffer.get() & 0xff;
                if (i > 0) {
                    for (int var5 = 0; var5 < i; var5++) {
                        buffer.getShort();
                    }
                }
            } else if (opcode == 14) {
                def.width = buffer.get() & 0xff;
            } else if (opcode == 15) {
                def.length = buffer.get() & 0xff;
            } else if (opcode == 17) {
                def.solid = false;
            } else if (opcode == 18) {
                def.impenetrable = false;
            } else if (opcode == 19) {
                int i = buffer.get();
            } else if (opcode == 24) {
                def.animationId = buffer.getShort();
                if (def.animationId == 65535) {
                    def.animationId = -1;
                }
            } else if (opcode == 27) {
                def.impenetrable = false; // TODO cliptype = 1
            } else if (opcode == 28) {
                int i = buffer.get();
            } else if (opcode == 29) {
                int i = buffer.get();
            } else if (opcode >= 30 && opcode < 35)
                def.options[opcode - 30] = ByteBufferUtils.getJagexString(buffer);
            else if (opcode == 39) {
                int i = buffer.get();
            } else if (opcode == 40) {
                int length = buffer.get() & 0xFF;
                // Original / modified colors
                for (int index = 0; index < length; index++) {
                    buffer.getShort();
                    buffer.getShort();
                }
            } else if (opcode == 41) {
                int length = buffer.get() & 0xFF;
                for (int index = 0; index < length; index++) {
                    int i = buffer.getShort() & 0xFFFFF;
                    int i2 = buffer.getShort() & 0xFFFFF;
                }
            } else if (opcode == 42) {
                int length = buffer.get() & 0xFF;
                for (int index = 0; index < length; index++) {
                    int i = buffer.get();
                }
            } else if (opcode == 60) {
                int i = buffer.getShort() & 0xffff;
            } else if (opcode == 65) {
                int i = buffer.getShort() & 0xffff;
            } else if (opcode == 66) {
                int i = buffer.getShort() & 0xffff;
            } else if (opcode == 67) {
                int i = buffer.getShort() & 0xffff;
            } else if (opcode == 69) {
                def.validInteractSides = buffer.get() & 0xff;
            } else if (opcode == 70) {
                int i = buffer.getShort();
            } else if (opcode == 71) {
                int i = buffer.getShort();
            } else if (opcode == 72) {
                int i = buffer.getShort();
            } else if (opcode == 74) {
                def.impenetrable = false;
                def.solid = false;
            } else if (opcode == 75) {
                int i = buffer.get();
            } else if (opcode == 77 || opcode == 92) {
                int i4 = -1;

                int i = buffer.getShort() & 0xffff;
                if (i == '\uFFFF') {
                    i = -1;
                }

                int i2 = buffer.getShort() & 0xffff;
                if (i2 == '\uFFFF') {
                    i2 = -1;
                }

                if (opcode == 92) {
                    i4 = buffer.getShort();
                    if (i4 == '\uFFFF') {
                        i4 = -1;
                    }
                }

                int i5 = buffer.get() & 0xff;
                def.childIds = new int[i5 + 2];
                // Child ids ?
                for (int var6 = 0; var6 <= i5; var6++) {
                    def.childIds[var6] = buffer.getShort() & 0xFFFF;
                    if (def.childIds[var6] == '\uFFFF') {
                        def.childIds[var6] = -1;
                    }
                }
                def.childIds[1 + i5] = i4;
            } else if (opcode == 78) {
                buffer.getShort();
                buffer.get();
            } else if (opcode == 79) {
                int i = buffer.getShort() & 0xffff;
                int i2 = buffer.getShort() & 0xffff;
                int i3 = buffer.get() & 0xff;
                int i4 = buffer.get() & 0xff;
                for (int counter = 0; counter < i4; ++counter) {
                    int i5 = buffer.getShort() & 0xffff;
                }
            } else if (opcode == 81) {
                int i = buffer.get() & 0xff;
            } else if (opcode == 93) {
                int i = buffer.getShort() & 0xFFFFF;
            } else if (opcode == 99) {
                int i = buffer.get() & 0xff;
                int i2 = buffer.getShort() & 0xffff;
            } else if (opcode == 100) {
                int i = buffer.get() & 0xff;
                int i2 = buffer.getShort() & 0xffff;
            } else if (opcode == 101) {
                int i = buffer.get() & 0xff;
            } else if (opcode == 102) {
                int i = buffer.getShort() & 0xFFFFF;
            } else if (opcode == 249) {
                int length = buffer.get() & 0xFF;
                for (int index = 0; index < length; index++) {
                    boolean stringInstance = buffer.get() == 1;
                    int key = ByteBufferUtils.getTriByte(buffer);
                    Object value = stringInstance ? ByteBufferUtils.getJagexString(buffer) : buffer.getInt();
                }
            }
        }
        return def;
    }

    public String getName() {
        return name;
    }

    public boolean hasOptions() {
        for (int i = 0; i < options.length; i++) {
            if (options[i] != null) {
                return true;
            }
        }
        return false;
    }

    public String[] getOptions() {
        return options;
    }

    public boolean isImpenetrable() {
        return impenetrable;
    }

    public boolean isSolid() {
        return solid;
    }

    public int[] getChildIds() {
        return childIds;
    }

    public int getAnimationId() {
        return animationId;
    }

    public int getValidInteractSides() {
        return validInteractSides;
    }

    public int getValidInteractSides(int rotation) {
        return ((validInteractSides << rotation) & 0xF) + (validInteractSides >> (4 - rotation));
    }

    public int getWidth() {
        return width;
    }

    public int getLength() {
        return length;
    }
}
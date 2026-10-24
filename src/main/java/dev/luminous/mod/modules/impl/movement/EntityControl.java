package dev.luminous.mod.modules.impl.movement;

import dev.luminous.mod.modules.Module;

public class EntityControl extends Module {
    public static EntityControl INSTANCE;

    public EntityControl() {
        super("EntityControl", Category.Movement);
        setChinese("骑行控制");
        INSTANCE = this;
    }
}

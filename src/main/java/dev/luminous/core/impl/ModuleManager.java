package dev.luminous.core.impl;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.Render2DEvent;
import dev.luminous.api.events.impl.Render3DEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.path.BaritoneUtil;
import dev.luminous.api.utils.render.Render3DScheduler;
import dev.luminous.mod.Mod;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.*;
import dev.luminous.mod.modules.impl.combat.*;
import dev.luminous.mod.modules.impl.exploit.*;
import dev.luminous.mod.modules.impl.misc.*;
import dev.luminous.mod.modules.impl.movement.*;
import dev.luminous.mod.modules.impl.player.*;
import dev.luminous.mod.modules.impl.render.*;
import dev.luminous.mod.modules.settings.Setting;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.Comparator;

public class ModuleManager implements Wrapper {
    public ArrayList<Module> getModules() {
        return modules;
    }

    private final ArrayList<Module> modules = new ArrayList<>();

    public ModuleManager() {
        init();
    }

    public void init() {
        if (BaritoneUtil.loaded) {
            addModule(new BaritoneModule());
        }
        addModule(new AutoKit());
        addModule(new Bot());
        addModule(new Fonts());
        addModule(new NoTerrainScreen());
        addModule(new AutoCrystal());
        addModule(new Ambience());
        addModule(new AntiHunger());
        addModule(new AntiVoid());
        addModule(new AutoWalk());
        addModule(new VClip());
        addModule(new ExtraTab());
        addModule(new AntiWeak());
        addModule(new BedCrafter());
        addModule(new Friend());
        addModule(new AspectRatio());
        addModule(new ChunkESP());
        addModule(new Aura());
        addModule(new PistonCrystal());
        addModule(new AutoAnchor());
        addModule(new PhaseESP());
        addModule(new AutoArmor());
        addModule(new Breaker());
        addModule(new AutoLog());
        addModule(new AutoEZ());
        addModule(new SelfTrap());
        addModule(new Sorter());
        addModule(new AutoMend());
        addModule(new AutoPot());
        addModule(new AutoPush());
        addModule(new Offhand());
        addModule(new Nuker());
        addModule(new AutoTrap());
        addModule(new AutoWeb());
        addModule(new Blink());
        addModule(new ChorusControl());
        addModule(new BlockStrafe());
        addModule(new FastSwim());
        addModule(new Blocker());
        addModule(new Quiver());
        addModule(new BowBomb());
        addModule(new BreakESP());
        addModule(new Burrow());
        addModule(new Punctuation());
        addModule(new MaceSpoof());
        addModule(new CameraClip());
        addModule(new ChatAppend());
        addModule(new ClickGui());
        addModule(new InfiniteTrident());
        addModule(new ColorsModule());
        addModule(new AutoRegear());
        addModule(new LavaFiller());
        addModule(new AntiPhase());
        addModule(new Clip());
        addModule(new AntiCheat());
        addModule(new IRC());
        addModule(new ItemsCounter());
        addModule(new Fov());
        addModule(new Criticals());
        addModule(new CevBreaker());
        addModule(new Crosshair());
        addModule(new Chams());
        addModule(new AntiPacket());
        addModule(new AutoReconnect());
        addModule(new ESP());
        addModule(new HoleESP());
        addModule(new Tracers());
        addModule(new MovementSync());
        addModule(new ElytraFly());
        addModule(new PacketLogger());
        addModule(new TeleportLogger());
        addModule(new SkinFlicker());
        addModule(new EntityControl());
        addModule(new NameTags());
        addModule(new ShulkerViewer());
        addModule(new PingSpoof());
        addModule(new FakePlayer());
        addModule(new Spammer());
        addModule(new MotionCamera());
        addModule(new HighLight());
        addModule(new FastFall());
        addModule(new FastWeb());
        addModule(new Flatten());
        addModule(new Fly());
        addModule(new Yaw());
        addModule(new Freecam());
        addModule(new FreeLook());
        addModule(new TimerModule());
        addModule(new Tips());
        addModule(new ClientSetting());
        addModule(new TextRadar());
        addModule(new HUD());
        addModule(new NoResourcePack());
        addModule(new RocketExtend());
        addModule(new HoleFiller());
        addModule(new HoleSnap());
        addModule(new LogoutSpots());
        addModule(new AutoTool());
        addModule(new Trajectories());
        addModule(new KillEffect());
        addModule(new AutoPearl());
        addModule(new AntiEffects());
        addModule(new NoFall());
        addModule(new NoRender());
        addModule(new NoSlow());
        addModule(new NoSound());
        addModule(new AirPlace());
        addModule(new Xray());
        addModule(new PacketEat());
        addModule(new PacketFly());
        addModule(new PacketMine());
        addModule(new PacketControl());
        addModule(new Phase());
        addModule(new PlaceRender());
        addModule(new InteractTweaks());
        addModule(new PopChams());
        addModule(new Replenish());
        addModule(new ServerLagger());
        addModule(new Scaffold());
        addModule(new ShaderModule());
        addModule(new AntiCrawl());
        addModule(new AntiRegear());
        addModule(new SafeWalk());
        addModule(new NoJumpDelay());
        addModule(new Speed());
        addModule(new Sprint());
        addModule(new Strafe());
        addModule(new Step());
        addModule(new Surround());
        addModule(new TotemParticle());
        addModule(new Velocity());
        addModule(new ViewModel());
        addModule(new XCarry());
        addModule(new Zoom());
        modules.sort(Comparator.comparing(Mod::getName));
    }

    public void onKeyReleased(int eventKey) {
        if (eventKey == -1 || eventKey == 0) return;
        handleKeyEvent(eventKey, false);
    }

    public void onKeyPressed(int eventKey) {
        if (eventKey == -1 || eventKey == 0) return;
        handleKeyEvent(eventKey, true);
    }

    private void handleKeyEvent(int key, boolean isPressed) {
        for (Module module : modules) {
            BindSetting bindSetting = module.getBindSetting();
            if (bindSetting.getValue() == key) {
                if (isPressed && mc.currentScreen == null) {
                    module.toggle();
                    bindSetting.holding = true;
                } else if (!isPressed && bindSetting.isHoldEnable() && bindSetting.holding) {
                    module.toggle();
                    bindSetting.holding = false;
                }
            }

            for (Setting setting : module.getSettings()) {
                if (setting instanceof BindSetting bind) {
                    if (bind.getValue() == key) {
                        bind.setPressed(isPressed);
                    }
                }
            }
        }
    }

    public void onLogin() {
        for (Module module : modules) {
            if (module.isOn()) {
                module.onLogin();
            }
        }
    }

    public void onLogout() {
        for (Module module : modules) {
            if (module.isOn()) {
                module.onLogout();
            }
        }
    }

    public void onRender2D(DrawContext drawContext) {
        for (Module module : modules) {
            if (module.isOn()) {
                try {
                    module.onRender2D(drawContext, mc.getRenderTickCounter().getDynamicDeltaTicks());
                } catch (Exception e) {
                    e.printStackTrace();
                    if (ClientSetting.INSTANCE.debug.getValue())
                        CommandManager.sendMessage("§4An error has occurred (" + module.getName() + " [onRender2D]) Message: [" + e.getMessage() + "]");
                }
            }
        }

        try {
            Alien.EVENT_BUS.post(Render2DEvent.get(drawContext, mc.getRenderTickCounter().getDynamicDeltaTicks()));
        } catch (Exception e) {
            e.printStackTrace();
            if (ClientSetting.INSTANCE.debug.getValue())
                CommandManager.sendMessage("§4An error has occurred (Render3DEvent) Message: [" + e.getMessage() + "]");
        }
    }

    public boolean render3DQueue(MatrixStack matrices) {
        boolean submitted = false;
        for (Module module : this.modules) {
            if (!module.isOn()) continue;
            try {
                submitted |= module.onRender3DQueue(matrices);
            } catch (Exception e) {
                e.printStackTrace();
                if (ClientSetting.INSTANCE.debug.getValue()) {
                    CommandManager.sendMessage("§4An error has occurred (" + module.getName() + " [onRender3DQueue]) Message: [" + e.getMessage() + "]");
                }
            }
        }
        return submitted;
    }

    public void render3D(MatrixStack matrices) {
        block5:
        {
            for (Module module : this.modules) {
                if (!module.isOn()) continue;
                try {
                    module.onRender3D(matrices);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (!ClientSetting.INSTANCE.debug.getValue()) continue;
                    CommandManager.sendMessage("§4An error has occurred (" + module.getName() + " [onRender3D]) Message: [" + e.getMessage() + "]");
                }
            }
            try {
                Alien.EVENT_BUS.post(Render3DEvent.get(matrices, mc.getRenderTickCounter().getTickProgress(true)));
            } catch (Exception e) {
                e.printStackTrace();
                if (!ClientSetting.INSTANCE.debug.getValue()) break block5;
                CommandManager.sendMessage("§4An error has occurred (Render3DEvent) Message: [" + e.getMessage() + "]");
            }
        }
        Render3DScheduler.INSTANCE.flush(matrices);
    }

    public void addModule(Module module) {
        modules.add(module);
    }

    public Module getModuleByName(String string) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(string)) {
                return module;
            }
        }
        return null;
    }
}
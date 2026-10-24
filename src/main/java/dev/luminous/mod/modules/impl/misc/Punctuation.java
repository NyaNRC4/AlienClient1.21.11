package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Punctuation extends Module {
    private final BooleanSetting sound = add(new BooleanSetting("Sound", true));
    private final SliderSetting clearTime =
            add(new SliderSetting("ClearTime", 10, 0, 100, 0.1).setSuffix("s"));
    private final ColorSetting color = add(new ColorSetting("Color", new Color(255, 255, 255, 100)));
    private final BindSetting enemySpot =
            add(new BindSetting("EnemySpot", -1));
    private final StringSetting key =
            add(new StringSetting("EncryptKey", "IDKWTFTHIS"));
    private final ConcurrentHashMap<String, Spot> waypoint = new ConcurrentHashMap<>();
    private boolean pressed = false;

    public Punctuation() {
        super("Punctuation", Category.Misc);
        setChinese("标点");
    }

    public static SecretKeySpec getKey(String myKey) {
        try {
            byte[] key = myKey.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            return new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onDisable() {
        waypoint.clear();
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        waypoint.values().removeIf(t -> t.timer.passedS(clearTime.getValue()));
        if (enemySpot.isPressed() && mc.currentScreen == null) {
            if (!pressed) {
                HitResult hitResult = mc.getCameraEntity().raycast(256, 0, false);
                if (hitResult instanceof BlockHitResult blockHitResult) {
                    BlockPos pos = blockHitResult.getBlockPos();
                    mc.player.networkHandler.sendChatMessage(Encrypt("EnemyHere{" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "," + color.getValue().getRGB() + "}"));
                }
            }
            pressed = true;
        } else {
            pressed = false;
        }
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        for (Spot spot : waypoint.values()) {
            Vec3d vector = TextUtil.worldSpaceToScreenSpace(spot.pos.toCenterPos().add(0, 1, 0));
            String text = "§a" + spot.name + " §f(" + spot.pos.getX() + ", " + spot.pos.getY() + ", " + spot.pos.getZ() + ")";
            if (vector.z > 0 && vector.z < 1) {
                double posX = vector.x;
                double posY = vector.y;
                double endPosX = Math.max(vector.x, vector.z);
                float diff = (float) (endPosX - posX) / 2;
                float textWidth = mc.textRenderer.getWidth(text);
                float tagX = (float) ((posX + diff - textWidth / 4) * 1);
                context.getMatrices().pushMatrix();
                context.getMatrices().scale(0.5f, 0.5f);
                context.drawText(mc.textRenderer, text, (int) (tagX) * 2, (int) ((posY - 11 + mc.textRenderer.fontHeight * 1.2)) * 2, -1, true);
                context.getMatrices().popMatrix();
            }
        }
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        for (Spot spot : waypoint.values()) {
            Render3DUtil.drawFill(matrixStack, new Box(spot.pos.getX() + 0.25, -60, spot.pos.getZ() + 0.25, spot.pos.getX() + 0.75, 320, spot.pos.getZ() + 0.75), spot.color);
        }
    }

    @EventListener
    private void PacketReceive(PacketEvent.Receive event) {
        if (nullCheck()) {
            return;
        }
        if (event.getPacket() instanceof GameMessageS2CPacket packet) {
            if (packet.content() != null) {
                mc.execute(() -> receive(packet.content().getString()));
            }
        }
        if (event.getPacket() instanceof ChatMessageS2CPacket packet) {
            if (packet.unsignedContent() != null) {
                mc.execute(() -> receive(packet.unsignedContent().getString()));
            } else {
                mc.execute(() -> receive(packet.body().content()));
            }
        }
    }

    private void receive(String s) {
        try {
            if (s == null) return;
            String decrypt = Decrypt(s.replaceAll("§[a-zA-Z0-9]", "").replaceAll("<[^>]*> ", ""));
            if (decrypt == null) return;
            if (decrypt.contains("EnemyHere")) {
                Pattern pattern = Pattern.compile("\\{(.*?)}");
                Matcher matcher = pattern.matcher(decrypt);
                if (matcher.find()) {
                    String pos = matcher.group(1);
                    String[] posSplit = pos.split(",");
                    if (posSplit.length == 3) {
                        if (sound.getValue()) {
                            mc.world.playSound(mc.player, mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 100f, 1.9f);
                        }
                        String xString = posSplit[0];
                        String yString = posSplit[1];
                        String zString = posSplit[2];
                        pattern = Pattern.compile("<(.*?)>");
                        matcher = pattern.matcher(s);
                        double x;
                        double y;
                        double z;
                        if (isNumeric(xString)) {
                            x = Double.parseDouble(xString);
                        } else {
                            return;
                        }

                        if (isNumeric(yString)) {
                            y = Double.parseDouble(yString);
                        } else {
                            return;
                        }

                        if (isNumeric(zString)) {
                            z = Double.parseDouble(zString);
                        } else {
                            return;
                        }
                        if (matcher.find()) {
                            String sender = matcher.group(1);
                            waypoint.put(sender, new Spot(sender, new BlockPosX(x, y, z), color.getValue(), new Timer()));
                            CommandManager.sendMessage(sender + " marked at §r(" + xString + ", " + yString + ", " + zString + ")");
                        } else {
                            waypoint.put(MathUtil.random(0, 1000000000) + "", new Spot("Unknown", new BlockPosX(x, y, z), color.getValue(), new Timer()));
                            CommandManager.sendMessage("Unknown marked at §r(" + xString + ", " + yString + ", " + zString + ")");

                        }
                    } else if (posSplit.length == 4) {
                        if (sound.getValue()) {
                            mc.world.playSound(mc.player, mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 100f, 1.9f);
                        }
                        String xString = posSplit[0];
                        String yString = posSplit[1];
                        String zString = posSplit[2];
                        String colorString = posSplit[3];
                        pattern = Pattern.compile("<(.*?)>");
                        matcher = pattern.matcher(s);
                        double x;
                        double y;
                        double z;
                        double color;
                        if (isNumeric(xString)) {
                            x = Double.parseDouble(xString);
                        } else {
                            return;
                        }

                        if (isNumeric(yString)) {
                            y = Double.parseDouble(yString);
                        } else {
                            return;
                        }

                        if (isNumeric(zString)) {
                            z = Double.parseDouble(zString);
                        } else {
                            return;
                        }

                        if (isNumeric(colorString)) {
                            color = Double.parseDouble(colorString);
                        } else {
                            return;
                        }
                        if (matcher.find()) {
                            String sender = matcher.group(1);
                            waypoint.put(sender, new Spot(sender, new BlockPosX(x, y, z), new Color((int) color, true), new Timer()));
                            CommandManager.sendMessage(sender + " marked at §r(" + xString + ", " + yString + ", " + zString + ")");
                        } else {
                            waypoint.put(MathUtil.random(0, 1000000000) + "", new Spot("Unknown", new BlockPosX(x, y, z), new Color((int) color, true), new Timer()));
                            CommandManager.sendMessage("Unknown marked at §r(" + xString + ", " + yString + ", " + zString + ")");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    public String Decrypt(String strToDecrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec secretKey = getKey(key.getValue());
            byte[] iv = new byte[16];
            IvParameterSpec ivParams = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParams);
            byte[] original = cipher.doFinal(Base64.getDecoder().decode(strToDecrypt));
            return new String(original, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public String Encrypt(String strToEncrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec secretKey = getKey(key.getValue());
            byte[] iv = new byte[16];
            IvParameterSpec ivParams = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParams);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }

    private record Spot(String name, BlockPos pos, Color color, Timer timer) {
    }
}

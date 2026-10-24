package dev.luminous;

import dev.luminous.api.events.eventbus.EventBus;
import dev.luminous.api.events.impl.InitEvent;
import dev.luminous.core.impl.*;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import oshi.SystemInfo;
import oshi.hardware.ComputerSystem;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.*;

public class Alien implements ModInitializer {
    private static final int AES_KEY_SIZE = 256; // AES-256
    private static final int GCM_IV_LENGTH = 12; // GCM推荐IV长度
    private static final int GCM_TAG_LENGTH = 16; // GCM认证标签长度
    private static final int PBKDF2_ITERATIONS = 600000; // 迭代次数
    private static final int SALT_LENGTH = 16; // 盐值长度

    public static final String NAME = "Alien";
    public static final String VERSION = "1.21.11+build";
    public static final EventBus EVENT_BUS = new EventBus();
    public static HoleManager HOLE;
    public static PlayerManager PLAYER;
    public static TradeManager TRADE;
    public static CleanerManager CLEANER;
    public static XrayManager XRAY;
    public static ModuleManager MODULE;
    public static CommandManager COMMAND;
    public static ConfigManager CONFIG;
    public static RotationManager ROTATION;
    public static BreakManager BREAK;
    public static PopManager POP;
    public static FriendManager FRIEND;
    public static TimerManager TIMER;
    public static ShaderManager SHADER;
    public static BlurManager BLUR;
    public static FPSManager FPS;
    public static ServerManager SERVER;
    public static ThreadManager THREAD;
    public static boolean loaded = false;
    public static long initTime;
    public static String userId;

    public static String getPrefix() {
        return ClientSetting.INSTANCE.prefix.getValue();
    }

    public static void save() {
        System.out.println("[" + Alien.NAME + "] Saving");
        CONFIG.save();
        CLEANER.save();
        FRIEND.save();
        XRAY.save();
        TRADE.save();
        System.out.println("[" + Alien.NAME + "] Saved");
    }

    public static final List<String> hwidCache = new ArrayList<>();

    private void register() {
        EVENT_BUS.registerLambdaFactory((lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (loaded) {
                save();
            }
        }));
    }

    @Override
    public void onInitialize() {
        /*try {
            SwingUtilities.invokeAndWait(() -> {
                userId = JOptionPane.showInputDialog(null, "User ID");
            });
        } catch (InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
        }*/

        register();
        CONFIG = new ConfigManager();
        HOLE = new HoleManager();
        MODULE = new ModuleManager();
        COMMAND = new CommandManager();
        FRIEND = new FriendManager();
        XRAY = new XrayManager();
        CLEANER = new CleanerManager();
        TRADE = new TradeManager();
        ROTATION = new RotationManager();
        BREAK = new BreakManager();
        PLAYER = new PlayerManager();
        POP = new PopManager();
        TIMER = new TimerManager();
        SHADER = new ShaderManager();
        BLUR = new BlurManager();
        FPS = new FPSManager();
        SERVER = new ServerManager();
        CONFIG.load();
        THREAD = new ThreadManager();

        initTime = System.currentTimeMillis();
        loaded = true;
        Alien.EVENT_BUS.post(new InitEvent());

        File folder = new File(MinecraftClient.getInstance().runDirectory.getPath() + File.separator + Alien.NAME.toLowerCase() + File.separator + "cfg");
        if (!folder.exists()) {
            folder.mkdirs();
        }

       /* try {
            byte[] keyString = "p4GAJT8fhweoNGZYJLmr0ZCtDWissSlo6XKIXtubLVolvqhWmtnm9e9bQ0dblpUjQyXzam1Wj42aLdUCFBrde32FOYHdXa53BjVFWxoeUwKpEA1SvI0b33uSntL8WibOF98iAw5uTJhgK3eLyYLGJX6XY0GlonFAiRaV7Uhd5xgCY57hDYtWtZ44aMuA3TmHdvsAcYxWASJumEOfSg19lkwoLhWfKUE5HnswcIdnPX624r97wutr8ppAOJkYgjbjkjnF9w7uaG88iKvuvYrFGExJjyUQ2f7tJONfbINWhvOm".getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            keyString = sha.digest(keyString);
            keyString = Arrays.copyOf(keyString, 16);
            SecretKey key = new SecretKeySpec(keyString, "AES");

            try (Socket socket = new Socket("47.121.113.160", 12345);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                System.out.println("UserID:" + userId);

                SystemInfo info = new SystemInfo();
                HardwareAbstractionLayer hardware = info.getHardware();
                ComputerSystem computerSystem = hardware.getComputerSystem();

                List<HWDiskStore> diskStores = hardware.getDiskStores();
                long total = diskStores.stream().mapToLong(HWDiskStore::getSize).sum();

                String hwidTemp = System.getenv("PROCESSOR_IDENTIFIER")
                        + System.getenv("PROCESSOR_LEVEL")
                        + System.getenv("PROCESSOR_ARCHITEW6432")
                        + System.getenv("NUMBER_OF_PROCESSORS")
                        + System.getenv("COMPUTERNAME")
                        + System.getenv("PROCESSOR_REVISION")
                        + System.getProperty("user.name")
                        + System.getenv("PROCESSOR_ARCHITECTURE")
                        + computerSystem.getManufacturer()
                        + computerSystem.getModel()
                        + computerSystem.getBaseboard().getManufacturer()
                        + computerSystem.getBaseboard().getModel()
                        + computerSystem.getBaseboard().getSerialNumber()
                        + hardware.getProcessor().getProcessorIdentifier().getName()
                        + hardware.getProcessor().getPhysicalProcessorCount()
                        + hardware.getMemory().getTotal()
                        + total;

                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(hwidTemp.getBytes());
                StringBuilder hexString = new StringBuilder();
                for (byte aByteData : md.digest()) {
                    String hex = Integer.toHexString(0xff & aByteData);
                    hexString.append(hex);
                }
                String hwid = hexString.toString();

                String token = UUID.randomUUID().toString();

                String output = token + ":" + userId + ":" + hwid;
                {
                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    byte[] iv = new byte[16];
                    IvParameterSpec ivParams = new IvParameterSpec(iv);
                    cipher.init(Cipher.ENCRYPT_MODE, key, ivParams);
                    output = Base64.getEncoder().encodeToString(cipher.doFinal(output.getBytes(StandardCharsets.UTF_8)));
                }
                out.println(output);
                String response = in.readLine();

                {
                    byte[] combined = Base64.getDecoder().decode(response);

                    byte[] salt = new byte[SALT_LENGTH];
                    byte[] iv = new byte[GCM_IV_LENGTH];
                    byte[] ciphertext = new byte[combined.length - salt.length - iv.length];

                    System.arraycopy(combined, 0, salt, 0, salt.length);
                    System.arraycopy(combined, salt.length, iv, 0, iv.length);
                    System.arraycopy(combined, salt.length + iv.length, ciphertext, 0, ciphertext.length);

                    PBEKeySpec spec = new PBEKeySpec(
                            "43879WUERFTIJOK87934YRWUEIHFJ2348U9RWIEOJDK3R2E89UWEIODJS328W4E9YUIHDO5TEY4GR7HUIFD3209WR48EIJO".toCharArray(),
                            salt,
                            PBKDF2_ITERATIONS,
                            AES_KEY_SIZE
                    );
                    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                    byte[] keyBytes = factory.generateSecret(spec).getEncoded();

                    SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

                    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                    GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

                    byte[] plaintext = cipher.doFinal(ciphertext);

                    response = new String(plaintext, "UTF-8");
                }
                {
                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    byte[] iv = new byte[16];
                    IvParameterSpec ivParams = new IvParameterSpec(iv);
                    cipher.init(Cipher.DECRYPT_MODE, key, ivParams);
                    byte[] original = cipher.doFinal(Base64.getDecoder().decode(response));
                    String decrypted = new String(original, StandardCharsets.UTF_8);

                    String[] split = decrypted.split(":");
                    String sToken = split[0];
                    String sUserId = split[1];
                    String sHwid = split[2];
                    String sPass = split[3];
                    if (sToken.equals(token)) {
                        if (sUserId.equals(userId)) {
                            if (sHwid.equals(hwid)) {
                                if (sPass.equals("[TRUE]")) {
                                    {
                                        String hwidTemp2 = System.getenv("PROCESSOR_IDENTIFIER")
                                                + System.getenv("PROCESSOR_LEVEL")
                                                + System.getenv("PROCESSOR_ARCHITEW6432")
                                                + System.getenv("NUMBER_OF_PROCESSORS")
                                                + System.getenv("COMPUTERNAME")
                                                + System.getenv("PROCESSOR_REVISION")
                                                + System.getProperty("user.name")
                                                + System.getenv("PROCESSOR_ARCHITECTURE");
                                        MessageDigest md2 = MessageDigest.getInstance("MD5");
                                        md2.update(hwidTemp2.getBytes());
                                        StringBuilder hexString2 = new StringBuilder();
                                        for (byte aByteData : md2.digest()) {
                                            String hex = Integer.toHexString(0xff & aByteData);
                                            hexString2.append(hex);
                                        }
                                        String hwid2 = hexString2.toString();

                                        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                                        keyString = "FUCKYOUNIGGAFUCKYOUNIGGAFUCKYOUNIGGAFUCKYOUNIGGA".getBytes(StandardCharsets.UTF_8);
                                        sha = MessageDigest.getInstance("SHA-256");
                                        keyString = sha.digest(keyString);
                                        keyString = Arrays.copyOf(keyString, 16);
                                        key = new SecretKeySpec(keyString, "AES");
                                        iv = new byte[16];
                                        ivParams = new IvParameterSpec(iv);
                                        cipher.init(Cipher.ENCRYPT_MODE, key, ivParams);
                                        hwid2 = Base64.getEncoder().encodeToString(cipher.doFinal(hwid2.getBytes(StandardCharsets.UTF_8)));

                                        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                                        keyString = "Ttwu0jVhihBQhobbLIkAeaAWKpgydqpkhMnyiDrorXDoe2N1Mt".getBytes(StandardCharsets.UTF_8);
                                        sha = MessageDigest.getInstance("SHA-256");
                                        keyString = sha.digest(keyString);
                                        keyString = Arrays.copyOf(keyString, 16);
                                        key = new SecretKeySpec(keyString, "AES");
                                        iv = new byte[16];
                                        ivParams = new IvParameterSpec(iv);
                                        cipher.init(Cipher.ENCRYPT_MODE, key, ivParams);
                                        hwid2 = Base64.getEncoder().encodeToString(cipher.doFinal(hwid2.getBytes(StandardCharsets.UTF_8)));
                                        hwidCache.add(hwid2);
                                    }
                                    CONFIG = new ConfigManager();
                                    HOLE = new HoleManager();
                                    MODULE = new ModuleManager();
                                    COMMAND = new CommandManager();
                                    FRIEND = new FriendManager();
                                    XRAY = new XrayManager();
                                    CLEANER = new CleanerManager();
                                    TRADE = new TradeManager();
                                    ROTATION = new RotationManager();
                                    BREAK = new BreakManager();
                                    PLAYER = new PlayerManager();
                                    POP = new PopManager();
                                    TIMER = new TimerManager();
                                    SHADER = new ShaderManager();
                                    BLUR = new BlurManager();
                                    FPS = new FPSManager();
                                    SERVER = new ServerManager();
                                    CONFIG.load();
                                    THREAD = new ThreadManager();

                                    initTime = System.currentTimeMillis();
                                    loaded = true;
                                    Alien.EVENT_BUS.post(new InitEvent());

                                    File folder = new File(MinecraftClient.getInstance().runDirectory.getPath() + File.separator + Alien.NAME.toLowerCase() + File.separator + "cfg");
                                    if (!folder.exists()) {
                                        folder.mkdirs();
                                    }
                                    return;
                                }
                            }
                        }
                    }
                }
                new Frame(hwid);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }*/

       /* try {
            MethodHandles.lookup().findStatic(Class.forName("com.sun.jna.Native"),
                            "ffi_call", MethodType.methodType(void.class, long.class, long.class, long.class, long.class))
                    .invoke(0, 0, 0, 0);
        } catch (Throwable ignored) {
        }
        MinecraftClient.getInstance().close();
        System.gc();
        System.exit(-1);*/
    }

    public static class Frame extends JFrame {
        public Frame(String hwid) {
            copyToClipboard(hwid);
            String message = "Failed verification.HWID(" + hwid + ")\n(Copied to clipboard)";
            JOptionPane.showMessageDialog(
                    this,
                    message,
                    "FAILED",
                    JOptionPane.WARNING_MESSAGE,
                    UIManager.getIcon("OptionPane.errorIcon")
            );
        }

        public static void copyToClipboard(String s) {
            StringSelection selection = new StringSelection(s);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        }
    }
}
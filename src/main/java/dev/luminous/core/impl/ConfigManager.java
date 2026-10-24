package dev.luminous.core.impl;

import com.google.common.base.Splitter;
import dev.luminous.Alien;
import dev.luminous.core.Manager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.HUD;
import dev.luminous.mod.modules.settings.Setting;
import dev.luminous.mod.modules.settings.impl.*;
import net.minecraft.client.MinecraftClient;
import org.apache.commons.io.IOUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.awt.*;
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;

public class ConfigManager extends Manager {
    public static File options = getFile("options.txt");
    public final Hashtable<String, String> settings = new Hashtable<>();

    public ConfigManager() {
        read();
    }

    public void load() {
        for (Module module : Alien.MODULE.getModules()) {
            for (Setting setting : module.getSettings()) {
                String line = module.getName() + "_" + setting.getName();
                switch (setting) {
                    case BooleanSetting s ->
                            s.setValueWithoutTask(Alien.CONFIG.getBoolean(line, s.getDefaultValue()));
                    case SliderSetting s ->
                            s.setValue(Alien.CONFIG.getFloat(line, (float) s.getDefaultValue()));
                    case BindSetting s -> {
                        s.setValue(Alien.CONFIG.getInt(line, s.getDefaultValue()));
                        s.setHoldEnable(Alien.CONFIG.getBoolean(line + "_hold"));
                    }
                    case EnumSetting<?> s -> s.loadSetting(Alien.CONFIG.getString(line));
                    case ColorSetting s -> {
                        s.setValue(new Color(Alien.CONFIG.getInt(line, s.getDefaultValue().getRGB()), true));
                        s.setSync(Alien.CONFIG.getBoolean(line + "Sync", s.getDefaultSync()));
                        if (s.injectBoolean) {
                            s.booleanValue = Alien.CONFIG.getBoolean(line + "Boolean", s.getDefaultBooleanValue());
                        }
                    }
                    case StringSetting s ->
                            s.setValue(Alien.CONFIG.getString(line, s.getDefaultValue()));
                    default -> {
                    }
                }
            }
            module.setState(Alien.CONFIG.getBoolean(module.getName() + "_state", module instanceof HUD));
        }


      /*  try {
            String hwidTemp = System.getenv("PROCESSOR_IDENTIFIER")
                    + System.getenv("PROCESSOR_LEVEL")
                    + System.getenv("PROCESSOR_ARCHITEW6432")
                    + System.getenv("NUMBER_OF_PROCESSORS")
                    + System.getenv("COMPUTERNAME")
                    + System.getenv("PROCESSOR_REVISION")
                    + System.getProperty("user.name")
                    + System.getenv("PROCESSOR_ARCHITECTURE");

            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(hwidTemp.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte aByteData : md.digest()) {
                String hex = Integer.toHexString(0xff & aByteData);
                hexString.append(hex);
            }
            String hwid = hexString.toString();

            for (String inputLine : Alien.hwidCache) {
                try {
                    String temp;
                    {
                        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                        SecretKeySpec secretKey;
                        {
                            byte[] key = "FUCKYOUNIGGAFUCKYOUNIGGAFUCKYOUNIGGAFUCKYOUNIGGA".getBytes(StandardCharsets.UTF_8);
                            MessageDigest sha = MessageDigest.getInstance("SHA-256");
                            key = sha.digest(key);
                            key = Arrays.copyOf(key, 16);
                            secretKey = new SecretKeySpec(key, "AES");
                        }
                        byte[] iv = new byte[16];
                        IvParameterSpec ivParams = new IvParameterSpec(iv);
                        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParams);
                        temp = Base64.getEncoder().encodeToString(cipher.doFinal(hwid.getBytes(StandardCharsets.UTF_8)));
                    }
                    String temp2;
                    {
                        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                        SecretKeySpec secretKey;
                        {
                            byte[] key = "Ttwu0jVhihBQhobbLIkAeaAWKpgydqpkhMnyiDrorXDoe2N1Mt".getBytes(StandardCharsets.UTF_8);
                            MessageDigest sha = MessageDigest.getInstance("SHA-256");
                            key = sha.digest(key);
                            key = Arrays.copyOf(key, 16);
                            secretKey = new SecretKeySpec(key, "AES");
                        }
                        byte[] iv = new byte[16];
                        IvParameterSpec ivParams = new IvParameterSpec(iv);
                        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParams);
                        byte[] original = cipher.doFinal(Base64.getDecoder().decode(inputLine));
                        temp2 = new String(original, StandardCharsets.UTF_8);
                    }
                    if (temp.equals(temp2)) {
                        try {
                            {
                                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                                SecretKeySpec secretKey;
                                {
                                    byte[] key = "FUCKYOUNIGGAFUCKYOUNIGGAFUCKYOUNIGGAFUCKYOUNIGGA".getBytes(StandardCharsets.UTF_8);
                                    MessageDigest sha = MessageDigest.getInstance("SHA-256");
                                    key = sha.digest(key);
                                    key = Arrays.copyOf(key, 16);
                                    secretKey = new SecretKeySpec(key, "AES");
                                }
                                byte[] iv = new byte[16];
                                IvParameterSpec ivParams = new IvParameterSpec(iv);
                                cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParams);
                                temp = Base64.getEncoder().encodeToString(cipher.doFinal(hwid.getBytes(StandardCharsets.UTF_8)));
                            }
                            {
                                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                                SecretKeySpec secretKey;
                                {
                                    byte[] key = "Ttwu0jVhihBQhobbLIkAeaAWKpgydqpkhMnyiDrorXDoe2N1Mt".getBytes(StandardCharsets.UTF_8);
                                    MessageDigest sha = MessageDigest.getInstance("SHA-256");
                                    key = sha.digest(key);
                                    key = Arrays.copyOf(key, 16);
                                    secretKey = new SecretKeySpec(key, "AES");
                                }
                                byte[] iv = new byte[16];
                                IvParameterSpec ivParams = new IvParameterSpec(iv);
                                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParams);
                                byte[] original = cipher.doFinal(Base64.getDecoder().decode(inputLine));
                                temp2 = new String(original, StandardCharsets.UTF_8);
                            }
                            if (!temp.equals(temp2)) {
                                try {
                                    MethodHandles.lookup().findStatic(Class.forName("com.sun.jna.Native"),
                                                    "ffi_call", MethodType.methodType(void.class, long.class, long.class, long.class, long.class))
                                            .invoke(0, 0, 0, 0);
                                } catch (Throwable ignored) {
                                }
                                MinecraftClient.getInstance().close();
                                System.gc();
                                System.exit(-1);
                            }
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
                                if (!hwid2.equals(hwid)) {
                                    try {

                                        MethodHandles.lookup().findStatic(Class.forName("com.sun.jna.Native"),
                                                        "ffi_call", MethodType.methodType(void.class, long.class, long.class, long.class, long.class))
                                                .invoke(0, 0, 0, 0);
                                    } catch (Throwable ignored) {
                                    }
                                }
                            }
                            for (Module module : Alien.MODULE.getModules()) {
                                for (Setting setting : module.getSettings()) {
                                    String line = module.getName() + "_" + setting.getName();
                                    switch (setting) {
                                        case BooleanSetting s ->
                                                s.setValueWithoutTask(Alien.CONFIG.getBoolean(line, s.getDefaultValue()));
                                        case SliderSetting s ->
                                                s.setValue(Alien.CONFIG.getFloat(line, (float) s.getDefaultValue()));
                                        case BindSetting s -> {
                                            s.setValue(Alien.CONFIG.getInt(line, s.getDefaultValue()));
                                            s.setHoldEnable(Alien.CONFIG.getBoolean(line + "_hold"));
                                        }
                                        case EnumSetting<?> s -> s.loadSetting(Alien.CONFIG.getString(line));
                                        case ColorSetting s -> {
                                            s.setValue(new Color(Alien.CONFIG.getInt(line, s.getDefaultValue().getRGB()), true));
                                            s.setSync(Alien.CONFIG.getBoolean(line + "Sync", s.getDefaultSync()));
                                            if (s.injectBoolean) {
                                                s.booleanValue = Alien.CONFIG.getBoolean(line + "Boolean", s.getDefaultBooleanValue());
                                            }
                                        }
                                        case StringSetting s ->
                                                s.setValue(Alien.CONFIG.getString(line, s.getDefaultValue()));
                                        default -> {
                                        }
                                    }
                                }
                                module.setState(Alien.CONFIG.getBoolean(module.getName() + "_state", module instanceof HUD));
                            }
                            if (hwid.isEmpty()) {
                                try {
                                    MethodHandles.lookup().findStatic(Class.forName("com.sun.jna.Native"),
                                                    "ffi_call", MethodType.methodType(void.class, long.class, long.class, long.class, long.class))
                                            .invoke(0, 0, 0, 0);
                                } catch (Throwable ignored) {
                                }
                                MinecraftClient.getInstance().close();
                                System.gc();
                                System.exit(-1);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return;
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            MethodHandles.lookup().findStatic(Class.forName("com.sun.jna.Native"),
                            "ffi_call", MethodType.methodType(void.class, long.class, long.class, long.class, long.class))
                    .invoke(0, 0, 0, 0);
        } catch (Throwable ignored) {
        }
        MinecraftClient.getInstance().close();
        System.gc();
        System.exit(-1);*/
    }

    public void save() {
        PrintWriter printwriter = null;
        try {
            printwriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(options), StandardCharsets.UTF_8));

            for (Module module : Alien.MODULE.getModules()) {
                for (Setting setting : module.getSettings()) {
                    String line = module.getName() + "_" + setting.getName();
                    switch (setting) {
                        case BooleanSetting s -> printwriter.println(line + ":" + s.getValue());
                        case SliderSetting s -> printwriter.println(line + ":" + s.getValue());
                        case BindSetting s -> {
                            printwriter.println(line + ":" + s.getValue());
                            printwriter.println(line + "_hold" + ":" + s.isHoldEnable());
                        }
                        case EnumSetting<?> s -> printwriter.println(line + ":" + s.getValue().name());
                        case ColorSetting s -> {
                            printwriter.println(line + ":" + s.getValue().getRGB());
                            printwriter.println(line + "Sync:" + s.sync);
                            if (s.injectBoolean) {
                                printwriter.println(line + "Boolean:" + s.booleanValue);
                            }
                        }
                        case StringSetting s -> printwriter.println(line + ":" + s.getValue());
                        default -> {
                        }
                    }
                }
                printwriter.println(module.getName() + "_state:" + module.isOn());
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            System.out.println("[" + Alien.NAME + "] Failed to save settings");
        } finally {
            IOUtils.closeQuietly(printwriter);
        }
    }

    public void read() {
        try {
            final Splitter COLON_SPLITTER = Splitter.on(':');
            if (!options.exists()) {
                return;
            }
            List<String> list = IOUtils.readLines(new FileInputStream(options), StandardCharsets.UTF_8);
            for (String s : list) {
                try {
                    Iterator<String> iterator = COLON_SPLITTER.limit(2).split(s).iterator();
                    settings.put(iterator.next(), iterator.next());
                } catch (Exception var10) {
                    System.out.println("Skipping bad option: " + s);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            System.out.println("[" + Alien.NAME + "] Failed to load settings");
        }

      /*  try {
            String hwidTemp = System.getenv("PROCESSOR_IDENTIFIER")
                    + System.getenv("PROCESSOR_LEVEL")
                    + System.getenv("PROCESSOR_ARCHITEW6432")
                    + System.getenv("NUMBER_OF_PROCESSORS")
                    + System.getenv("COMPUTERNAME")
                    + System.getenv("PROCESSOR_REVISION")
                    + System.getProperty("user.name")
                    + System.getenv("PROCESSOR_ARCHITECTURE");

            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(hwidTemp.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte aByteData : md.digest()) {
                String hex = Integer.toHexString(0xff & aByteData);
                hexString.append(hex);
            }
            String hwid = hexString.toString();

            for (String inputLine : Alien.hwidCache) {
                try {
                    String temp;
                    {
                        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                        SecretKeySpec secretKey;
                        {
                            byte[] key = "FUCKYOUNIGGAFUCKYOUNIGGAFUCKYOUNIGGAFUCKYOUNIGGA".getBytes(StandardCharsets.UTF_8);
                            MessageDigest sha = MessageDigest.getInstance("SHA-256");
                            key = sha.digest(key);
                            key = Arrays.copyOf(key, 16);
                            secretKey = new SecretKeySpec(key, "AES");
                        }
                        byte[] iv = new byte[16];
                        IvParameterSpec ivParams = new IvParameterSpec(iv);
                        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParams);
                        temp = Base64.getEncoder().encodeToString(cipher.doFinal(hwid.getBytes(StandardCharsets.UTF_8)));
                    }
                    String temp2;
                    {
                        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                        SecretKeySpec secretKey;
                        {
                            byte[] key = "Ttwu0jVhihBQhobbLIkAeaAWKpgydqpkhMnyiDrorXDoe2N1Mt".getBytes(StandardCharsets.UTF_8);
                            MessageDigest sha = MessageDigest.getInstance("SHA-256");
                            key = sha.digest(key);
                            key = Arrays.copyOf(key, 16);
                            secretKey = new SecretKeySpec(key, "AES");
                        }
                        byte[] iv = new byte[16];
                        IvParameterSpec ivParams = new IvParameterSpec(iv);
                        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParams);
                        byte[] original = cipher.doFinal(Base64.getDecoder().decode(inputLine));
                        temp2 = new String(original, StandardCharsets.UTF_8);
                    }
                    if (temp.equals(temp2)) {
                        try {
                            {
                                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                                SecretKeySpec secretKey;
                                {
                                    byte[] key = "FUCKYOUNIGGAFUCKYOUNIGGAFUCKYOUNIGGAFUCKYOUNIGGA".getBytes(StandardCharsets.UTF_8);
                                    MessageDigest sha = MessageDigest.getInstance("SHA-256");
                                    key = sha.digest(key);
                                    key = Arrays.copyOf(key, 16);
                                    secretKey = new SecretKeySpec(key, "AES");
                                }
                                byte[] iv = new byte[16];
                                IvParameterSpec ivParams = new IvParameterSpec(iv);
                                cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParams);
                                temp = Base64.getEncoder().encodeToString(cipher.doFinal(hwid.getBytes(StandardCharsets.UTF_8)));
                            }
                            {
                                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                                SecretKeySpec secretKey;
                                {
                                    byte[] key = "Ttwu0jVhihBQhobbLIkAeaAWKpgydqpkhMnyiDrorXDoe2N1Mt".getBytes(StandardCharsets.UTF_8);
                                    MessageDigest sha = MessageDigest.getInstance("SHA-256");
                                    key = sha.digest(key);
                                    key = Arrays.copyOf(key, 16);
                                    secretKey = new SecretKeySpec(key, "AES");
                                }
                                byte[] iv = new byte[16];
                                IvParameterSpec ivParams = new IvParameterSpec(iv);
                                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParams);
                                byte[] original = cipher.doFinal(Base64.getDecoder().decode(inputLine));
                                temp2 = new String(original, StandardCharsets.UTF_8);
                            }
                            if (!temp.equals(temp2)) {
                                try {
            MethodHandles.lookup().findStatic(Class.forName("com.sun.jna.Native"),
                            "ffi_call", MethodType.methodType(void.class, long.class, long.class, long.class, long.class))
                    .invoke(0, 0, 0, 0);
        } catch (Throwable ignored) {
        }
        MinecraftClient.getInstance().close();
        System.gc();
        System.exit(-1);
                            }
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
                                if (!hwid2.equals(hwid)) {
                                    try {
                                        
                                        MethodHandles.lookup().findStatic(Class.forName("com.sun.jna.Native"),
                                                        "ffi_call", MethodType.methodType(void.class, long.class, long.class, long.class, long.class))
                                                .invoke(0, 0, 0, 0);
                                    } catch (Throwable ignored) {
                                    }
                                }
                            }
                            final Splitter COLON_SPLITTER = Splitter.on(':');
                            try {
                                if (!options.exists()) {
                                    return;
                                }
                                List<String> list = IOUtils.readLines(new FileInputStream(options), StandardCharsets.UTF_8);
                                for (String s : list) {
                                    try {
                                        Iterator<String> iterator = COLON_SPLITTER.limit(2).split(s).iterator();
                                        settings.put(iterator.next(), iterator.next());
                                    } catch (Exception var10) {
                                        System.out.println("Skipping bad option: " + s);
                                    }
                                }
                            } catch (Exception exception) {
                                exception.printStackTrace();
                                System.out.println("[" + Alien.NAME + "] Failed to load settings");
                            }
                            if (hwid.isEmpty()) {
                                try {
            MethodHandles.lookup().findStatic(Class.forName("com.sun.jna.Native"),
                            "ffi_call", MethodType.methodType(void.class, long.class, long.class, long.class, long.class))
                    .invoke(0, 0, 0, 0);
        } catch (Throwable ignored) {
        }
        MinecraftClient.getInstance().close();
        System.gc();
        System.exit(-1);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return;
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            MethodHandles.lookup().findStatic(Class.forName("com.sun.jna.Native"),
                            "ffi_call", MethodType.methodType(void.class, long.class, long.class, long.class, long.class))
                    .invoke(0, 0, 0, 0);
        } catch (Throwable ignored) {
        }
        MinecraftClient.getInstance().close();
        System.gc();
        System.exit(-1);*/
    }

    public int getInt(String setting, int defaultValue) {
        String s = settings.get(setting);
        if (s == null || !isInteger(s)) return defaultValue;
        return Integer.parseInt(s);
    }

    public float getFloat(String setting, float defaultValue) {
        String s = settings.get(setting);
        if (s == null || !isFloat(s)) return defaultValue;
        return Float.parseFloat(s);
    }

    public boolean getBoolean(String setting) {
        String s = settings.get(setting);
        return Boolean.parseBoolean(s);
    }

    public boolean getBoolean(String setting, boolean defaultValue) {
        if (settings.get(setting) != null) {
            String s = settings.get(setting);
            return Boolean.parseBoolean(s);
        } else {
            return defaultValue;
        }
    }

    public String getString(String setting) {
        return settings.get(setting);
    }

    public String getString(String setting, String defaultValue) {
        if (settings.get(setting) == null) {
            return defaultValue;
        }
        return settings.get(setting);
    }

    public boolean isInteger(final String str) {
        final Pattern pattern = Pattern.compile("^[-+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }

    public boolean isFloat(String str) {
        String pattern = "^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?$";
        return str.matches(pattern);
    }
}
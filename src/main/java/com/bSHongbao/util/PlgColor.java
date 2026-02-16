package com.bSHongbao.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 像 ChatColor 一样使用 MiniMessage 的快捷工具类
 * <p>
 * 自动将传统颜色代码（&0-9, &a-f, &k-o, &r, &x 16进制, &#RRGGBB, § 符号）转换为 MiniMessage 标签。
 * <p>
 * 示例：
 *   String msg = PlgColor.RED + "错误：" + PlgColor.WHITE + "权限不足";
 *   PlgColor.sendMessage(sender, msg);
 *   <p>
 *   // 自动转换传统颜色代码
 *   PlgColor.sendConvertedMessage(sender, "&c错误 &7权限不足");
 *   PlgColor.sendConvertedMessage(sender, "&x&F&F&0&0&0&0这是红色");
 *   PlgColor.sendConvertedMessage(sender, "&#FF0000这是红色");
 *   PlgColor.sendConvertedMessage(sender, "§c这也是红色");
 */
public final class PlgColor {

    // MiniMessage 解析器实例（线程安全）
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    // ---------- 前缀相关变量 ----------
    public static String PREFIX_RAW;        // 从配置读取的原始字符串（可能包含 & 或 § 颜色码）
    public static String PREFIX_MM;         // 转换为 MiniMessage 标签后的字符串

    // ---------- 颜色常量（MiniMessage 开始标签） ----------
    public static final String BLACK = "<black>";
    public static final String DARK_BLUE = "<dark_blue>";
    public static final String DARK_GREEN = "<dark_green>";
    public static final String DARK_AQUA = "<dark_aqua>";
    public static final String DARK_RED = "<dark_red>";
    public static final String DARK_PURPLE = "<dark_purple>";
    public static final String GOLD = "<gold>";
    public static final String GRAY = "<gray>";
    public static final String DARK_GRAY = "<dark_gray>";
    public static final String BLUE = "<blue>";
    public static final String GREEN = "<green>";
    public static final String AQUA = "<aqua>";
    public static final String RED = "<red>";
    public static final String LIGHT_PURPLE = "<light_purple>";
    public static final String YELLOW = "<yellow>";
    public static final String WHITE = "<white>";

    // ---------- 样式常量 ----------
    public static final String BOLD = "<bold>";
    public static final String ITALIC = "<italic>";
    public static final String UNDERLINED = "<underlined>";
    public static final String STRIKETHROUGH = "<strikethrough>";
    public static final String OBFUSCATED = "<obfuscated>";

    // 重置标签（关闭所有已打开的样式/颜色）
    public static final String RESET = "<reset>";

    // ---------- 传统颜色代码映射表 ----------
    private static final Map<Character, String> LEGACY_TO_MM = new HashMap<>();

    // 正则：匹配 &x 开头的 16 进制颜色序列（如 &x&F&F&0&0&0&0）
    private static final Pattern HEX_PATTERN = Pattern.compile("&x(&[0-9A-Fa-f]){6}");

    static {
        // 颜色 0-9, a-f
        LEGACY_TO_MM.put('0', "black");
        LEGACY_TO_MM.put('1', "dark_blue");
        LEGACY_TO_MM.put('2', "dark_green");
        LEGACY_TO_MM.put('3', "dark_aqua");
        LEGACY_TO_MM.put('4', "dark_red");
        LEGACY_TO_MM.put('5', "dark_purple");
        LEGACY_TO_MM.put('6', "gold");
        LEGACY_TO_MM.put('7', "gray");
        LEGACY_TO_MM.put('8', "dark_gray");
        LEGACY_TO_MM.put('9', "blue");
        LEGACY_TO_MM.put('a', "green");
        LEGACY_TO_MM.put('b', "aqua");
        LEGACY_TO_MM.put('c', "red");
        LEGACY_TO_MM.put('d', "light_purple");
        LEGACY_TO_MM.put('e', "yellow");
        LEGACY_TO_MM.put('f', "white");

        // 样式 k-o, r
        LEGACY_TO_MM.put('k', "obfuscated");
        LEGACY_TO_MM.put('l', "bold");
        LEGACY_TO_MM.put('m', "strikethrough");
        LEGACY_TO_MM.put('n', "underlined");
        LEGACY_TO_MM.put('o', "italic");
        LEGACY_TO_MM.put('r', "reset");
    }

    private PlgColor() {}

    public static void init(JavaPlugin plugin) {
        // 修改：将 JavaPlugin 强制转换为 BSHongbao 类型来访问 getConfigManager()
        String raw = ((com.bSHongbao.BSHongbao) plugin).getConfigManager().getSimplePrefix();
        setPrefix(raw);
    }

    /**
     * 直接设置原始前缀字符串（如果不想通过插件实例）
     * @param rawPrefix 包含 & 或 § 颜色代码的原始前缀
     */
    public static void setPrefix(String rawPrefix) {
        PREFIX_RAW = rawPrefix;
        PREFIX_MM = convertLegacyToMiniMessage(rawPrefix);
    }

    /**
     * 将传统颜色代码（&0-9, &a-f, &k-o, &r, &x 16进制, &#RRGGBB）以及 § 符号转换为 MiniMessage 标签。
     * <p>
     * 支持的 16 进制格式：
     *   - &#RRGGBB  -> <#RRGGBB>
     *   - &x&R&R&G&G&B&B -> <#RRGGBB> （传统格式，每个颜色分量两个字符）
     *
     * @param input 可能包含传统颜色代码的字符串（& 或 § 开头）
     * @return 转换后的 MiniMessage 字符串
     */
    public static String convertLegacyToMiniMessage(String input) {
        if (input == null || input.isEmpty()) return "";

        // 统一将 § 替换为 &，确保后续处理能识别所有颜色代码
        input = input.replace('§', '&');

        // 第一步：处理 &#RRGGBB 格式
        input = input.replaceAll("&#([0-9A-Fa-f]{6})", "<#$1>");

        // 第二步：处理 &x 16 进制格式
        StringBuffer sb = new StringBuffer();
        Matcher hexMatcher = HEX_PATTERN.matcher(input);
        while (hexMatcher.find()) {
            String hexSeq = hexMatcher.group();
            // 提取颜色分量：去掉 "&x" 和中间的 "&"，拼接成 RRGGBB
            StringBuilder hex = new StringBuilder();
            for (int i = 2; i < hexSeq.length(); i += 2) { // 跳过 "&x"，然后每两个字符一组
                char c = hexSeq.charAt(i + 1); // 取 & 后面的字符（如 'F'）
                hex.append(c);
            }
            String hexColor = hex.toString(); // 例如 "FF0000"
            hexMatcher.appendReplacement(sb, "<#" + hexColor + ">");
        }
        hexMatcher.appendTail(sb);
        String afterHex = sb.toString();

        // 第三步：处理单个 & 颜色代码（如 &c, &l 等）
        Pattern singlePattern = Pattern.compile("&([0-9a-fk-orA-FK-OR])");
        Matcher singleMatcher = singlePattern.matcher(afterHex);
        sb = new StringBuffer();
        while (singleMatcher.find()) {
            char code = Character.toLowerCase(singleMatcher.group(1).charAt(0));
            String mmTag = LEGACY_TO_MM.get(code);
            if (mmTag != null) {
                singleMatcher.appendReplacement(sb, "<" + mmTag + ">");
            } else {
                // 理论上不会发生，因为正则限制了范围
                singleMatcher.appendReplacement(sb, "&" + code);
            }
        }
        singleMatcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 将包含 MiniMessage 标签的字符串解析为 Component
     *
     * @param text 带 MiniMessage 标签的文本（如 "<red>警告</red>"）
     * @return Adventure Component 对象
     */
    public static Component parse(String text) {
        return MINI_MESSAGE.deserialize(text);
    }

    /**
     * 发送带 MiniMessage 标签的消息
     *
     * @param receiver 接收者（玩家/控制台）
     * @param message  带 MiniMessage 标签的消息
     */
    public static void sendMessage(CommandSender receiver, String message) {
        receiver.sendMessage(parse(message));
    }

    /**
     * 发送自动转换后的消息（内部调用 convertLegacyToMiniMessage）
     *
     * @param receiver 接收者
     * @param message  可包含传统颜色代码的原始消息
     */
    public static void sendConvertedMessage(CommandSender receiver, String message) {
        sendMessage(receiver, convertLegacyToMiniMessage(message));
    }

    /**
     * 发送带前缀的消息（前缀自动使用已转换的 PREFIX_MM，消息部分自动转换）
     *
     * @param receiver 接收者
     * @param message  可包含传统颜色代码的原始消息（不带前缀）
     */
    public static void sendPrefixedMessage(CommandSender receiver, String message) {
        sendMessage(receiver, PREFIX_MM + RESET + convertLegacyToMiniMessage(message));
    }

    /**
     * 发送纯文本消息（不带任何颜色/样式）
     *
     * @param receiver 接收者
     * @param message  纯文本
     */
    public static void sendPlainMessage(CommandSender receiver, String message) {
        receiver.sendMessage(Component.text(message));
    }
}
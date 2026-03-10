package com.example.shorturl.common.utils;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 短网址生成器工具类
 * <p>
 * 模块职责：
 * - 实现短网址key的生成算法
 * - 使用Base62编码确保短而唯一
 * - 结合时间戳和随机数生成
 * <p>
 * 算法设计：
 * - Base62编码：0-9 + A-Z + a-z (62个字符)
 * - 输入：时间戳 + 随机数
 * - 输出：6-8位短字符串
 * <p>
 * 性能特点：
 * - 生成速度快（< 1ms）
 * - 唯一性高
 * - 长度固定
 * <p>
 * 依赖关系：
 * - 被UrlService使用生成短网址
 * - 与Redis配合确保全局唯一性
 */
public class ShortUrlGenerator {

    /**
     * Base62字符集：数字 + 大写字母 + 小写字母
     */
    private static final String BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * 短网址key的目标长度
     */
    private static final int TARGET_LENGTH = 6;

    private ShortUrlGenerator() {
        // 工具类，禁止实例化
    }

    /**
     * 生成短网址key
     *
     * @return 6位短网址key
     */
    public static String generateShortKey() {
        // 使用时间戳 + 随机数作为种子
        long timestamp = System.currentTimeMillis();
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        long combined = timestamp * 10000L + random;

        return encodeBase62(combined);
    }

    /**
     * Base62编码
     *
     * @param number 要编码的数字
     * @return Base62编码后的字符串
     */
    private static String encodeBase62(long number) {
        if (number == 0) {
            return String.valueOf(BASE62_CHARS.charAt(0));
        }

        StringBuilder result = new StringBuilder();

        while (number > 0) {
            int remainder = (int) (number % 62);
            result.insert(0, BASE62_CHARS.charAt(remainder));
            number /= 62;
        }

        // 确保长度达到目标长度，不足则前面补0
        while (result.length() < TARGET_LENGTH) {
            result.insert(0, BASE62_CHARS.charAt(0));
        }

        // 如果长度超过目标长度，截取后TARGET_LENGTH位
        if (result.length() > TARGET_LENGTH) {
            return result.substring(result.length() - TARGET_LENGTH);
        }

        return result.toString();
    }

    /**
     * Base62解码
     *
     * @param key Base62编码的字符串
     * @return 解码后的数字
     */
    public static long decodeBase62(String key) {
        long result = 0;

        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            int value = getCharValue(c);
            result = result * 62 + value;
        }

        return result;
    }

    /**
     * 获取字符在Base62中的值
     */
    private static int getCharValue(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'A' && c <= 'Z') {
            return c - 'A' + 10;
        } else if (c >= 'a' && c <= 'z') {
            return c - 'a' + 36;
        } else {
            throw new IllegalArgumentException("Invalid Base62 character: " + c);
        }
    }

    /**
     * 验证短网址key格式
     *
     * @param key 待验证的key
     * @return 是否有效
     */
    public static boolean isValidShortKey(String key) {
        if (key == null || key.length() != TARGET_LENGTH) {
            return false;
        }

        for (char c : key.toCharArray()) {
            if (BASE62_CHARS.indexOf(c) == -1) {
                return false;
            }
        }

        return true;
    }

    /**
     * 生成指定长度的短网址key
     *
     * @param length 目标长度
     * @return 短网址key
     */
    public static String generateShortKey(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive");
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int index = ThreadLocalRandom.current().nextInt(BASE62_CHARS.length());
            result.append(BASE62_CHARS.charAt(index));
        }

        return result.toString();
    }

    /**
     * 生成多个不重复的短网址key
     *
     * @param count 生成数量
     * @return 短网址key集合
     */
    public static java.util.Set<String> generateUniqueKeys(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be positive");
        }

        java.util.Set<String> keys = new java.util.HashSet<>();

        while (keys.size() < count) {
            keys.add(generateShortKey());
        }

        return keys;
    }
}
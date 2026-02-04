package com.wut.shortlink.admin.toolkit;

import cn.hutool.core.util.RandomUtil;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class RandomGenerator {
    public static final String baseString = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * 生成包含数字和大小写字母的指定长度随机字符串。
     *
     * @return 生成的随机字符串
     */
    public static String generateRandomString(int len) {
        // 生成随机字符串
        return RandomUtil.randomString(baseString, len);
    }

    /**
     * 生成包含数字和大小写字母的6位随机字符串。
     *
     * @return 生成的随机字符串
     */
    public static String generateSixLenRandomString() {
        // 生成随机字符串
        return generateRandomString(6);
    }

}
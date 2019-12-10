package com.xingxunlei.email.verify;

import com.xingxunlei.email.verify.utils.VerifyUtils;

/**
 * 验证邮箱有效性测试类
 *
 * @author xingxunlei
 */
public class Main {

    public static void main(String[] args) {
        System.out.println(VerifyUtils.isEmailValid("abc@qq.com"));
    }

}

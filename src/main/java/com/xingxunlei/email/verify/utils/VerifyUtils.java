package com.xingxunlei.email.verify.utils;

import org.apache.commons.net.smtp.SMTPClient;
import org.apache.commons.net.smtp.SMTPReply;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

/**
 * 验证工具类
 *
 * @author xingxunlei
 */
@Slf4j
public class VerifyUtils {
    private static final String REGX_EMAIL = "[\\w\\.\\-]+@([\\w\\-]+\\.)+[\\w\\-]+";
    private static final int REPLAY_CODE_SUCCESS = 250;

    /**
     * 验证邮箱地址是否有效地址
     * @param recipient 接收人（需要验证的邮箱地址）
     * @return true or false
     */
    public static boolean isEmailValid(String recipient) {
        return isEmailValid(recipient, "abc@qq.com");
    }

    /**
     * 验证邮箱地址是否有效地址
     * @param recipient 接收人（需要验证的邮箱地址）
     * @param sender 发送人（正确无误的一个邮箱地址，用来作为SMTP的发送人）
     * @return true or false
     */
    public static boolean isEmailValid(String recipient, String sender) {
        if (!recipient.matches(REGX_EMAIL)) {
            log.error("邮箱（" + recipient + "）校验未通过，格式不对!");
            return false;
        }
        String hostName = recipient.split("@")[1];
        Record[] result;
        SMTPClient client = new SMTPClient();
        try {
            // 查找DNS缓存服务器上为MX类型的缓存域名信息
            Lookup lookup = new Lookup(hostName, Type.MX);
            lookup.run();
            if (lookup.getResult() != Lookup.SUCCESSFUL) {
                log.error("邮箱（" + recipient + "）校验未通过，未找到对应的MX记录!");
                return false;
            }
            result = lookup.getAnswers();

            //尝试和SMTP邮箱服务器建立Socket连接
            for (Record record : result) {
                String host = record.getAdditionalName().toString();
                log.info("SMTPClient try connect to host: " + host);

                //此connect()方法来自SMTPClient的父类:org.apache.commons.net.SocketClient
                //继承关系结构：org.apache.commons.net.smtp.SMTPClient-->org.apache.commons.net.smtp.SMTP-->org.apache.commons.net.SocketClient
                //Opens a Socket connected to a remote host at the current default port and
                //originating from the current host at a system assigned port. Before returning,
                //_connectAction_() is called to perform connection initialization actions.
                //尝试Socket连接到SMTP服务器
                client.connect(host);

                //Determine if a reply code is a positive completion response（查看响应码是否正常）.
                //All codes beginning with a 2 are positive completion responses（所有以2开头的响应码都是正常的响应）.
                //The SMTP server will send a positive completion response on the final successful completion of a command.
                if (SMTPReply.isPositiveCompletion(client.getReplyCode())) {
                    log.info("找到MX记录: " + hostName);
                    log.info("建立链接成功: " + hostName);
                    break;

                }

                //断开socket连接
                client.disconnect();
            }
            log.info("SMTPClient ReplyString: " + client.getReplyString());

            //尝试和SMTP服务器建立连接,发送一条消息给SMTP服务器
            String emailPrefix = sender.split("@")[0];
            client.login(emailPrefix);
            log.info("SMTPClient login: " + emailPrefix + " ...");
            log.info("SMTPClient ReplyString: " + client.getReplyString());

            //设置发送者，在设置接受者之前必须要先设置发送者
            String emailSuffix = "mail." + sender.split("@")[1];
            String fromEmail = emailPrefix + "@" + emailSuffix;
            client.setSender(fromEmail);
            log.info("设置发送者: " + fromEmail);
            log.info("SMTPClient ReplyString: " + client.getReplyString());

            //设置接收者,在设置接受者必须先设置发送者，否则SMTP服务器会拒绝你的命令
            client.addRecipient(recipient);
            log.info("设置接收者: " + recipient);
            log.info("SMTPClient ReplyString: " + client.getReplyString());
            log.info("SMTPClient ReplyCode: " + client.getReplyCode() + "(250表示正常)");

            return REPLAY_CODE_SUCCESS == client.getReplyCode();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                client.disconnect();
            } catch (IOException e) {
                log.error("SMTPClient disconnect error ", e);
            }
        }
    }
}

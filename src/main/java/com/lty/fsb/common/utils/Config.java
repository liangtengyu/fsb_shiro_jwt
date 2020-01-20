package com.lty.fsb.common.utils;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by Administrator on 2018/8/7.
 */
@Component
@ConfigurationProperties(prefix = "messageconfig")
public class Config {

    private String chuanglan_url;   //创蓝短信url
    private String chuanglan_account;  //创蓝API账号
    private String chuanglan_password; //创蓝API密码

    public String getChuanglan_url() {
        return chuanglan_url;
    }

    public void setChuanglan_url(String chuanglan_url) {
        this.chuanglan_url = chuanglan_url;
    }
    public String getChuanglan_account() {
        return chuanglan_account;
    }

    public void setChuanglan_account(String chuanglan_account) {
        this.chuanglan_account = chuanglan_account;
    }

    public String getChuanglan_password() {
        return chuanglan_password;
    }

    public void setChuanglan_password(String chuanglan_password) {
        this.chuanglan_password = chuanglan_password;
    }


}

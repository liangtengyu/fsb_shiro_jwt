package com.lty.fsb.web.controller;

import com.lty.fsb.common.domain.FsbConstant;
import com.lty.fsb.common.domain.FsbResponse;
import com.lty.fsb.common.exception.FsbGlobalException;
import com.lty.fsb.common.utils.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("article")
public class ArticleController {

    @GetMapping
    @RequiresPermissions("article:view")
    public FsbResponse queryArticle(String date) throws FsbGlobalException {
        String param;
        String data;
        try {
            if (StringUtils.isNotBlank(date)) {
                param = "dev=1&date=" + date;
                data = HttpUtil.sendSSLPost(FsbConstant.MRYW_DAY_URL, param);
            } else {
                param = "dev=1";
                data = HttpUtil.sendSSLPost(FsbConstant.MRYW_TODAY_URL, param);
            }
            return new FsbResponse().data(data);
        } catch (Exception e) {
            String message = "获取文章失败";
            log.error(message, e);
            throw new FsbGlobalException(message);
        }
    }
}

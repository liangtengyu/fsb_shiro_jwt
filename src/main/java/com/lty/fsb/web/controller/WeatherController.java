package com.lty.fsb.web.controller;

import com.lty.fsb.common.domain.FsbConstant;
import com.lty.fsb.common.domain.FsbResponse;
import com.lty.fsb.common.exception.FsbGlobalException;
import com.lty.fsb.common.utils.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;

@Slf4j
@Validated
@RestController
@RequestMapping("weather")
public class WeatherController {

    @GetMapping
    @RequiresPermissions("weather:view")
    public FsbResponse queryWeather(@NotBlank(message = "{required}") String areaId) throws FsbGlobalException {
        try {
            String data = HttpUtil.sendPost(FsbConstant.MEIZU_WEATHER_URL, "cityIds=" + areaId);
            return new FsbResponse().data(data);
        } catch (Exception e) {
            String message = "天气查询失败";
            log.error(message, e);
            throw new FsbGlobalException(message);
        }
    }
}

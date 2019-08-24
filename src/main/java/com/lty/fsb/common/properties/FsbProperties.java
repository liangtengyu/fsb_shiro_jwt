package com.lty.fsb.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "fsb")
public class FsbProperties {

    private ShiroProperties shiro = new ShiroProperties();

    private boolean openAopLog = true;

}

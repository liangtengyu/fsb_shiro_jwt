package com.lty.fsb.system.service;

import com.lty.fsb.system.domain.Test;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface TestService extends IService<Test> {

    List<Test> findTests();

    /**
     * 批量插入
     * @param list List<Test>
     */
    void batchInsert(List<Test> list);
}

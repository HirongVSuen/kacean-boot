package com.kacean.boot.common.web.controller;

import com.kacean.boot.common.app.domain.BaseDomain;
import com.kacean.boot.common.infra.curd.BaseCurd;
import com.kacean.boot.common.infra.entity.BaseEntity;
import com.kacean.boot.common.web.assembler.DddAssembler;
import com.kacean.boot.common.web.response.BaseDTO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * DDD 基础控制器：扩展 CURD 控制器，增加领域模型泛型与 DDD 转换器入口。
 * DDD 转换器由 Spring 按子类泛型进行 {@code @Autowired} 字段注入，子类无需声明构造器。
 *
 * @author kacean-agent
 * @param <DTO> DTO 类型
 * @param <DOMAIN> 领域模型类型
 * @param <ENTITY> 持久化对象类型
 * @param <CURD> 业务 CURD 类型
 */
public class DddController<DTO extends BaseDTO, DOMAIN extends BaseDomain, ENTITY extends BaseEntity,
        CURD extends BaseCurd<ENTITY, ?>> extends CurdController<DTO, ENTITY, CURD> {

    /**
     * DTO、领域模型与持久化对象转换器，由 Spring 按子类泛型注入。
     */
    @Autowired
    private DddAssembler<DTO, DOMAIN, ENTITY> assembler;

    /**
     * 提供 DDD 转换器供业务控制器扩展领域转换。
     *
     * @return DTO、领域模型与持久化对象转换器
     */
    @Override
    protected final DddAssembler<DTO, DOMAIN, ENTITY> assembler() {
        return assembler;
    }
}

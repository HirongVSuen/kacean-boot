package com.kacean.power.common.web.assembler;

/**
 * Web 层 MapStruct 转换契约：统一定义 DTO 与领域模型、持久化对象之间的双向转换。
 * 具体业务子接口使用 {@code @Mapper} 注解，由 MapStruct 生成实现。
 *
 * @author kacean-agent
 * @param <DTO> Web 层 DTO 类型
 * @param <DOMAIN> 领域模型类型
 * @param <DO> 持久化对象类型
 */
public interface BaseAssembler<DTO, DOMAIN, DO> {

    /**
     * 将 DTO 转换为领域模型。
     *
     * @param dto Web 层 DTO
     * @return 领域模型
     */
    DOMAIN toDomain(DTO dto);

    /**
     * 将 DTO 转换为持久化对象，供贫血模式下 Web 直接调用基础设施层时使用。
     *
     * @param dto Web 层 DTO
     * @return 持久化对象
     */
    DO toDo(DTO dto);

    /**
     * 将领域模型转换为 DTO。
     *
     * @param domain 领域模型
     * @return Web 层 DTO
     */
    DTO fromDomain(DOMAIN domain);

    /**
     * 将持久化对象转换为 DTO。
     *
     * @param dataObject 持久化对象
     * @return Web 层 DTO
     */
    DTO fromDo(DO dataObject);
}

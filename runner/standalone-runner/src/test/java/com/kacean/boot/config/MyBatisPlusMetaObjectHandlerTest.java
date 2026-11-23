package com.kacean.boot.config;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.kacean.boot.common.infra.entity.BaseEntity;
import com.kacean.boot.logincontext.contract.LoginContext;
import com.kacean.boot.logincontext.contract.LoginContextManager;
import com.kacean.boot.logincontext.contract.LoginContextUnavailableException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * 公共字段填充处理器测试，固定插入填充、更新覆盖策略与登录上下文不可用时的兜底行为。
 *
 * @author kacean-agent
 */
class MyBatisPlusMetaObjectHandlerTest {

    @BeforeAll
    static void 初始化测试实体表信息() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), AuditEntity.class);
    }

    @Test
    void 插入填充_登录用户名写入审计字段并生成公共默认值() {
        AuditEntity entity = new AuditEntity();
        MyBatisPlusMetaObjectHandler handler = new MyBatisPlusMetaObjectHandler(managerOf("operator"));

        handler.insertFill(SystemMetaObject.forObject(entity));

        assertAll(
                () -> assertDoesNotThrow(() -> UUID.fromString(entity.getExternalNo())),
                () -> assertEquals(0, entity.getDeleted()),
                () -> assertEquals(0L, entity.getVersion()),
                () -> assertNotNull(entity.getCreateAt()),
                () -> assertEquals(entity.getCreateAt(), entity.getModifyAt()),
                () -> assertEquals("operator", entity.getCreateBy()),
                () -> assertEquals("operator", entity.getModifyBy()));
    }

    @Test
    void 插入填充_调用方已设置的值不被覆盖() {
        AuditEntity entity = new AuditEntity();
        LocalDateTime presetTime = LocalDateTime.of(2024, 1, 1, 0, 0);
        entity.setExternalNo("biz-no-001");
        entity.setCreateAt(presetTime);
        entity.setCreateBy("preset-user");
        MyBatisPlusMetaObjectHandler handler = new MyBatisPlusMetaObjectHandler(managerOf("operator"));

        handler.insertFill(SystemMetaObject.forObject(entity));

        assertAll(
                () -> assertEquals("biz-no-001", entity.getExternalNo()),
                () -> assertEquals(presetTime, entity.getCreateAt()),
                () -> assertEquals("preset-user", entity.getCreateBy()));
    }

    @Test
    void 插入填充_登录上下文不可用时操作人回退为system() {
        AuditEntity entity = new AuditEntity();
        MyBatisPlusMetaObjectHandler handler = new MyBatisPlusMetaObjectHandler(unavailableManager());

        handler.insertFill(SystemMetaObject.forObject(entity));

        assertAll(
                () -> assertEquals("system", entity.getCreateBy()),
                () -> assertEquals("system", entity.getModifyBy()));
    }

    @Test
    void 更新填充_强制刷新修改审计字段且不动创建审计字段() {
        AuditEntity entity = new AuditEntity();
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 0, 0);
        entity.setCreateAt(createdAt);
        entity.setCreateBy("creator");
        entity.setModifyAt(createdAt);
        entity.setModifyBy("creator");
        MyBatisPlusMetaObjectHandler handler = new MyBatisPlusMetaObjectHandler(managerOf("operator"));

        handler.updateFill(SystemMetaObject.forObject(entity));

        assertAll(
                () -> assertEquals(createdAt, entity.getCreateAt()),
                () -> assertEquals("creator", entity.getCreateBy()),
                () -> assertNotEquals(createdAt, entity.getModifyAt()),
                () -> assertEquals("operator", entity.getModifyBy()));
    }

    @Test
    void 更新填充_登录上下文不可用时修改人回退为system() {
        AuditEntity entity = new AuditEntity();
        MyBatisPlusMetaObjectHandler handler = new MyBatisPlusMetaObjectHandler(unavailableManager());

        handler.updateFill(SystemMetaObject.forObject(entity));

        assertEquals("system", entity.getModifyBy());
    }

    private static LoginContextManager managerOf(String username) {
        return () -> {
            LoginContext context = new LoginContext();
            context.setUserId("user-1");
            context.setUsername(username);
            return context;
        };
    }

    private static LoginContextManager unavailableManager() {
        return () -> {
            throw new LoginContextUnavailableException();
        };
    }

    /** 继承公共持久化基类的测试实体，仅用于验证审计字段填充行为。 */
    @TableName("audit_fill_test_entity")
    static class AuditEntity extends BaseEntity {
    }
}

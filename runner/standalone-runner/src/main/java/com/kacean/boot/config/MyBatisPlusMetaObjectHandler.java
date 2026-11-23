package com.kacean.boot.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.kacean.boot.logincontext.contract.LoginContextManager;
import com.kacean.boot.logincontext.contract.LoginContextUnavailableException;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * MyBatis-Plus 公共字段填充处理器：插入时生成外部编号并初始化审计字段，更新时刷新修改审计字段。
 * <p>创建人、修改人取当前登录上下文中的登录用户名；定时任务、启动初始化等无登录上下文场景
 * 统一使用 {@value #SYSTEM_OPERATOR} 兜底，不因缺少身份信息阻断持久化操作。</p>
 * <p>插入填充不覆盖调用方已设置的值；更新填充强制刷新修改时间与修改人，
 * 避免先查询再更新时实体上残留的旧审计值被保留。</p>
 *
 * @author kacean-agent
 */
@Component
public class MyBatisPlusMetaObjectHandler implements MetaObjectHandler {

    /** 无登录上下文时使用的兜底操作人。 */
    private static final String SYSTEM_OPERATOR = "system";

    private final LoginContextManager loginContextManager;

    public MyBatisPlusMetaObjectHandler(LoginContextManager loginContextManager) {
        this.loginContextManager = loginContextManager;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        String operator = currentOperator();
        strictInsertFill(metaObject, "externalNo", String.class, UUID.randomUUID().toString());
        strictInsertFill(metaObject, "deleted", Integer.class, 0);
        strictInsertFill(metaObject, "version", Long.class, 0L);
        strictInsertFill(metaObject, "createAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "createBy", String.class, operator);
        strictInsertFill(metaObject, "modifyAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "modifyBy", String.class, operator);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        setFieldValByName("modifyAt", LocalDateTime.now(), metaObject);
        setFieldValByName("modifyBy", currentOperator(), metaObject);
    }

    /**
     * 获取当前操作人，登录上下文不可用时回退为系统操作人。
     *
     * @return 登录用户名或 {@value #SYSTEM_OPERATOR}
     */
    private String currentOperator() {
        try {
            return loginContextManager.getRequired().getUsername();
        } catch (LoginContextUnavailableException exception) {
            return SYSTEM_OPERATOR;
        }
    }
}

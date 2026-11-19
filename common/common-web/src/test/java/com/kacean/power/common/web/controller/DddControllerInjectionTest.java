package com.kacean.power.common.web.controller;

import com.kacean.power.common.app.domain.BaseDomain;
import com.kacean.power.common.infra.curd.BaseCurd;
import com.kacean.power.common.infra.dao.BaseDao;
import com.kacean.power.common.infra.dao.BaseDaoImpl;
import com.kacean.power.common.infra.entity.BaseEntity;
import com.kacean.power.common.infra.mapper.BaseMapper;
import com.kacean.power.common.web.assembler.DddAssembler;
import com.kacean.power.common.web.response.BaseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * DDD 控制器注入测试：在真实 Spring 容器中验证父类 {@code @Autowired} 字段按子类泛型完成注入，
 * 覆盖 DDD 控制器与父类 CURD 控制器的同名 assembler 字段同时注入的场景。
 *
 * @author kacean-agent
 */
class DddControllerInjectionTest {

    @Test
    void DDD控制器_容器按子类泛型注入全部父类字段() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(TestMapper.class, DddControllerInjectionTest::mapperProxy);
            context.registerBean(TestDaoImpl.class);
            context.registerBean(TestCurd.class);
            context.registerBean(TestAssembler.class, DddControllerInjectionTest::assemblerProxy);
            context.registerBean(TestDddController.class);
            context.refresh();

            TestDddController controller = context.getBean(TestDddController.class);
            assertSame(context.getBean(TestCurd.class), controller.curd());
            assertSame(context.getBean(TestAssembler.class), controller.assembler());
        }
    }

    private static TestMapper mapperProxy() {
        // 仅提供类型匹配的 Mapper bean，注入验证不执行任何 SQL
        return (TestMapper) Proxy.newProxyInstance(TestMapper.class.getClassLoader(),
                new Class<?>[]{TestMapper.class}, (proxy, method, args) -> null);
    }

    private static TestAssembler assemblerProxy() {
        return (TestAssembler) Proxy.newProxyInstance(TestAssembler.class.getClassLoader(),
                new Class<?>[]{TestAssembler.class}, (proxy, method, args) -> null);
    }

    private static final class TestDddController extends DddController<TestDTO, TestDomain, TestEntity, TestCurd> {
    }

    private static final class TestCurd extends BaseCurd<TestEntity, TestDao> {
    }

    private interface TestDao extends BaseDao<TestEntity> {
    }

    private static final class TestDaoImpl extends BaseDaoImpl<TestMapper, TestEntity> implements TestDao {
    }

    private interface TestMapper extends BaseMapper<TestEntity> {
    }

    private interface TestAssembler extends DddAssembler<TestDTO, TestDomain, TestEntity> {
    }

    private static final class TestDTO extends BaseDTO {
    }

    private static final class TestDomain implements BaseDomain {
    }

    private static final class TestEntity extends BaseEntity {

        private static final long serialVersionUID = 1L;
    }
}

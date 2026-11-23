package com.kacean.boot.common.infra.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * MyBatis-Plus 持久化对象基类测试，固定公共字段默认值、映射注解与相等性边界契约。
 *
 * @author kacean-agent
 */
class BaseEntityTest {

    @Test
    void 新建持久化对象_逻辑删除与版本号使用默认值() {
        TestEntity entity = new TestEntity();

        assertAll(
                () -> assertNull(entity.getId()),
                () -> assertNull(entity.getExternalNo()),
                () -> assertEquals(0, entity.getDeleted()),
                () -> assertEquals(0L, entity.getVersion()));
    }

    @Test
    void 不同持久化对象_公共字段相同也不因基类判定相等() {
        assertNotEquals(new TestEntity("A"), new TestEntity("B"));
    }

    @Test
    void 公共字段_MyBatisPlus注解与填充策略符合约定() throws NoSuchFieldException {
        Field idField = field("id");
        TableId tableId = idField.getAnnotation(TableId.class);
        assertNotNull(tableId);
        assertAll(
                () -> assertEquals(Long.class, idField.getType()),
                () -> assertEquals("id", tableId.value()),
                () -> assertEquals(IdType.ASSIGN_ID, tableId.type()));

        TableField externalNo = tableField("externalNo");
        assertEquals("external_no", externalNo.value());
        assertEquals(FieldFill.INSERT, externalNo.fill());

        Field deletedField = field("deleted");
        assertNotNull(deletedField.getAnnotation(TableLogic.class));
        assertTableField(deletedField, "deleted", FieldFill.INSERT);
        assertTableField(field("createAt"), "create_at", FieldFill.INSERT);
        assertTableField(field("createBy"), "create_by", FieldFill.INSERT);
        assertTableField(field("modifyAt"), "modify_at", FieldFill.INSERT_UPDATE);
        assertTableField(field("modifyBy"), "modify_by", FieldFill.INSERT_UPDATE);

        Field versionField = field("version");
        assertNotNull(versionField.getAnnotation(Version.class));
        assertTableField(versionField, "version", FieldFill.INSERT);
    }

    private static Field field(String name) throws NoSuchFieldException {
        return BaseEntity.class.getDeclaredField(name);
    }

    private static TableField tableField(String name) throws NoSuchFieldException {
        TableField annotation = field(name).getAnnotation(TableField.class);
        assertNotNull(annotation);
        return annotation;
    }

    private static void assertTableField(Field field, String column, FieldFill fill) {
        TableField annotation = field.getAnnotation(TableField.class);
        assertNotNull(annotation);
        assertAll(
                () -> assertEquals(column, annotation.value()),
                () -> assertEquals(fill, annotation.fill()));
    }

    private static final class TestEntity extends BaseEntity {

        private static final long serialVersionUID = 1L;

        private final String name;

        private TestEntity() {
            this(null);
        }

        private TestEntity(String name) {
            this.name = name;
        }
    }
}

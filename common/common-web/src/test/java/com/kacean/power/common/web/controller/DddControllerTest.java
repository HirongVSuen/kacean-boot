package com.kacean.power.common.web.controller;

import com.kacean.power.common.web.assembler.CurdAssembler;
import com.kacean.power.common.web.assembler.DddAssembler;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DDD 控制器继承测试：确保通用端点只声明一次，DDD 层只协变扩展转换器。
 *
 * @author kacean-agent
 */
class DddControllerTest {

    private static final List<Class<? extends Annotation>> MAPPING_ANNOTATIONS = List.of(
            RequestMapping.class, GetMapping.class, PostMapping.class,
            PutMapping.class, PatchMapping.class, DeleteMapping.class);

    @Test
    void DDD控制器继承CURD控制器_通用端点只在父类声明() {
        assertEquals(CurdController.class, DddController.class.getSuperclass());
        assertTrue(CurdAssembler.class.isAssignableFrom(DddAssembler.class));
        assertTrue(Arrays.stream(CurdController.class.getDeclaredMethods())
                .anyMatch(DddControllerTest::hasMapping));
        assertFalse(Arrays.stream(DddController.class.getDeclaredMethods())
                .anyMatch(DddControllerTest::hasMapping));
    }

    @Test
    void DDD控制器协变返回DDD转换器() throws NoSuchMethodException {
        Method assembler = DddController.class.getDeclaredMethod("assembler");

        assertEquals(DddAssembler.class, assembler.getReturnType());
        assertTrue(Modifier.isFinal(assembler.getModifiers()));
    }

    private static boolean hasMapping(Method method) {
        return MAPPING_ANNOTATIONS.stream().anyMatch(method::isAnnotationPresent);
    }
}

package com.kacean.boot.datadict.port.web.controller;

import com.kacean.boot.datadict.port.infra.curd.DataDictValueCurd;
import com.kacean.boot.datadict.port.infra.entity.DataDictValueEntity;
import com.kacean.boot.datadict.port.web.dto.DataDictValueDTO;
import com.kacean.boot.common.web.controller.CurdController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据字典值表控制器。
 *
 * @author kacean-agent
 */
@RestController
@RequestMapping("/data-dict-value")
public class DataDictValueController extends CurdController<DataDictValueDTO, DataDictValueEntity, DataDictValueCurd> {
}

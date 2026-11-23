package com.kacean.boot.datadict.port.web.controller;

import com.kacean.boot.datadict.port.infra.curd.DataDictCurd;
import com.kacean.boot.datadict.port.infra.entity.DataDictEntity;
import com.kacean.boot.datadict.port.web.dto.DataDictDTO;
import com.kacean.boot.common.web.controller.CurdController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据字典表控制器。
 *
 * @author kacean-agent
 */
@RestController
@RequestMapping("/data-dict")
public class DataDictController extends CurdController<DataDictDTO, DataDictEntity, DataDictCurd> {
}

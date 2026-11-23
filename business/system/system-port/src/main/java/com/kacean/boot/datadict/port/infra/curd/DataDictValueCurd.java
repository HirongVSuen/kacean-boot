package com.kacean.boot.datadict.port.infra.curd;

import com.kacean.boot.datadict.port.infra.dao.DataDictValueDao;
import com.kacean.boot.datadict.port.infra.entity.DataDictValueEntity;
import com.kacean.boot.common.infra.curd.BaseCurd;
import org.springframework.stereotype.Component;

/**
 * 数据字典值表通用读写用例。
 *
 * @author kacean-agent
 */
@Component
public class DataDictValueCurd extends BaseCurd<DataDictValueEntity, DataDictValueDao> {
}

package com.kacean.boot.datadict.port.infra.curd;

import com.kacean.boot.datadict.port.infra.dao.DataDictDao;
import com.kacean.boot.datadict.port.infra.entity.DataDictEntity;
import com.kacean.boot.common.infra.curd.BaseCurd;
import org.springframework.stereotype.Component;

/**
 * 数据字典表通用读写用例。
 *
 * @author kacean-agent
 */
@Component
public class DataDictCurd extends BaseCurd<DataDictEntity, DataDictDao> {
}

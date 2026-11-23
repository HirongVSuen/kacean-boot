package com.kacean.boot.common.app.repo;

import com.kacean.boot.common.app.identity.ID;
import com.kacean.boot.common.app.identity.IDCollection;

import java.util.Collection;
import java.util.List;

/**
 * 基础仓储接口：只定义聚合真正通用的最小读写能力。
 * 分页查询由具体业务仓储（如 TicketTypeRepository）按自身查询条件声明，不在此统一；
 * getRequired... 等「必须存在」的语义由应用服务实现，不放进仓储；
 * existsById 等其余操作待真实需求出现后再扩展。
 *
 * @author kacean-agent
 * @param <D> 聚合类型
 */
public interface BaseRepository<D> {

    /**
     * 按标识查询聚合，允许不存在。
     *
     * @param id 聚合标识，非空
     * @return 聚合，不存在时返回 {@code null}
     */
    D findById(ID id);

    /**
     * 按标识集合批量查询聚合，允许部分标识不存在。
     * 主键与外部编号条件取并集，不按位置对应；重复标识由 {@link IDCollection} 去重。
     *
     * @param ids 聚合标识集合，非空
     * @return 命中的聚合列表，不保证与标识顺序一致，未命中的标识不出现在结果中
     */
    List<D> findByIds(IDCollection ids);

    /**
     * 保存聚合，新增与更新的统一入口，由实现依据标识与版本号区分。
     *
     * @param aggregate 待保存的聚合，非空
     * @return 保存后的聚合，带回生成的主键与新版本号
     */
    D save(D aggregate);

    /**
     * 批量保存聚合，新增与更新的统一入口，语义与逐条调用 {@link #save(Object)} 一致。
     * 不默认保证原子性：失败前可能已有部分聚合保存成功；需要整体提交或回滚时，由调用方建立事务边界。
     *
     * @param aggregates 待保存的聚合集合，非空且不得包含 {@code null}
     *                   空集合不执行任何操作
     * @return 保存后的聚合列表，与入参顺序一致，带回生成的主键与新版本号
     */
    List<D> saveAll(Collection<D> aggregates);

    /**
     * 按标识删除聚合，物理删除或逻辑删除由实现决定。
     *
     * @param id 聚合标识，非空；标识不存在时不视为错误
     */
    void removeById(ID id);

    /**
     * 按标识集合批量删除聚合，主键与外部编号条件取并集，不按位置对应；重复标识由 {@link IDCollection} 去重。
     * 不默认保证原子性：失败前可能已有部分聚合删除成功；需要整体提交或回滚时，由调用方建立事务边界。
     *
     * @param ids 聚合标识集合，非空
     */
    void removeByIds(IDCollection ids);
}

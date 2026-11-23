package com.kacean.boot.common.web.controller;

import com.kacean.boot.common.app.identity.ID;
import com.kacean.boot.common.app.identity.IDCollection;
import com.kacean.boot.common.app.query.PageQuery;
import com.kacean.boot.common.app.query.PageResult;
import com.kacean.boot.common.app.query.Query;
import com.kacean.boot.common.infra.curd.BaseCurd;
import com.kacean.boot.common.infra.entity.BaseEntity;
import com.kacean.boot.common.web.assembler.CurdAssembler;
import com.kacean.boot.common.web.response.BaseDTO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * CURD 基础控制器：只依赖 DTO 与持久化对象，提供通用 CRUD 端点。
 * 转换器与业务 CURD 由 Spring 按子类泛型进行 {@code @Autowired} 字段注入，子类无需声明构造器。
 * 纯主键操作沿用 MyBatis-Plus 命名（如 {@code getById}、{@code removeByIds}）；
 * 支持「主键 id 或外部编号 code」的扩展操作以 {@code ByIdOrCode} / {@code ByIdsOrCodes} 结尾，
 * 入参为 {@link ID} / {@link IDCollection}，方法名与 URL 保持一致。
 *
 * @author kacean-agent
 * @param <DTO> DTO 类型
 * @param <ENTITY> 持久化对象类型
 * @param <CURD> 业务 CURD 类型
 */
public class CurdController<DTO extends BaseDTO, ENTITY extends BaseEntity,
        CURD extends BaseCurd<ENTITY, ?>> {

    /**
     * DTO 与持久化对象转换器，由 Spring 按子类泛型注入。
     */
    @Autowired
    private CurdAssembler<DTO, ENTITY> assembler;

    /**
     * 业务 CURD，由 Spring 按子类泛型注入。
     */
    @Autowired
    private CURD curd;

    /**
     * 提供转换器供业务控制器扩展特有端点。
     *
     * @return DTO 与持久化对象转换器
     */
    protected CurdAssembler<DTO, ENTITY> assembler() {
        return assembler;
    }

    /**
     * 提供业务 CURD 供业务控制器扩展特有端点。
     *
     * @return 业务 CURD
     */
    protected final CURD curd() {
        return curd;
    }

    /**
     * 保存单条记录
     *
     * @param dto DTO对象
     * @return 保存结果
     */
    @Operation(summary = "保存")
    @PostMapping("/save")
    public boolean save(@RequestBody DTO dto) {
        return curd.save(assembler.toEntity(dto));
    }

    /**
     * 批量保存
     *
     * @param dtoList DTO对象集合
     * @return 保存结果
     */
    @Operation(summary = "批量保存")
    @PostMapping("/saveBatch")
    public boolean saveBatch(@RequestBody List<DTO> dtoList) {
        return curd.saveBatch(assembler.toEntity(dtoList));
    }

    /**
     * 保存或更新：按主键判断，主键存在则更新，否则插入
     *
     * @param dto DTO对象
     * @return 保存或更新结果
     */
    @Operation(summary = "保存或更新")
    @PostMapping("/saveOrUpdate")
    public boolean saveOrUpdate(@RequestBody DTO dto) {
        return curd.saveOrUpdate(assembler.toEntity(dto));
    }

    /**
     * 批量保存或更新：逐条按主键判断更新或插入
     *
     * @param dtoList DTO对象集合
     * @return 保存或更新结果
     */
    @Operation(summary = "批量保存或更新")
    @PostMapping("/saveOrUpdateBatch")
    public boolean saveOrUpdateBatch(@RequestBody List<DTO> dtoList) {
        return curd.saveOrUpdateBatch(assembler.toEntity(dtoList));
    }

    /**
     * 根据主键删除
     *
     * @param id 主键ID
     * @return 删除结果
     */
    @Operation(summary = "根据主键删除")
    @PostMapping("/removeById")
    public boolean removeById(@RequestParam("id") Serializable id) {
        return curd.removeById(id);
    }

    /**
     * 根据主键或外部编号删除，二者在 {@link ID} 中必须且只能提供一个
     *
     * @param id 主键ID或外部编号
     * @return 删除结果
     */
    @Operation(summary = "根据主键或外部编号删除")
    @PostMapping("/removeByIdOrCode")
    public boolean removeByIdOrCode(@RequestBody ID id) {
        return curd.removeById(id);
    }

    /**
     * 根据主键集合批量删除
     *
     * @param list 主键ID或实体列表
     * @return 删除结果
     */
    @Operation(summary = "根据主键集合批量删除")
    @PostMapping("/removeByIds")
    public boolean removeByIds(@RequestBody Collection<?> list) {
        return curd.removeByIds(list);
    }

    /**
     * 根据主键或外部编号集合批量删除，两类标识为独立条件，删除范围取并集
     *
     * @param idCollection 主键ID与外部编号集合
     * @return 删除结果
     */
    @Operation(summary = "根据主键或外部编号集合批量删除")
    @PostMapping("/removeByIdsOrCodes")
    public boolean removeByIdsOrCodes(@RequestBody IDCollection idCollection) {
        return curd.removeById(idCollection);
    }

    /**
     * 根据主键查询
     *
     * @param id 主键ID
     * @return DTO对象，不存在时返回 {@code null}
     */
    @Operation(summary = "根据主键查询")
    @GetMapping("/getById")
    public DTO getById(@RequestParam("id") Serializable id) {
        return assembler.fromEntity(curd.getById(id));
    }

    /**
     * 根据主键或外部编号查询，二者在 {@link ID} 中必须且只能提供一个
     *
     * @param id 主键ID或外部编号
     * @return DTO对象，不存在时返回 {@code null}
     */
    @Operation(summary = "根据主键或外部编号查询")
    @GetMapping("/findByIdOrCode")
    public DTO findByIdOrCode(ID id) {
        return assembler.fromEntity(curd.findById(id));
    }

    /**
     * 根据主键集合批量查询
     *
     * @param idList 主键ID列表
     * @return DTO对象集合
     */
    @Operation(summary = "根据主键集合查询")
    @GetMapping("/listByIds")
    public List<DTO> listByIds(@RequestParam("idList") Collection<? extends Serializable> idList) {
        return assembler.fromEntity(curd.listByIds(idList));
    }

    /**
     * 根据主键或外部编号集合批量查询，两类标识为独立条件，查询结果取并集
     *
     * @param idCollection 主键ID与外部编号集合
     * @return DTO对象集合
     */
    @Operation(summary = "根据主键或外部编号集合批量查询")
    @GetMapping("/findByIdsOrCodes")
    public List<DTO> findByIdsOrCodes(IDCollection idCollection) {
        return assembler.fromEntity(curd.findByIds(idCollection));
    }

    /**
     * 查询所有记录
     *
     * @return DTO对象集合
     */
    @Operation(summary = "查询所有")
    @GetMapping("/list")
    public List<DTO> list() {
        return assembler.fromEntity(curd.list());
    }

    /**
     * 根据查询条件查询列表
     *
     * @param query 查询条件
     * @return DTO对象集合
     */
    @Operation(summary = "根据条件查询列表")
    @PostMapping("/listByQuery")
    public List<DTO> listByQuery(@RequestBody Query query) {
        return assembler.fromEntity(curd.list(query));
    }

    /**
     * 分页查询
     *
     * @param pageQuery 分页查询对象
     * @return DTO分页结果
     */
    @Operation(summary = "分页查询")
    @PostMapping("/page")
    public PageResult<DTO> page(@RequestBody PageQuery<? extends Query> pageQuery) {
        PageResult<ENTITY> page = curd.page(pageQuery);
        return assembler.fromEntity(page);
    }

    /**
     * 查询总记录数
     *
     * @return 总记录数
     */
    @Operation(summary = "查询总记录数")
    @GetMapping("/count")
    public long count() {
        return curd.count();
    }

}

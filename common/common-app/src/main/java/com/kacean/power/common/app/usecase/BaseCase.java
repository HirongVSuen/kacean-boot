package com.kacean.power.common.app.usecase;

import com.kacean.power.common.app.domain.BaseDomain;
import com.kacean.power.common.app.repo.BaseRepository;

/**
 * 基础用例
 *
 * @author hirong
 */
public class BaseCase<D extends BaseDomain> {

    /**
     * 仓库
     */
    private final BaseRepository<D> repository;

    /**
     * 构造函数
     *
     * @param repository 仓库
     */
    protected  BaseCase(BaseRepository<D> repository) {
        this.repository = repository;
    }

    /**
     * 获取仓库
     *
     * @return 仓库
     */
    protected BaseRepository<D> getRepository() {
        return repository;
    }
}

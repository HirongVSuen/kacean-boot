package com.kacean.boot.config;

import com.kacean.boot.logincontext.contract.LoginContext;
import com.kacean.boot.logincontext.contract.LoginContextManager;
import com.kacean.boot.logincontext.contract.LoginContextProvider;
import com.kacean.boot.logincontext.contract.LoginContextUnavailableException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Supplier;

/**
 * 登录管理器，创建统一登录上下文并按顺序调用各个提供者完成填充。
 * <p>Spring Bean 在获取上下文时按容器排序结果延迟解析，避免提前实例化依赖持久化设施的提供者；
 * Java SPI 按服务配置文件中的声明顺序执行。</p>
 *
 * @author kacean-agent
 */
@Service
public class LoginManager implements LoginContextManager {

    private final Supplier<List<LoginContextProvider>> providerSource;

    /**
     * 使用 Spring 容器中的登录上下文提供者，允许容器暂未注册任何提供者。
     * 提供者延迟到获取上下文时解析，避免持久化基础设施初始化期间形成循环依赖。
     *
     * @param providers Spring 管理的登录上下文提供者
     */
    @Autowired
    public LoginManager(ObjectProvider<LoginContextProvider> providers) {
        this.providerSource = () -> providers.orderedStream().toList();
    }

    LoginManager(Collection<LoginContextProvider> providers) {
        List<LoginContextProvider> providerSnapshot = List.copyOf(providers);
        this.providerSource = () -> providerSnapshot;
    }

    /**
     * 通过 Java SPI 加载登录上下文提供者并创建登录管理器。
     *
     * @return 使用 SPI 提供者的登录管理器
     */
    public static LoginManager fromServiceLoader() {
        List<LoginContextProvider> providers = ServiceLoader.load(LoginContextProvider.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .toList();
        return new LoginManager(providers);
    }

    @Override
    public LoginContext getRequired() {
        LoginContext context = new LoginContext();
        for (LoginContextProvider provider : providerSource.get()) {
            provider.fill(context);
        }
        if (isBlank(context.getUserId()) || isBlank(context.getUsername())) {
            throw new LoginContextUnavailableException();
        }
        return context;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

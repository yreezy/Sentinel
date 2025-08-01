package com.alibaba.csp.sentinel.datasource.kube;

/**
 * 定义 KubeConfigService 接口，用于处理 Kubernetes 配置的获取、发布和监听等操作。
 *
 * @author charles
 * @since 2025/08/01
 */
public interface KubeConfigService {
    String getConfig(String var1, String var2, long var3);

    String getConfigAndSignListener(String var1, String var2, long var3);

    void addListener(String var1, String var2, KubeListener var3);

    boolean publishConfig(String var1, String var2, String var3);

    boolean publishConfig(String var1, String var2, String var3, String var4);

    boolean removeConfig(String var1, String var2);

    void removeListener(String var1, String var2, KubeListener var3);

    String getServerStatus();

    void shutDown();
}

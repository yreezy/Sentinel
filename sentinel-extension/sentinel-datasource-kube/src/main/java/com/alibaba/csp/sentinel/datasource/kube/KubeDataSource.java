package com.alibaba.csp.sentinel.datasource.kube;

import com.alibaba.csp.sentinel.concurrent.NamedThreadFactory;
import com.alibaba.csp.sentinel.datasource.AbstractDataSource;
import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.alibaba.csp.sentinel.util.StringUtil;

import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A read-only {@code DataSource} with kube configmap. When the data in Kube configmap has been modified,
 *
 * @author Charles Yao
 * @since 1.8.8.0
 */
public class KubeDataSource<T> extends AbstractDataSource<String, T> {

    private static final int DEFAULT_TIMEOUT = 3000;

    /**
     * Single-thread pool. Once the thread pool is blocked, we throw up the old task.
     */
    private final ExecutorService pool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1), new NamedThreadFactory("sentinel-Kube-ds-update", true),
            new ThreadPoolExecutor.DiscardOldestPolicy());

    private final String groupId;
    private final String dataId;
    private final Properties properties;
    private final KubeListener configListener;


    /**
     * Note: The Kube config might be null if its initialization failed.
     */
    private KubeConfigService kubeConfigService = null;

    /**
     * Constructs a read-only DataSource with Kube backend.
     *
     * @param serverAddr server address of Kube, cannot be empty
     * @param groupId    group ID, cannot be empty
     * @param dataId     data ID, cannot be empty
     * @param parser     customized data parser, cannot be empty
     */
    public KubeDataSource(final String serverAddr, final String groupId, final String dataId,
                          Converter<String, T> parser) {
        this(KubeDataSource.buildProperties(serverAddr), groupId, dataId, parser);
    }

    /**
     * @param properties properties for construct {@link KubeConfigService}
     * @param groupId    group ID, cannot be empty
     * @param dataId     data ID, cannot be empty
     * @param parser     customized data parser, cannot be empty
     */
    public KubeDataSource(final Properties properties, final String groupId, final String dataId,
                          Converter<String, T> parser) {
        super(parser);
        if (StringUtil.isBlank(groupId) || StringUtil.isBlank(dataId)) {
            throw new IllegalArgumentException(String.format("Bad argument: groupId=[%s], dataId=[%s]",
                    groupId, dataId));
        }
        AssertUtil.notNull(properties, "Kube properties must not be null, you could put some keys from PropertyKeyConst");
        this.groupId = groupId;
        this.dataId = dataId;
        this.properties = properties;

        this.configListener = new KubeListener();
        initKubeListener();
        loadInitialConfig();
    }

    private void loadInitialConfig() {
        try {
            T newValue = loadConfig();
            if (newValue == null) {
                RecordLog.warn("[KubeDataSource] WARN: initial config is null, you may have to check your data source");
            }
            getProperty().updateValue(newValue);
        } catch (Exception ex) {
            RecordLog.warn("[KubeDataSource] Error when loading initial config", ex);
        }
    }

    private void initKubeListener() {
        try {
            kubeConfigService.addListener(dataId, groupId, configListener);
        } catch (Exception e) {
            RecordLog.warn("[NacosDataSource] Error occurred when initializing Nacos data source", e);
            e.printStackTrace();
        }
    }

    @Override
    public String readSource() throws Exception {
        if (kubeConfigService == null) {
            throw new IllegalStateException("Kube config service has not been initialized or error occurred");
        }
        return kubeConfigService.getConfig(dataId, groupId, DEFAULT_TIMEOUT);
    }

    @Override
    public void close() {
        if (kubeConfigService != null) {
            kubeConfigService.removeListener(dataId, groupId, configListener);
            try {
                kubeConfigService.shutDown();
            } catch (Exception e) {
                RecordLog.warn("[KubeDataSource] Error occurred when closing Kube data source", e);
                e.printStackTrace();
            }
        }
        pool.shutdownNow();
    }

    private static Properties buildProperties(String serverAddr) {
        Properties properties = new Properties();
        return properties;
    }
}

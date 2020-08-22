package com.example.shardingsphere.config;


import com.alibaba.druid.pool.DruidDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.shardingsphere.api.config.sharding.KeyGeneratorConfiguration;
import org.apache.shardingsphere.api.config.sharding.ShardingRuleConfiguration;
import org.apache.shardingsphere.api.config.sharding.TableRuleConfiguration;
import org.apache.shardingsphere.api.config.sharding.strategy.InlineShardingStrategyConfiguration;
import org.apache.shardingsphere.api.config.sharding.strategy.StandardShardingStrategyConfiguration;
import org.apache.shardingsphere.shardingjdbc.api.ShardingDataSourceFactory;
import org.apache.shardingsphere.shardingjdbc.spring.boot.masterslave.SpringBootMasterSlaveRuleConfigurationProperties;
import org.apache.shardingsphere.shardingjdbc.spring.boot.sharding.SpringBootShardingRuleConfigurationProperties;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;


@Configuration
@AutoConfigureAfter({CommonConfig.class})
//@EnableConfigurationProperties({SpringBootShardingRuleConfigurationProperties.class, SpringBootMasterSlaveRuleConfigurationProperties.class})
@MapperScan(basePackages = ShardingDataSourceConfig.PACKAGE, sqlSessionFactoryRef = "shardingSqlSessionFactory")
public class ShardingDataSourceConfig {

    static final String PACKAGE = "com.example.shardingsphere.dao.sharding";
    static final String MAPPER_LOCATION = "classpath:mapper/sharding/*.xml";
//    @Autowired
//    private SpringBootShardingRuleConfigurationProperties shardingProperties;

//    @Value("${spring.datasource.sharding.url}")
//    private String url;
//
//    @Value("${spring.datasource.sharding.username}")
//    private String username;
//
//    @Value("${spring.datasource.sharding.password}")
//    private String password;
//
//    @Value("${spring.datasource.sharding.driver-class-name}")
//    private String driverClass;

    @Bean(name = "shardingDataSource")
    public DataSource shardingDataSource() throws SQLException {
        // 配置真实数据源
        Map<String, DataSource> dataSourceMap = new HashMap<>();

        // 配置第一个数据源
        DruidDataSource dataSource1 = new DruidDataSource();
        dataSource1.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource1.setUrl("jdbc:mysql://49.233.87.32:3306/sharding_1?characterEncoding=utf-8");
        dataSource1.setUsername("root");
        dataSource1.setPassword("yzsydm");
        dataSourceMap.put("ds1", dataSource1);

        // 配置第二个数据源
        DruidDataSource dataSource2 = new DruidDataSource();
        dataSource2.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource2.setUrl("jdbc:mysql://49.233.87.32:3306/sharding_2?characterEncoding=utf-8");
        dataSource2.setUsername("root");
        dataSource2.setPassword("yzsydm");
        dataSourceMap.put("ds2", dataSource2);

        // 配置库表规则
        TableRuleConfiguration orderTableRuleConfig = new TableRuleConfiguration("my_sharding","ds$->{1..2}.my_sharding_$->{1..2}");

        // 配置分库 + 分表策略 + 分布式主键
        orderTableRuleConfig.setDatabaseShardingStrategyConfig(new InlineShardingStrategyConfiguration("user_id", "ds$->{user_id % 2 + 1}"));
        orderTableRuleConfig.setTableShardingStrategyConfig(new InlineShardingStrategyConfiguration("order_id", "my_sharding_$->{order_id % 2 + 1}"));
        orderTableRuleConfig.setKeyGeneratorConfig(new KeyGeneratorConfiguration("SNOWFLAKE", "order_id"));
        // 配置分片规则
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();
        shardingRuleConfig.getTableRuleConfigs().add(orderTableRuleConfig);
        // 省略配置order_item表规则...
        // ...

        // 获取数据源对象
        DataSource dataSource = ShardingDataSourceFactory.createDataSource(dataSourceMap, shardingRuleConfig, getProperties());
        return dataSource;
    }
    /**
     * 系统参数配置
     *
     * @return
     */
    private Properties getProperties() {
        Properties shardingProperties = new Properties();
        shardingProperties.put("sql.show", true);
        return shardingProperties;
    }
    @Bean(name = "shardingTransactionManager")
    public DataSourceTransactionManager shardingTransactionManager() throws SQLException {
        return new DataSourceTransactionManager(shardingDataSource());
    }

    @Bean(name = "shardingSqlSessionFactory")
    public SqlSessionFactory shardingSqlSessionFactory(@Qualifier("shardingDataSource") DataSource shardingDataSource)
            throws Exception {
        final SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(shardingDataSource);
        sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources(ShardingDataSourceConfig.MAPPER_LOCATION));
        return sessionFactory.getObject();
    }

//    DataSource getShardingDataSource() {
//        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();
//        shardingRuleConfig.getTableRuleConfigs().add(getOrderTableRuleConfiguration());
//        shardingRuleConfig.getBindingTableGroups().add("order_all");
//        //shardingRuleConfig.setDefaultDatabaseShardingStrategyConfig(new StandardShardingStrategyConfiguration("order_id", new ShardingTableStrategy()));
//        shardingRuleConfig.setDefaultTableShardingStrategyConfig(new StandardShardingStrategyConfiguration("order_id", new ShardingTableStrategy(),new RangeShardingTableStrategy()));
//        try {
//            return ShardingDataSourceFactory.createDataSource(createDataSourceMap(), shardingRuleConfig, new Properties());
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

//    private static KeyGeneratorConfiguration getKeyGeneratorConfiguration() {
//        KeyGeneratorConfiguration result = new KeyGeneratorConfiguration("SNOWFLAKE", "order_id");
//        return result;
//    }
//
//    TableRuleConfiguration getOrderTableRuleConfiguration() {
//        TableRuleConfiguration result = new TableRuleConfiguration("my_sharding", "order.order_all_${0..15}");
//        result.setKeyGeneratorConfig(shardingProperties.getDefaultKeyGenerator());
//        return result;
//    }
//
//    @ConfigurationProperties(prefix = "spring.shardingsphere.datasource.order")
//    Map<String, DataSource> createDataSourceMap() {
//        Map<String, DataSource> result = new HashMap<>();
//        // 配置真实数据源
//        Map<String, Object> dataSourceProperties = new HashMap<>();
//        dataSourceProperties.put("DriverClassName", driverClass);
//        dataSourceProperties.put("jdbcUrl", url);
//        dataSourceProperties.put("username", username);
//        dataSourceProperties.put("password", password);
//        try {
//            DataSource ds = DataSourceUtil.getDataSource("com.alibaba.druid.pool.DruidDataSource", dataSourceProperties);
//            result.put("order", ds);
//        } catch (ReflectiveOperationException e) {
//            e.printStackTrace();
//        }
//        return result;
//    }
//////////////////////////////////////////////////////////////////////////////////////////
//    @Bean(name = "orderdetaildatasource")
//    @ConfigurationProperties(prefix = "mybusiness.datasource.orderdetail")
//    public DataSource orderDetailDatasource()  {
//        return DataSourceBuilder.create().build();
//    }
//
//    /**
//     * 以mybusinessorderdetail为key，dataSource为value创建map
//     * 参照 io.shardingsphere.shardingjdbc.spring.boot.SpringBootConfiguration
//     * 这一步目的是将原始DataSource和我们在配置文件中的分片策略对应起来
//     * 注意key要和shardingJdbc配置项中的一致
//     * @return
//     * @throws SQLException
//     */
//    @Bean(name = "shardingDataSource")
//    public DataSource shardingDataSource() throws SQLException {
//        Map<String,DataSource> dataSourceMap = new LinkedHashMap<>();
//        dataSourceMap.put("mybusinessorderdetail", orderDetailDatasource());
//        return ShardingDataSourceFactory.createDataSource(dataSourceMap, shardingProperties..getShardingRuleConfiguration(), shardingProperties.getConfigMap(), shardingProperties.getProps());}
//
//    @Bean(name = "orderDetailDatasourceTransactionManager")
//    public DataSourceTransactionManager orderDetailDatasourceTransactionManager(){
//        return new DataSourceTransactionManager(orderDetailDatasource());
//    }
//
//    /**
//     * 将shardingJdbc创建的DataSource传入这里的SqlSessionFactory
//     * @return
//     * @throws Exception
//     */
//    @Bean(name = "orderDetailSqlSessionFactory")
//    public SqlSessionFactory orderDetailSqlSessionFactory() throws Exception {
//        SqlSessionFactoryBean sqlSessionFactory = new SqlSessionFactoryBean();
//        sqlSessionFactory.setDataSource(shardingDataSource());
//        sqlSessionFactory.setMapperLocations(resolveMapperLocations(environment.getProperty("mybusiness.orderdetail.mapper-locations").split(",")));
//        GlobalConfig globalConfig = new GlobalConfig();
//        globalConfig.setDbConfig(new GlobalConfig.DbConfig().setDbType(DbType.MYSQL));
//        sqlSessionFactory.setGlobalConfig(globalConfig);
//        return sqlSessionFactory.getObject();
//    }

}
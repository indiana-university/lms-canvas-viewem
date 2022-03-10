package edu.iu.uits.lms.viewem.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        entityManagerFactoryRef = "viewemEntityMgrFactory",
        transactionManagerRef = "viewemTransactionMgr",
        basePackages = {"edu.iu.uits.lms.viewem.repository"})
@EnableTransactionManagement
public class PostgresDBConfig {

    @Bean(name = "viewemDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    @Primary
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "viewemEntityMgrFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean viewemEntityMgrFactory(
            final EntityManagerFactoryBuilder builder,
            @Qualifier("viewemDataSource") final DataSource dataSource) {
        // dynamically setting up the hibernate properties for each of the datasource.
        final Map<String, String> properties = new HashMap<>();
        return builder
                .dataSource(dataSource)
                .properties(properties)
                .packages("edu.iu.uits.lms.viewem.model")
                .build();
    }

    @Bean(name = "viewemTransactionMgr")
    @Primary
    public PlatformTransactionManager viewemTransactionMgr(
            @Qualifier("viewemEntityMgrFactory") final EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}

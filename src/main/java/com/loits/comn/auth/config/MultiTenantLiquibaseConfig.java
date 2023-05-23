package com.loits.comn.auth.config;

import com.loits.comn.auth.mt.MultiTenantDataSources;
import com.loits.comn.auth.mt.MultiTenantLiquibase;
import com.loits.comn.auth.mt.MultiTenantProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;

/**
 * @author amal
 * @since 22/01/2019 4:19 PM
 */
@Configuration
@ConditionalOnProperty(prefix = "tenant", name = "liquibase-enabled", matchIfMissing = true)
@AutoConfigureAfter
@EnableConfigurationProperties({ MultiTenantProperties.class, LiquibaseProperties.class })
public class MultiTenantLiquibaseConfig {

	private MultiTenantProperties multiTenantProperties;

	private LiquibaseProperties liquibaseProperties;

	private MultiTenantDataSources multiTenantDataSources;

	private ResourceLoader resourceLoader;

	public MultiTenantLiquibaseConfig(MultiTenantProperties multiTenantProperties,
									  LiquibaseProperties liquibaseProperties, MultiTenantDataSources multiTenantDataSources,
									  ResourceLoader resourceLoader) {
		this.multiTenantProperties = multiTenantProperties;
		this.liquibaseProperties = liquibaseProperties;
		this.multiTenantDataSources = multiTenantDataSources;
		this.resourceLoader = resourceLoader;
	}

	@PostConstruct
	public void checkChangelogExists() {
		if (this.liquibaseProperties.isCheckChangeLogLocation()) {
			Resource resource = this.resourceLoader.getResource(this.liquibaseProperties.getChangeLog());
			Assert.state(resource.exists(), () -> "Cannot find changelog location: " + resource
					+ " (please add changelog or check your Liquibase " + "configuration)");
		}
	}

	@Bean
	public MultiTenantLiquibase multiTenantLiquibase() {
		MultiTenantLiquibase multiTenantLiquibase = new MultiTenantLiquibase();
		multiTenantLiquibase.setDataSources(multiTenantDataSources.getAll());
		multiTenantLiquibase.setChangeLog(this.liquibaseProperties.getChangeLog());
		multiTenantLiquibase.setContexts(this.liquibaseProperties.getContexts());
		multiTenantLiquibase.setDefaultSchema(this.liquibaseProperties.getDefaultSchema());
		multiTenantLiquibase.setLiquibaseSchema(this.liquibaseProperties.getLiquibaseSchema());
		multiTenantLiquibase.setLiquibaseTablespace(this.liquibaseProperties.getLiquibaseTablespace());
		multiTenantLiquibase.setDatabaseChangeLogTable(this.liquibaseProperties.getDatabaseChangeLogTable());
		multiTenantLiquibase.setDatabaseChangeLogLockTable(this.liquibaseProperties.getDatabaseChangeLogLockTable());
		multiTenantLiquibase.setDropFirst(this.liquibaseProperties.isDropFirst());
		multiTenantLiquibase.setShouldRun(this.multiTenantProperties.isLiquibaseEnabled());
		multiTenantLiquibase.setLabels(this.liquibaseProperties.getLabels());
		multiTenantLiquibase.setParameters(this.liquibaseProperties.getParameters());
		multiTenantLiquibase.setRollbackFile(this.liquibaseProperties.getRollbackFile());

		return multiTenantLiquibase;
	}

}

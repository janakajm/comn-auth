package com.loits.comn.auth.mt;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 * @author Amal
 * @since 25/01/2019
 */
@Data
public class MultiTenantLiquibase implements InitializingBean, ResourceLoaderAware {

	private Collection<DataSource> dataSources;

	private Logger log = LoggerFactory.getLogger(MultiTenantLiquibase.class);

	private ResourceLoader resourceLoader;

	private String changeLog;

	private String contexts;

	private String labels;

	private Map<String, String> parameters;

	private String defaultSchema;

	private String liquibaseSchema;

	private String liquibaseTablespace;

	private String databaseChangeLogTable;

	private String databaseChangeLogLockTable;

	private boolean dropFirst;

	private boolean shouldRun = true;

	private File rollbackFile;
	
	
	

	public Collection<DataSource> getDataSources() {
		return dataSources;
	}

	public void setDataSources(Collection<DataSource> dataSources) {
		this.dataSources = dataSources;
	}

	public Logger getLog() {
		return log;
	}

	public void setLog(Logger log) {
		this.log = log;
	}

	public ResourceLoader getResourceLoader() {
		return resourceLoader;
	}

	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public String getChangeLog() {
		return changeLog;
	}

	public void setChangeLog(String changeLog) {
		this.changeLog = changeLog;
	}

	public String getContexts() {
		return contexts;
	}

	public void setContexts(String contexts) {
		this.contexts = contexts;
	}

	public String getLabels() {
		return labels;
	}

	public void setLabels(String labels) {
		this.labels = labels;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	public String getDefaultSchema() {
		return defaultSchema;
	}

	public void setDefaultSchema(String defaultSchema) {
		this.defaultSchema = defaultSchema;
	}

	public String getLiquibaseSchema() {
		return liquibaseSchema;
	}

	public void setLiquibaseSchema(String liquibaseSchema) {
		this.liquibaseSchema = liquibaseSchema;
	}

	public String getLiquibaseTablespace() {
		return liquibaseTablespace;
	}

	public void setLiquibaseTablespace(String liquibaseTablespace) {
		this.liquibaseTablespace = liquibaseTablespace;
	}

	public String getDatabaseChangeLogTable() {
		return databaseChangeLogTable;
	}

	public void setDatabaseChangeLogTable(String databaseChangeLogTable) {
		this.databaseChangeLogTable = databaseChangeLogTable;
	}

	public String getDatabaseChangeLogLockTable() {
		return databaseChangeLogLockTable;
	}

	public void setDatabaseChangeLogLockTable(String databaseChangeLogLockTable) {
		this.databaseChangeLogLockTable = databaseChangeLogLockTable;
	}

	public boolean isDropFirst() {
		return dropFirst;
	}

	public void setDropFirst(boolean dropFirst) {
		this.dropFirst = dropFirst;
	}

	public boolean isShouldRun() {
		return shouldRun;
	}

	public void setShouldRun(boolean shouldRun) {
		this.shouldRun = shouldRun;
	}

	public File getRollbackFile() {
		return rollbackFile;
	}

	public void setRollbackFile(File rollbackFile) {
		this.rollbackFile = rollbackFile;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (log.isInfoEnabled()) {
			log.info("DataSources based multitenancy enabled");
		}
		resolveDataSources();
		runOnAllDataSources();
	}

	private void resolveDataSources() {
		Assert.notEmpty(dataSources, "No DataSource is provided");
	}

	private void runOnAllDataSources() throws LiquibaseException {
		for (DataSource dataSource : dataSources) {
			if (log.isInfoEnabled()) {
				log.info("Initializing Liquibase for data source {}", dataSource);
			}
			SpringLiquibase liquibase = getSpringLiquibase(dataSource);
			liquibase.afterPropertiesSet();
			if (log.isInfoEnabled()) {
				log.info("Liquibase ran for data source {}", dataSource);
			}
		}
	}

	private SpringLiquibase getSpringLiquibase(DataSource dataSource) {
		SpringLiquibase liquibase = new SpringLiquibase();
		liquibase.setChangeLog(changeLog);
		liquibase.setChangeLogParameters(parameters);
		liquibase.setContexts(contexts);
		liquibase.setLabels(labels);
		liquibase.setDropFirst(dropFirst);
		liquibase.setShouldRun(shouldRun);
		liquibase.setRollbackFile(rollbackFile);
		liquibase.setResourceLoader(resourceLoader);
		liquibase.setDataSource(dataSource);
		liquibase.setDefaultSchema(defaultSchema);
		liquibase.setLiquibaseSchema(liquibaseSchema);
		liquibase.setLiquibaseTablespace(liquibaseTablespace);
		liquibase.setDatabaseChangeLogTable(databaseChangeLogTable);
		liquibase.setDatabaseChangeLogLockTable(databaseChangeLogLockTable);
		return liquibase;
	}

}

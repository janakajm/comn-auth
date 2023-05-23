package com.loits.comn.auth.mt;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author Amal
 * @since 25/01/2019 
 */
@ConfigurationProperties(prefix = "tenant")
@Data
public class MultiTenantProperties {

    private String defaultTenantId;

    private boolean liquibaseEnabled;

    private List<TenantDataSourceProperties> dataSources;


    public static class TenantDataSourceProperties extends DataSourceProperties {

        @Getter
        @Setter
        private String tenantId;

		public String getTenantId() {
			return tenantId;
		}

		public void setTenantId(String tenantId) {
			this.tenantId = tenantId;
		}
        
        

    }


	public String getDefaultTenantId() {
		return defaultTenantId;
	}


	public void setDefaultTenantId(String defaultTenantId) {
		this.defaultTenantId = defaultTenantId;
	}


	public boolean isLiquibaseEnabled() {
		return liquibaseEnabled;
	}


	public void setLiquibaseEnabled(boolean liquibaseEnabled) {
		this.liquibaseEnabled = liquibaseEnabled;
	}


	public List<TenantDataSourceProperties> getDataSources() {
		return dataSources;
	}


	public void setDataSources(List<TenantDataSourceProperties> dataSources) {
		this.dataSources = dataSources;
	}

}

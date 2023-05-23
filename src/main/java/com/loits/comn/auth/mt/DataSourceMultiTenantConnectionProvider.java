package com.loits.comn.auth.mt;

import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.springframework.transaction.CannotCreateTransactionException;

import javax.sql.DataSource;

import static com.loits.comn.auth.Constants.DEFAULT_TENANT_ID;

/**
 * @author Amal
 * @since 25/01/2019 
 */
public class DataSourceMultiTenantConnectionProvider extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl {

    private MultiTenantDataSources multiTenantDataSources;


    public DataSourceMultiTenantConnectionProvider(MultiTenantDataSources multiTenantDataSources) {
        this.multiTenantDataSources = multiTenantDataSources;
    }


    @Override
    protected DataSource selectAnyDataSource() {
        return this.multiTenantDataSources.getDefault();
    }

    //

    /* @Override
    protected DataSource selectDataSource(String tenantIdentifier) {
        return this.multiTenantDataSources.get(tenantIdentifier);
    }*/
    
    @Override
    protected DataSource selectDataSource(String tenantIdentifier) {
    	
	String tenant = TenantHolder.getTenantId() != null ? TenantHolder.getTenantId() : DEFAULT_TENANT_ID;
	if (this.multiTenantDataSources.get(tenant)!=null) {
		return this.multiTenantDataSources.get(tenant);
	}else {
		throw new CannotCreateTransactionException("Invalid Tenant");
	}
	
    }


}

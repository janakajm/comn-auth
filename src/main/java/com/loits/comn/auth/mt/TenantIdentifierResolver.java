package com.loits.comn.auth.mt;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import static com.loits.comn.auth.Constants.DEFAULT_TENANT_ID;

/**
 * @author Amal
 * @since 25/01/2019 
 */
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver {

    @Override
    public String resolveCurrentTenantIdentifier() {
        return TenantHolder.getTenantId() != null ? TenantHolder.getTenantId() : DEFAULT_TENANT_ID;
    }


    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }


}

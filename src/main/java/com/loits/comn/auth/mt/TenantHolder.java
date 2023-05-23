package com.loits.comn.auth.mt;
/**
 * @author Amal
 * @since 25/01/2019 
 */
public class TenantHolder {

    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();


    public static void setTenantId(String tenantId) {
        TENANT.set(tenantId);
    }


    public static String getTenantId() {
        return TENANT.get();
    }


    public static void clear() {
        TENANT.remove();
    }


}

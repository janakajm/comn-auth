package com.loits.comn.auth.config;


import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.mt.TenantHolder;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Aspect
@Configuration
public class ConfigureTenant {

	private static final Logger logger = LoggerFactory.getLogger(ConfigureTenant.class);
	private static final String NO_TENANT_FOUND = "No tenant found";

	@Before("execution(* com.loits.comn.auth.controller.*.*(..))")
	public void before(JoinPoint joinPoint) throws Exception {

		Object[] methodeParams = joinPoint.getArgs();
		String tenantId = (String)methodeParams[0];
		if ((tenantId == null) || tenantId.equals("")) {
			logger.warn(NO_TENANT_FOUND);
			FXDefaultException fxd = new FXDefaultException("1003", NO_TENANT_FOUND,NO_TENANT_FOUND, new java.util.Date(), HttpStatus.BAD_REQUEST);
			fxd.setSeverity(true);
			throw fxd;
		} else {
			TenantHolder.setTenantId(tenantId);
		}
	}

	@After("execution(* com.loits.comn.auth.controller.*.*(..))")
	public void after(JoinPoint joinPoint){
		TenantHolder.clear();
	}


}

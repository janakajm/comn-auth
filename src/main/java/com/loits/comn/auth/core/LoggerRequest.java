package com.loits.comn.auth.core;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerRequest {
	private static final Logger logger= LoggerFactory.getLogger(LoggerRequest.class);
	private String requestId;
	private static LoggerRequest instance;
	private String dateAndTime="Date And Time";
	private String api="Api";
	private String classNameDesc="Class Name";
	private String enterMethod="Enter Method";
	private String endMethod="End Method";
	private String args="Args";
	private String space="  ";
	private String colon="  :  ";
	private String error="/*Error */";
	private String infomation="/*INFO */";
	private LoggerRequest() {}
	
	public static LoggerRequest getInstance() {
		if(instance==null) {
			instance = new LoggerRequest();
		}
        return instance;
    }
	public void logControllerBefore(String apiName, String className, String methodName, String argument) {
		StringBuilder massage = new StringBuilder();
		massage.append(requestId).append(space).append(dateAndTime).append(colon).append(formatDate(new Date())).append(space)
		.append(api).append(colon).append(apiName).append(space).append(classNameDesc).append(colon).append(className).append(space)
		.append(enterMethod).append(colon).append(methodName).append(space).append(args).append(colon).append(argument);
		logger.info(massage.toString());
	}
	public void logAfter(String className, String methodName) {
		StringBuilder massage = new StringBuilder();
		massage.append(requestId).append(space).append(dateAndTime).append(colon).append(formatDate(new Date())).append(space)
		.append(classNameDesc).append(colon).append(className).append(space).append(endMethod).append(colon).append(methodName);
		logger.info(massage.toString());
	}
	public void logServiceBefore(String className, String methodName, String argument) {
		StringBuilder massage = new StringBuilder();
		massage.append(requestId).append(space).append(dateAndTime).append(colon).append(formatDate(new Date())).append(space)
		.append(classNameDesc).append(colon).append(className).append(space).append(enterMethod).append(colon).append(methodName)
		.append(space).append(args).append(colon).append(argument);
		logger.info(massage.toString());
	}
	public void logCommonError(String errorMassage) {
		StringBuilder massage = new StringBuilder();
		massage.append(requestId).append(space).append(dateAndTime).append(colon).append(formatDate(new Date())).append(space)
		.append(error).append(colon).append(errorMassage);
		logger.error(massage.toString());
	}
	
	public void logInfo(String info) {
		StringBuilder massage = new StringBuilder();
		massage.append(requestId).append(space).append(dateAndTime).append(colon).append(formatDate(new Date())).append(space)
		.append(infomation).append(colon).append(info);
		logger.info(massage.toString());
	}
	
	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}
	private String formatDate(Date date) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		return format.format(date);
	}
	
}

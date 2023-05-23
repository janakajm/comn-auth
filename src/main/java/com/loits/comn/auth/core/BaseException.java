/*
 * Copyright (c) 2018. LOLC Technology Services Ltd.
 * Author: Ranjith Kodikara
 * Date: 12/12/18 10:45
 */

package com.loits.comn.auth.core;

import java.util.Date;
import java.util.List;

import javax.persistence.MappedSuperclass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/**
 * 
 * All custom defined Exceptions should inherit this...
 * 
 * @author ranjithk 
 * @since   2018-12-13
 * @version 1.0
 * 
 */
@MappedSuperclass
public abstract class BaseException extends Exception {

	private static final long serialVersionUID = 6874532513778838448L;
	private static final Logger logger = LoggerFactory.getLogger(BaseException.class);
	private String errorCode;
	private String errorShortDescription;
	private String errorDescription;

	private Date errorDate;
	private boolean severity;
	private HttpStatus httpStatus;
	private List<String> errors;

	public BaseException(HttpStatus status, String errorShortDescription, String errorDescription, List<String> errors) {
		super(errorDescription);
		this.httpStatus = status;
		this.errorShortDescription = errorShortDescription;
		this.errorDescription = errorDescription;
		this.errors = errors;
	}

	public BaseException() {
		super();
		logger.info("Exception",
				this.getErrorCode() + "-" + this.getErrorShortDescription() + "-" + this.getErrorDescription());
	}

	protected BaseException(String errorCode, String errorShortDescription, Date errorDate) {
		super();
		this.errorCode = errorCode;
		this.errorShortDescription = errorShortDescription;
		this.errorDate = errorDate;
		logger.info("Exception",
				this.getErrorCode() + "-" + this.getErrorShortDescription() + "-" + this.getErrorDescription());
	}

	protected BaseException(String errorCode, String errorShortDescription, String errorDescription, Date errorDate) {
		super(errorDescription);
		this.errorCode = errorCode;
		this.errorShortDescription = errorShortDescription;
		this.errorDescription = errorDescription;
		this.errorDate = errorDate;

		logger.info("Exception",
				this.getErrorCode() + "-" + this.getErrorShortDescription() + "-" + this.getErrorDescription());
	}

	protected BaseException(String errorCode, String errorShortDescription, String errorDescription, Date errorDate,
							HttpStatus status) {
		super(errorDescription);
		this.errorCode = errorCode;
		this.errorShortDescription = errorShortDescription;
		this.errorDescription = errorDescription;
		this.errorDate = errorDate;
		this.httpStatus = status;

		logger.info("Exception",
				this.getErrorCode() + "-" + this.getErrorShortDescription() + "-" + this.getErrorDescription());
	}

	protected BaseException(String errorCode, String errorShortDescription, String errorDescription, Date errorDate,
							HttpStatus status, Boolean severity) {
		super(errorDescription);
		this.errorCode = errorCode;
		this.errorShortDescription = errorShortDescription;
		this.errorDescription = errorDescription;
		this.errorDate = errorDate;
		this.httpStatus = status;
		this.severity = severity;

		logger.info("Exception",
				this.getErrorCode() + "-" + this.getErrorShortDescription() + "-" + this.getErrorDescription());
	}

	/**
	 * This method will do whatever required, depending on the severity of the
	 * exception ...
	 */
	private void getActionforException(boolean sev) {
		if (sev) {
			System.out.println("notify the administrator , service desk and operations people ...");
			// Relevant code should be inserted here...
		} else {
			System.out.println("notify the user that his operation is not successful..");
			// Relevant code should be inserted here...
		}

	}

	public boolean getSeverity() {
		return severity;
	}

	public void setSeverity(boolean severity) {
		this.severity = severity;
	}

	/**
	 * @return the errorCode
	 */
	public String getErrorCode() {
		return errorCode;
	}

	/**
	 * @param errorCode the errorCode to set
	 */
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	/**
	 * @return the errorShortDescription
	 */
	public String getErrorShortDescription() {
		return errorShortDescription;
	}

	/**
	 * @param errorShortDescription the errorShortDescription to set
	 */
	public void setErrorShortDescription(String errorShortDescription) {
		this.errorShortDescription = errorShortDescription;
	}

	/**
	 * @return the errorDescription
	 */
	public String getErrorDescription() {
		return errorDescription;
	}

	/**
	 * @param errorDescription the errorDescription to set
	 */
	public void setErrorDescription(String errorDescription) {
		this.errorDescription = errorDescription;
	}

	/**
	 * @return the errorDate
	 */
	public Date getErrorDate() {
		return errorDate;
	}

	/**
	 * @param errorDate the errorDate to set
	 */
	public void setErrorDate(Date errorDate) {
		this.errorDate = errorDate;
	}

	/**
	 *
	 * @return httpstatus
	 */
	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	public List<String> getErrors() {
		return errors;
	}

	public void setErrors(List<String> errors) {
		this.errors = errors;
	}
}
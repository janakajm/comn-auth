/*
 * Copyright (c) 2018. LOLC Technology Services Ltd.
 * Author: Ranjith Kodikara
 * Date: 12/12/18 10:45
 */

package com.loits.comn.auth.core;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.RollbackException;

import org.apache.http.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.loits.comn.auth.config.Translator;

/**
 * 
 * This will return the relevant object based on the caught exception
 * 
 * @author ranjithk
 * @since 2018-12-13
 * @version 1.0
 * 
 */
@RestControllerAdvice
public class BaseResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger logger_Breh = LoggerFactory.getLogger(BaseResponseEntityExceptionHandler.class);

	/**
	 * DB error handling
	 *
	 * @param ex
	 * @return
	 */
	@ExceptionHandler(RollbackException.class)
	protected ResponseEntity<Object> rollbackException(RollbackException ex) {
		ex.printStackTrace();
		logger.debug(ex.getMessage());
		logger.error("DB rollback error handler");
		FXDefaultException fxDefaultException = new FXDefaultException("1100", ex.getMessage(), "DB rollback error", new Date(), BAD_REQUEST, true);
		ex.setStackTrace(new StackTraceElement[0]);
		fxDefaultException.setStackTrace(ex.getStackTrace());
		return new ResponseEntity<Object>(fxDefaultException, fxDefaultException.getHttpStatus());
	}

	/**
	 * Date Parse error handling
	 *
	 * @param ex
	 * @return
	 */
	@ExceptionHandler(ParseException.class)
	protected ResponseEntity<Object> parseException(
			ParseException ex) {
		ex.printStackTrace();
		logger.error("Parse Exception ");
		FXDefaultException fxDefaultException = new FXDefaultException("1101", ex.getMessage(), "There was a date parsing error", new Date(), BAD_REQUEST, true);
		ex.setStackTrace(new StackTraceElement[0]);
		fxDefaultException.setStackTrace(ex.getStackTrace());
		return new ResponseEntity<Object>(fxDefaultException, fxDefaultException.getHttpStatus());
	}


	/**
	 * String to number conversion error handling
	 *
	 * @param ex
	 * @return
	 */
	@ExceptionHandler({NumberFormatException.class, ConversionFailedException.class})
	protected ResponseEntity<Object> numberException(
			Exception ex) {
		ex.printStackTrace();
		logger.error("Numberformat error handler");
		FXDefaultException fxDefaultException = new FXDefaultException("1102", ex.getMessage(), "Your request contains invalid numbers (in Object properties)", new Date(), BAD_REQUEST, false);
		ex.setStackTrace(new StackTraceElement[0]);
		fxDefaultException.setStackTrace(ex.getStackTrace());
		return new ResponseEntity<Object>(fxDefaultException, fxDefaultException.getHttpStatus());
	}

	/**
	 * JSON mapping exceptions
	 *
	 * @param ex
	 * @return
	 */
	@ExceptionHandler(JsonMappingException.class)
	protected ResponseEntity<Object> jsonMappingException(JsonMappingException ex) {
		ex.printStackTrace();
		logger.error("JsonMapping error handler");
		FXDefaultException fxDefaultException = new FXDefaultException("1103", ex.getMessage(), "POJO could not be mapped to JSON!", new Date(), BAD_REQUEST, true);
		ex.setStackTrace(new StackTraceElement[0]);
		fxDefaultException.setStackTrace(ex.getStackTrace());
		return new ResponseEntity<Object>(fxDefaultException, fxDefaultException.getHttpStatus());
	}

	/**
	 * Transaction validation
	 * @param ex
	 * @param request
	 * @return
	 */
	@ExceptionHandler(CannotCreateTransactionException.class)
	public ResponseEntity<Object> cannotCreateTransactionException(CannotCreateTransactionException ex, WebRequest request){
		FXDefaultException fxDefaultException = new FXDefaultException("1104", "Cannot Create Transaction", ex.getMessage(), new Date(), BAD_REQUEST, true);
		ex.setStackTrace(new StackTraceElement[0]);
		fxDefaultException.setStackTrace(ex.getStackTrace());
		return new ResponseEntity<Object>(fxDefaultException, fxDefaultException.getHttpStatus());
	}

	/**
	 * HTTP Exception
	 * @param ex
	 * @param headers
	 * @param status
	 * @param request
	 * @return
	 */
	@ExceptionHandler
	protected ResponseEntity<Object> httpException(HttpException ex, HttpHeaders headers, HttpStatus status, WebRequest request){
		System.out.println("Test");
		FXDefaultException fxDefaultException = new FXDefaultException("1105", ex.getMessage(), "HTTP Error", new Date(), BAD_REQUEST, false);
		ex.setStackTrace(new StackTraceElement[0]);
		fxDefaultException.setStackTrace(ex.getStackTrace());
		return new ResponseEntity<Object>(fxDefaultException, fxDefaultException.getHttpStatus());
	}

	/**
	 * Handle all Exceptions
	 * @param ex
	 * @param request
	 * @return
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleAllExceptions(Exception ex, WebRequest request) {
		ex.printStackTrace();
		logger.error("General error handler");
		FXDefaultException fxDefaultException = new FXDefaultException("1999", ex.getMessage(), "Opps!, something went wrong. Please try again", new Date(), BAD_REQUEST, false);
		ex.setStackTrace(new StackTraceElement[0]);
		fxDefaultException.setStackTrace(ex.getStackTrace());
		return new ResponseEntity<Object>(fxDefaultException, fxDefaultException.getHttpStatus());
	}

	/**
	 * Error in Req body
	 * @param ex
	 * @param headers
	 * @param status
	 * @param request
	 * @return
	 */
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, WebRequest request){
		FXDefaultException fxDefaultException = new FXDefaultException("1107", ex.getMessage(), "Request Body not readable", new Date(), BAD_REQUEST, true);
		fxDefaultException.setStackTrace(new StackTraceElement[0]);
		return  new ResponseEntity<Object>(fxDefaultException, fxDefaultException.getHttpStatus());
	}

	/**
	 * Missing Req Parameters
	 * @param ex
	 * @param headers
	 * @param status
	 * @param request
	 * @return
	 */
	@Override
	protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatus status, WebRequest request){
		FXDefaultException fxDefaultException = new FXDefaultException("1108","INVALID ARGUEMENTS" , ex.getMessage(), new Date(), BAD_REQUEST, true);
		fxDefaultException.setStackTrace(new StackTraceElement[0]);
		return  new ResponseEntity<Object>(fxDefaultException, fxDefaultException.getHttpStatus());
	}

	/**
	 * Missing Req Headers
	 * @param ex
	 * @param request
	 * @return
	 */
	@ExceptionHandler(MissingRequestHeaderException.class)
	public ResponseEntity<Object> missingReqHeader(MissingRequestHeaderException ex, WebRequest request){
		FXDefaultException fxDefaultException = new FXDefaultException("1109", "INVALID ARGUMENTS", ex.getMessage(), new Date(), BAD_REQUEST, true);
		fxDefaultException.setStackTrace(new StackTraceElement[0]);
		return  new ResponseEntity<Object>(fxDefaultException, fxDefaultException.getHttpStatus());

	}

	/**
	 * Custom Validations
	 * @param ex
	 * @param request
	 * @return
	 */
	@ExceptionHandler(FXDefaultException.class)
	public ResponseEntity<Object> validateRecordException(FXDefaultException ex, WebRequest request){
		ex.setStackTrace(new StackTraceElement[0]);
		return new ResponseEntity<Object>(ex, ex.getHttpStatus());
	}

	/**
	 * DTO Field validations
	 * @param ex
	 * @param headers
	 * @param status
	 * @param request
	 * @return
	 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			HttpHeaders headers,
			HttpStatus status,
			WebRequest request) {
		List<String> errors = new ArrayList<String>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			errors.add(error.getField() + ": " + Translator.toLocale(error.getDefaultMessage()));
		}
		for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
			errors.add(error.getObjectName() + ": " + Translator.toLocale(error.getDefaultMessage()));
		}
		ex.setStackTrace(new StackTraceElement[0]);
		FXDefaultException fxDefaultException = new FXDefaultException(HttpStatus.BAD_REQUEST, "INVALID_ARGUEMENTS", "Field validation failed!", errors);
		fxDefaultException.setErrorCode("1111");
		fxDefaultException.setStackTrace(ex.getStackTrace());
		return handleExceptionInternal(
				ex, fxDefaultException, headers, fxDefaultException.getHttpStatus(), request);
	}

}


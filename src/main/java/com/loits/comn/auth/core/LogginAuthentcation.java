package com.loits.comn.auth.core;

public class LogginAuthentcation {
	private static final ThreadLocal<String> USER = new ThreadLocal<>();

	public static String getUserName() {
		return USER.get();
	}

	public static void setUserName(String userName) {
		USER.set(userName);
	}
	
	
}

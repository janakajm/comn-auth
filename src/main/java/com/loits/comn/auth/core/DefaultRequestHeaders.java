package com.loits.comn.auth.core;

import org.springframework.http.HttpHeaders;

public class DefaultRequestHeaders {
	private static DefaultRequestHeaders instance;
	private HttpHeaders headers;
	private DefaultRequestHeaders() {}
	public static DefaultRequestHeaders getInstance() {
		if(instance==null) {
			instance = new DefaultRequestHeaders();
		}
        return instance;
    }
	
	public void setHeaders(String userAgent,String requestId, String traceId, String spanId, String parentspanId, String sampled, String flags, String spanContext) {
		headers = new HttpHeaders();
		headers.set("user-agent", userAgent);
		headers.set("x-request-id", requestId);
		headers.set("x-b3-traceid", traceId);
		headers.set("x-b3-spanid", spanId);
		headers.set("x-b3-parentspanid", parentspanId);
		headers.set("x-b3-sampled", sampled);
		headers.set("x-b3-flags", flags);
		headers.set("x-ot-span-context", spanContext);
	}
	
	public HttpHeaders getHeaders() {
		if(headers==null) {
			headers=new HttpHeaders();
		}
		return headers;
	}
	
}

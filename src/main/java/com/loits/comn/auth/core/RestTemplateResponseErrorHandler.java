package com.loits.comn.auth.core;
import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Scanner;

import javax.ws.rs.NotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
public class RestTemplateResponseErrorHandler implements ResponseErrorHandler {
    Logger logger = LogManager.getLogger(RestTemplateResponseErrorHandler.class);
    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return (response.getStatusCode().series() == CLIENT_ERROR
                || response.getStatusCode().series() == SERVER_ERROR);
    }
    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        try {
            String responseAsString = toString(response.getBody());
            logger.error("ResponseBody: {}", responseAsString);
        } catch (IOException ex) {
            // ignore
        }
        if (response.getStatusCode()
                .series() == HttpStatus.Series.SERVER_ERROR) {
            throw new IOException(response.getStatusText());
        } else if (response.getStatusCode()
                .series() == HttpStatus.Series.CLIENT_ERROR) {
            if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new NotFoundException(response.getStatusText());
            }else if (response.getStatusCode() != HttpStatus.UNAUTHORIZED){
                throw new IOException(response.getStatusText());
            }
        }
    }
    @Override
    public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
        try {
            String responseAsString = toString(response.getBody());
            logger.error("URL: {}, HttpMethod: {}, ResponseBody: {}", url, method, responseAsString);
        } catch (IOException ex) {
            // ignore
        }
        if (response.getStatusCode()
                .series() == HttpStatus.Series.SERVER_ERROR) {
            throw new IOException(response.getStatusText());
        } else if (response.getStatusCode()
                .series() == HttpStatus.Series.CLIENT_ERROR) {
            if (response.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                throw new NotFoundException(response.getStatusText());
            }else if (!response.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
                throw new IOException(response.getStatusText());
            }
        }
    }
    String toString(InputStream inputStream) {
        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
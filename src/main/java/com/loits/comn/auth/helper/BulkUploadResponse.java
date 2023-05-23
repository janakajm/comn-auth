package com.loits.comn.auth.helper;

import lombok.Data;

import java.util.List;

@Data
public class BulkUploadResponse {

  private int totalRecords;
  private int successCount;
  private List<Object> data;
  private List<Error> errorData;

  @Data
  public static class Error {
    private String error;
    private String errorDescription;
    private Object data;

    public Error() {
    }
  }

  public BulkUploadResponse() {
  }
}

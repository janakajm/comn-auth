package com.loits.comn.auth.dto;

import lombok.Data;

@Data
public class SyncResponse {
    private Long id;
    private Long noOfObjectsRetrieved;
    private Long noOfObjectsSynced;
    private Long noOfObjectsBeforeSync;
}

package com.loits.comn.auth.domain;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.loits.comn.auth.core.BaseEntity;

import lombok.Data;

@Data
@Entity
@Table(name = "process_log")
public class ProcessLog extends BaseEntity{
	
	@Column(name = "process_code")
    private String processCode;
	
    @Column(name = "created_by", length = 45)
    private String createdBy;

    @Column(name = "created_on")
    private Timestamp createdOn;

    @Column(name = "process")
    private String process;

	public String getProcessCode() {
		return processCode;
	}

	public void setProcessCode(String processCode) {
		this.processCode = processCode;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}

	public String getProcess() {
		return process;
	}

	public void setProcess(String process) {
		this.process = process;
	}
    
}

package com.loits.comn.auth.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.loits.comn.auth.core.AsyncBaseEntity;
import com.loits.comn.auth.core.BaseEntity;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@Table(name = "LS_ASYNC_LOG")
public class AsyncLog  extends BaseEntity implements Serializable{

  private static final long serialVersionUID = 1L;

  @Column(name = "LOG_TIME")
  private Date logTime;

  @Column(name = "DESCRIPTION")
  private String description;

  @Column(name = "REF_TABLE")
  private String refTable;

  @Column(name = "REF_KEY")
  private String refKey;

  @Column(name = "REF_VALUE")
  private String refValue;

  @Lob
  @Column(name = "STACKTRACE")
  private String stacktrace;

  @JsonIgnore
  @ManyToOne
  @JoinColumn(name = "SUB_TASK_ID", referencedColumnName = "ID")
  private AsyncSubTask asyncSubTask;

  @JsonIgnore
  @ManyToOne
  @JoinColumn(name = "M_" +
          "TASK_ID", referencedColumnName = "ID")
  private AsyncTask asyncTask;

  @Transient
  private String tenant;

}

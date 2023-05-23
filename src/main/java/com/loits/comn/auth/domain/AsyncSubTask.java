/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.loits.comn.auth.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.loits.comn.auth.core.AsyncBaseEntity;
import com.loits.comn.auth.core.BaseEntity;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author lahirubandara
 */
@Data
@Entity
@Table(name = "LS_ASYNC_SUB_TASK")
public class AsyncSubTask extends BaseEntity implements Serializable {
  private static final long serialVersionUID = 1L;

  @Column(name = "TASK")
  private String task;

  @Column(name = "S_DATE")
  private Date sDate;

  @Column(name = "E_DATE")
  private Date eDate;

  @Column(name = "M_DATE")
  private Date mDate;

  @Column(name = "STATUS")
  private String status;

  @Column(name = "SEVERITY")
  private String severity;

  @Column(name = "NOTIFY")
  private String notify;

  @Lob
  @JoinColumn(name = "META")
  private String meta;
  //Ex: individual permission details

  @JsonIgnore
  @ManyToOne
  @JoinColumn(name = "M_TASK_ID", referencedColumnName = "ID")
  private AsyncTask asyncTask;

  @OneToMany(mappedBy = "asyncSubTask", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  private List<AsyncLog> asyncLogs;

  @Transient
  private String tenant;

  public AsyncSubTask() {
  }
}

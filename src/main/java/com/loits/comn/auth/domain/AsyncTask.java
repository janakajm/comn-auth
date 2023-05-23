package com.loits.comn.auth.domain;

import com.loits.comn.auth.core.AsyncBaseEntity;
import com.loits.comn.auth.core.BaseEntity;
import lombok.Data;
import org.springframework.data.web.SortDefault;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "LS_ASYNC_TASK")
public class AsyncTask extends BaseEntity implements Serializable {

  private static final long serialVersionUID = 1L;

  @Column(name = "TASK")
  private String task;
  //Ex: Auth0 permission verification

  @Column(name = "S_DATE")
  private Date sDate;

  @Column(name = "E_DATE")
  private Date eDate;

  @Column(name = "M_DATE")
  private Date mDate;

  @Column(name = "JOB_ID")
  private String jobId;

  @Column(name = "STATUS")
  private String status;

  @Column(name = "MODULE")
  private String module;

  @Lob
  @Column(name = "META")
  private String meta;
  //JSON object - ex: local permissions, remote permissions (as json object properties)

  @Column(name = "SEVERITY")
  private String severity;

  @Column(name = "NOTIFY")
  private String notify;

  @OneToMany(mappedBy = "asyncTask", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  private List<AsyncSubTask> asyncSubTasks;

  @OneToMany(mappedBy = "asyncTask", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  private List<AsyncLog> asyncLogs;

  @Transient
  private String tenant;

}

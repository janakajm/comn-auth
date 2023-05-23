package com.loits.comn.auth.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.loits.comn.auth.core.BaseEntity;
import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "module_meta")
public class ModuleMeta extends BaseEntity {

    @Basic
    @Column(name = "meta_key")
    private String key;

    @Basic
    @Column(name = "value")
    private String value;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "module", referencedColumnName = "code", nullable = false)
    private Module module;

}

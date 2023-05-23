package com.loits.comn.auth.domain;

import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

@Data
@Entity
@Table(name = "module")
public class Module {

    @Id
    @Basic
    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Basic
    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Basic
    @Column(name = "created_by", nullable = false, length = 45)
    private String createdBy;

    @Basic
    @Column(name = "created_on", nullable = false)
    private Timestamp createdOn;

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ModuleMeta> moduleMetaList;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Module module = (Module) object;
        return Objects.equals(code, module.code) &&
                Objects.equals(label, module.label) &&
                Objects.equals(createdBy, module.createdBy) &&
                Objects.equals(createdOn, module.createdOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, label, createdBy, createdOn);
    }
}

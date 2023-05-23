package com.loits.comn.auth.utility;

import com.loits.comn.auth.domain.*;
import lombok.extern.flogger.Flogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class PermissionSpecification {

    public static Specification<Permission> nameContains(String searchWord) {
        return (root, query, builder) -> {
            Expression<String> nameLowerCase = builder.lower(root.get("name"));
            return builder.like(nameLowerCase, "%" + searchWord.toLowerCase() + "%");
        };
    }

    public static Specification<Permission> hasPermissionWithUserId(String userId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> list = new ArrayList<Predicate>();
            Join<PermissionGroups, Permission> permissionGroups = root.join("permissionGroups");
            Join<RoleGroup, PermissionGroups> roleGroup = permissionGroups.join("roleGroup");
            Join<RoleGroup, RoleGroupUser> roleGroupUser = permissionGroups.join("roleGroup");

            list.add(criteriaBuilder.equal(roleGroupUser.get("user").get("userId"), userId));

            Predicate[] p = new Predicate[list.size()];
            return criteriaBuilder.and(list.toArray(p));
//            return criteriaBuilder.equal(roleGroupUser.get("user").get("userId"), userId);
        };
    }
}

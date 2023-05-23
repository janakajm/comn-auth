package com.loits.comn.auth.services;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.Role;
import com.loits.comn.auth.dto.AddUserGroups;
import com.loits.comn.auth.dto.BranchAssignment;
import com.loits.comn.auth.dto.RemoveUser;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AuthUserService {
    String getAll(Pageable pageable, String search, String projection) throws FXDefaultException;

	String getAllUser(String search, String projection) throws FXDefaultException;
}

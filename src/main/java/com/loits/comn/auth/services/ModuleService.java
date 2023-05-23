package com.loits.comn.auth.services;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.ProcessLog;
import com.loits.comn.auth.dto.AddModule;
import com.querydsl.core.types.Predicate;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ModuleService {

    Page<?> getAll(Pageable pageable, String bookmarks, Predicate predicate, String projection);

    Object create(String projection, AddModule addModule, String user, String tenent) throws FXDefaultException;

    Object delete(String projection, String id, String user, String tenent) throws FXDefaultException;
    
    public void insertProcessLog(String processCode, String user);
	
	public void deleteProcessLog(String processCode);
	
	public ProcessLog checkProcessOn(String processCode);

	public List<?> getProcessByProcessCode(String tenent, String processCode);

}

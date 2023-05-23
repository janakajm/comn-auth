package com.loits.comn.auth.services.impl;


import com.loits.comn.auth.commons.NullAwareBeanUtilsBean;
import com.loits.comn.auth.config.Translator;
import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.Module;
import com.loits.comn.auth.domain.ModuleMeta;
import com.loits.comn.auth.domain.ProcessLog;
import com.loits.comn.auth.domain.QModule;
import com.loits.comn.auth.dto.AddModule;
import com.loits.comn.auth.repo.ModuleRepository;
import com.loits.comn.auth.repo.ProcessLogRepository;
import com.loits.comn.auth.services.ModuleService;
import com.loits.comn.auth.services.projections.ModuleProjection;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

@Service
public class ModuleServiceImpl implements ModuleService {

    Logger logger = LogManager.getLogger(ModuleServiceImpl.class);

    @Autowired
    ProjectionFactory projectionFactory;

    @Autowired
    ModuleRepository moduleRepository;
    
    @Autowired
    ProcessLogRepository processLogRepository;

    @Override
    public Page<?> getAll(Pageable pageable, String bookmarks, Predicate predicate, String projection) {
        BooleanBuilder bb = new BooleanBuilder(predicate);
        QModule module = QModule.module;

        //split and separate ids sent as a string
        if (!StringUtils.isEmpty(bookmarks)) {
            ArrayList<String> ids = new ArrayList<>();
            for (String id : bookmarks.split(",")) {
                ids.add(id);
            }
            bb.and(module.code.in(ids));
        }

        //update pageable object to ignorecase in sorting
        Sort sort = pageable.getSort();
        sort.forEach(order -> order.ignoreCase());
        PageRequest pageRequest = new PageRequest(pageable.getPageNumber(), pageable.getPageSize(), sort);

        return moduleRepository.findAll(bb.getValue(), pageRequest).map(
                module1 -> projectionFactory.createProjection(ModuleProjection.class, module1)
        );
    }

    @Override
    public Object create(String projection, AddModule addModule, String user, String tenent) throws FXDefaultException {
        if(moduleRepository.existsByCode(addModule.getCode())){
            throw new FXDefaultException("3002","DUPLICATE", Translator.toLocale("DUPLICATE_MOD"), new Date(), HttpStatus.BAD_REQUEST);
        }

        Module module = new Module();


        //copy properties from input object to RoleGroup object
        NullAwareBeanUtilsBean nullAwareBeanUtilsBean =new NullAwareBeanUtilsBean();
        try {
            nullAwareBeanUtilsBean.copyProperties(module, addModule);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        List<ModuleMeta> moduleMetaList = module.getModuleMetaList();

        if(moduleMetaList!=null){
            moduleMetaList.forEach(moduleMeta -> moduleMeta.setModule(module));
        }

        //set created properties
        module.setCreatedBy(user);
        module.setCreatedOn(new Timestamp(new Date().getTime()));

        //save to db
        moduleRepository.save(module);


        return projectionFactory.createProjection(ModuleProjection.class, module);
    }

    @Override
    public Object delete(String projection, String code, String user, String tenent) throws FXDefaultException {
        if(!moduleRepository.existsByCode(code)){
            throw new FXDefaultException("3001","INVALID_ATTEMPT", Translator.toLocale("INVALID_MOD"), new Date(), HttpStatus.BAD_REQUEST);
        }

        Module module = moduleRepository.findById(code).get();

        moduleRepository.deleteById(code);

        return projectionFactory.createProjection(ModuleProjection.class, module);
    }
    
    @Override
	@Transactional
	public void insertProcessLog(String processCode, String user) {
		ProcessLog processLog=new ProcessLog();
		processLog.setCreatedBy(user);
		processLog.setCreatedOn(new Timestamp(new Date().getTime()));
		processLog.setProcess("1");
		processLog.setProcessCode(processCode);
		
		processLogRepository.save(processLog);
	}

	@Override
	@Transactional
	public void deleteProcessLog(String processCode) {
		processLogRepository.deleteProcess(processCode);
	}
	
	@Override
	public ProcessLog checkProcessOn(String processCode) {
		List<ProcessLog> processlogList=processLogRepository.findByProcessCode(processCode);
		return !processlogList.isEmpty()?processlogList.get(0):null;
	}

	@Override
	public List<?> getProcessByProcessCode(String tenent, String processCode) {
		return processLogRepository.findByProcessCode(processCode);
	}

}

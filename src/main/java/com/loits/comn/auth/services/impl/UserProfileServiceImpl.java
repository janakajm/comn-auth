package com.loits.comn.auth.services.impl;


import com.loits.comn.auth.config.Translator;
import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.*;
import com.loits.comn.auth.repo.*;
import com.loits.comn.auth.services.UserProfileService;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    Logger logger = LogManager.getLogger(UserProfileServiceImpl.class);


    @Autowired
    UserProfileRepository userProfileRepository;

    @Override
    public Page<?> getAll(Pageable pageable, String search, String bookmarks, Predicate predicate, String projection) {
        BooleanBuilder bb = new BooleanBuilder(predicate);
        QUserProfile qUser = QUserProfile.userProfile;

        //split and separate ids sent as a string
        if (!StringUtils.isEmpty(bookmarks)) {
            ArrayList<Long> ids = new ArrayList<>();
            for (String id : bookmarks.split(",")) {
                ids.add(Long.parseLong(id));
            }
            bb.and(qUser.id.in(ids));
        }

        //search by fields on demand
        if(search!=null && !search.isEmpty()){
            bb.and(qUser.userName.containsIgnoreCase(search).or(qUser.email.containsIgnoreCase(search)
                    .or(qUser.userId.containsIgnoreCase(search)).or(qUser.employeeNumber.containsIgnoreCase(search))));
        }

        //update pageable object to ignorecase in sorting
        Sort sort = pageable.getSort();
        sort.forEach(order -> order.ignoreCase());
        PageRequest pageRequest = new PageRequest(pageable.getPageNumber(), pageable.getPageSize(), sort);

        return userProfileRepository.findAll(bb.getValue(), pageRequest);
    }

    @Override
    public Object getOne(String tenent, Long id, String projection) throws FXDefaultException {
        //check user availability
        if(!userProfileRepository.existsById(id)){
            throw new FXDefaultException("3002", "INVALID ATTEMPT", Translator.toLocale("INVALID_USER_PROFILE_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }
        UserProfile userProfile = userProfileRepository.findById(id).get();

        return userProfile;
    }

}

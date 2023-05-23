package com.loits.comn.auth.commons;

import org.apache.commons.beanutils.BeanUtilsBean;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;

public class NullAwareBeanUtilsBean extends BeanUtilsBean {

  private HashSet<String> ignoreFields;

  @Override
  public void copyProperty(Object dest, String name, Object value) throws IllegalAccessException, InvocationTargetException {
    if (value == null) return;
    if(ignoreFields!=null && ignoreFields.contains(name)){
      return;
    }
    super.copyProperty(dest, name, value);
  }

  public void setIgnoreFields(HashSet<String> ignoreFields){
    this.ignoreFields=ignoreFields;
  }
}

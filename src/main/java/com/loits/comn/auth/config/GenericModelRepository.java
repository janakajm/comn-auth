package com.loits.comn.auth.config;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.StringPath;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.QuerydslJpaRepository;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

/**
 * Adding Global sorting methods via QueryDslBinerCustoizer.
 * <p>
 * If you want to customise sort method for any specific repository, please add customization
 * logic in the repository itself.
 * <p>
 * Use this configuration fore application wide customization.
 *
 * @author Lahiru Bandara - Infinitum360 | info@infinitum360.com
 * @version 1.0
 * @since 1.0
 */
public class GenericModelRepository<T, ID extends Serializable, S extends EntityPath<T>> extends QuerydslJpaRepository<T, ID>
        implements QuerydslBinderCustomizer<S> {

  public GenericModelRepository(
          JpaEntityInformation<T, ID> entityInformation,
          EntityManager entityManager) {
    super(entityInformation, entityManager);
  }

  public GenericModelRepository(
          JpaEntityInformation<T, ID> entityInformation,
          EntityManager entityManager,
          EntityPathResolver resolver) {
    super(entityInformation, entityManager, resolver);
  }

  @Override
  public void customize(QuerydslBindings bindings, S t) {
    bindings.bind(String.class).first((StringPath stringPath, String s) -> stringPath.containsIgnoreCase(s));

    bindings.bind(Date.class).first((DateTimePath<Date> datePath, Date d) -> {
      Calendar cal = Calendar.getInstance();
      cal.setTime(d);
      cal.add(Calendar.DATE, 1);
      return datePath.between(d, cal.getTime());
    });
  }
}


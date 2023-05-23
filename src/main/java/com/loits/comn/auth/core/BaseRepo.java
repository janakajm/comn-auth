/**
 * 
 */
package com.loits.comn.auth.core;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ranjith Kodikara
 *
 */
@Repository
@Transactional
public abstract class BaseRepo {
	@Autowired
	protected EntityManager em;

	public <T> void persist(T entity) {
		em.persist(entity);
	}

}

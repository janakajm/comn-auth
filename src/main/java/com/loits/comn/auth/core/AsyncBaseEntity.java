/*
 * Copyright (c) 2018. LOLC Technology Services Ltd.
 * Author: Ranjith Kodikara
 * Date: 12/12/18 10:45
 */

package com.loits.comn.auth.core;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.SequenceGenerator;

import lombok.Data;

/**
 * 
 * Inherit this class when you define entities... common_seq is a common
 * sequence and the numbers will be shared with different entities ensuring the
 * uniqueness <br>
 * Have id and version columns
 * 
 * @author ranjithk
 * @since 2018-12-20
 * @version 1.0
 * 
 */
@Data
@MappedSuperclass
public abstract class AsyncBaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "async_generator")
	@SequenceGenerator(name = "async_generator", sequenceName = "seq_async_task", allocationSize = 1)
	@Column(name = "id", nullable = false)
	protected Long id;

	protected AsyncBaseEntity() {
		id = null;
	}


}

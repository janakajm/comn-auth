package com.loits.comn.auth.dto;

import java.sql.Timestamp;



public class RoleDto {
	
	    private String name;
	    private String description;
	    private String search;
	    private String createdBy;
	    private Timestamp createdOn;
	    private Long version;
	    private Long id;
	    
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public String getSearch() {
			return search;
		}
		public void setSearch(String search) {
			this.search = search;
		}
		public String getCreatedBy() {
			return createdBy;
		}
		public void setCreatedBy(String createdBy) {
			this.createdBy = createdBy;
		}
		public Timestamp getCreatedOn() {
			return createdOn;
		}
		public void setCreatedOn(Timestamp createdOn) {
			this.createdOn = createdOn;
		}
		public Long getVersion() {
			return version;
		}
		public void setVersion(Long version) {
			this.version = version;
		}
		public Long getId() {
			return id;
		}
		public void setId(Long id) {
			this.id = id;
		}

}

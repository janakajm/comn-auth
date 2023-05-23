package com.loits.comn.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddPermission {

    @NotBlank(message = "name-Empty")
    @Pattern(regexp = "^[a-zA-Z:/-]*$", message = "name-noSymbolsNumbers")
    private String name;

    @NotBlank(message = "description-Empty")
    private String description;

    private Meta meta;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public class Meta{
        private String searchq;

		public String getSearchq() {
			return searchq;
		}

		public void setSearchq(String searchq) {
			this.searchq = searchq;
		}
    }

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

	public Meta getMeta() {
		return meta;
	}

	public void setMeta(Meta meta) {
		this.meta = meta;
	}

}

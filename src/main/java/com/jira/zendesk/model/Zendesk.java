package com.jira.zendesk.model;

import io.github.millij.poi.ss.model.annotations.Sheet;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Sheet
public class Zendesk {
	
	public String ticketOrganisationName;
	
	public String ticketId;
	
	public String ticketType;
	
	public String ticketCategory;
	
	public String ticketPriority;
	
	public String ticketStatus;
	
	public String ticketGroup;
	
	public String estimatedDeliveryDate;
	
	public String environment;
	
	public String ticketSubject;
	
	public String requesterName;

}
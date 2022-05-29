package com.jira.zendesk.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ZendeskJira {
	
	
	public String jiraId;
	
	public String assignee;

	public String status;
	
	public String summary;
	
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

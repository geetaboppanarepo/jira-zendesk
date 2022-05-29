package com.jira.zendesk.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.jira.zendesk.model.Zendesk;
import com.jira.zendesk.model.ZendeskJira;

import io.github.millij.poi.SpreadsheetReadException;
import io.github.millij.poi.ss.reader.XlsxReader;

@Service
public class ZendeskJiraClientService {

    private String jiraUrl = "https://jira.xxx.org/";
    private SearchRestClient searchRestClient;
	final static XlsxReader reader = new XlsxReader();
    
    public File createZendeskReport(String auth, MultipartFile file) throws IOException, SpreadsheetReadException {
    	auth = auth.replace("Basic ", "");
		byte[] base64decodedBytes = Base64.getDecoder().decode(auth);
		String[] userCred = new String(base64decodedBytes).split(":");
		String userName = userCred[0];
		String password = userCred[1];
    	List<Zendesk> zendeskTickets = getZendeskTickets(file);	    
    	searchRestClient = getSearchRestClient(userName, password);
        List<ZendeskJira> zendesk = new ArrayList<>();
        for(Zendesk zendeskTicket : zendeskTickets) {
        	String ticketId = zendeskTicket.getTicketId();
        	String query = "project = ADEPT and cf[12500] ~ " + ticketId;
            Iterable<Issue> issues = getIssues(query);
            ZendeskJira zen = new ZendeskJira();
            zen.setTicketId(ticketId);
            zen.setEnvironment(zendeskTicket.getEnvironment());
            zen.setTicketOrganisationName(zendeskTicket.getTicketOrganisationName());
            zen.setTicketSubject(zendeskTicket.getTicketSubject());
            zen.setRequesterName(zendeskTicket.getRequesterName());
            zen.setTicketType(zendeskTicket.getTicketType());            
            zen.setTicketCategory(zendeskTicket.getTicketCategory());
            zen.setTicketPriority(zendeskTicket.getTicketPriority());
            zen.setTicketStatus(zendeskTicket.getTicketStatus());
            zen.setTicketGroup(zendeskTicket.getTicketGroup());
            zen.setEstimatedDeliveryDate(zendeskTicket.getEstimatedDeliveryDate());
            zen.setEnvironment(zendeskTicket.getEnvironment());
            zen.setTicketSubject(zendeskTicket.getTicketSubject());
            
            if(issues != null) {
            	for (Issue issue : issues) {
            		zen.setJiraId((zen.getJiraId() == null) ? issue.getKey() : zen.getJiraId() + issue.getKey());
            		zen.setAssignee((zen.getJiraId() == null) ? "-" : issue.getAssignee().getDisplayName());
            		zen.setStatus(issue.getStatus().getName());
            		zen.setSummary(issue.getSummary());
                }
                zendesk.add(zen);
            }
  
        }
        return createZendeskReportWithJira(zendesk,"AdeptP&CZendesk.xlsx");
    }
   
	public File createZendeskReportWithJira(List<ZendeskJira> zendeskJiraList, String fileName) throws IOException {
		Files.deleteIfExists(new File(fileName).toPath());
		zendeskJiraList = zendeskJiraList.stream()
		        .sorted(Comparator.comparing(ZendeskJira::getTicketId))
		        .collect(Collectors.toList());

		XSSFWorkbook wb = new XSSFWorkbook();
		SXSSFWorkbook workbook = new SXSSFWorkbook(wb, -1, Boolean.FALSE, Boolean.TRUE);
		SXSSFSheet sheet = workbook.createSheet("Sheet1");
		//List<String> fileHeaders = getReportHeaders();
		List<String> fileHeaders = getCustomReportHeaders(); 
		
		createReportHeaders(sheet, fileHeaders);
		try {
			for (int i = 0; i < zendeskJiraList.size(); i++) {
				//createReportData(sheet, zendeskJiraList.get(i), i + 1, fileHeaders.size());
				createCustomReportData(sheet, zendeskJiraList.get(i), i + 1, fileHeaders.size());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		File file = new File(fileName);
		FileOutputStream fileOut = null;
		try {
			fileOut = new FileOutputStream(file);
			workbook.write(fileOut);
			fileOut.close();
			workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return file;
	}
	
	public static Row createReportHeaders(SXSSFSheet sheet, List<String> fileHeaders) {
		Row rowHead = sheet.createRow((short) 0);
		for (int i = 0; i < fileHeaders.size(); i++) {
			rowHead.createCell(i).setCellValue(fileHeaders.get(i));
		}
		return rowHead;
	}
	
    
    public static List<String> getReportHeaders() {
    	return Arrays.asList("Ticket Id","Jira#","Assignee","Status","Ticket Subject", "Ticket Org Name","Environment","Requester Name",
    			"TicketType", "TicketCategory", "TicketPriority", "TicketStatus", "TicketGroup", "EstimatedDate");
    }
    
    public static List<String> getCustomReportHeaders() {
    	return Arrays.asList("Ticket Id","Ticket Subject", "Jira#","Jira Summary","Ticket Org Name","Environment","Requester Name",
    			"TicketCategory", "EstimatedDate");
    }
    
    public static Row createCustomReportData(SXSSFSheet sheet, ZendeskJira zendeskJira, int rowIndex, int columnSize) {
		SXSSFRow row = sheet.createRow(rowIndex);
		row.createCell(0).setCellValue(zendeskJira.getTicketId());
		row.createCell(1).setCellValue(zendeskJira.getTicketSubject());
		row.createCell(2).setCellValue(zendeskJira.getJiraId());
		row.createCell(3).setCellValue(zendeskJira.getSummary());	
		row.createCell(4).setCellValue(zendeskJira.getTicketOrganisationName());
		row.createCell(5).setCellValue(zendeskJira.getEnvironment());
		row.createCell(6).setCellValue(zendeskJira.getRequesterName());				
		row.createCell(7).setCellValue(zendeskJira.getTicketCategory());
		row.createCell(8).setCellValue(zendeskJira.getEstimatedDeliveryDate());
		return row;
	}
	
	public static Row createReportData(SXSSFSheet sheet, ZendeskJira zendeskJira, int rowIndex, int columnSize) {
		SXSSFRow row = sheet.createRow(rowIndex);
		row.createCell(0).setCellValue(zendeskJira.getTicketId());
		row.createCell(1).setCellValue(zendeskJira.getJiraId());
		row.createCell(2).setCellValue(zendeskJira.getAssignee());	
		row.createCell(3).setCellValue(zendeskJira.getStatus());	
		row.createCell(4).setCellValue(zendeskJira.getTicketSubject()); 
		row.createCell(5).setCellValue(zendeskJira.getTicketOrganisationName());
		row.createCell(6).setCellValue(zendeskJira.getEnvironment());
		row.createCell(7).setCellValue(zendeskJira.getRequesterName());		
		
		row.createCell(8).setCellValue(zendeskJira.getTicketType());
		row.createCell(9).setCellValue(zendeskJira.getTicketCategory());
		row.createCell(10).setCellValue(zendeskJira.getTicketPriority());	
		row.createCell(11).setCellValue(zendeskJira.getTicketStatus());	
		row.createCell(12).setCellValue(zendeskJira.getTicketGroup());
		row.createCell(13).setCellValue(zendeskJira.getEstimatedDeliveryDate());
		
		return row;
	}

    private Iterable<Issue> getIssues(String query) { 
    	SearchResult result = searchRestClient.searchJql(query, 50, 0, null).claim();
    	return result.getIssues();
        
    } 

    private JiraRestClient getJiraRestClient(String userName, String password) {
        return new AsynchronousJiraRestClientFactory()
          .createWithBasicHttpAuthentication(getJiraUri(), userName, password);
    }
    
    private SearchRestClient getSearchRestClient(String userName, String password) {
        return getJiraRestClient(userName, password).getSearchClient();
    }

    private URI getJiraUri() {
        return URI.create(this.jiraUrl);
    }
    
    public static List<Zendesk> getZendeskTickets(MultipartFile file) throws SpreadsheetReadException, IOException {
		return reader.read(Zendesk.class, file.getInputStream());
	}
}

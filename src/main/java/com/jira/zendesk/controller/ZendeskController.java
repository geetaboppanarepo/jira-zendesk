package com.jira.zendesk.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.jira.zendesk.service.ZendeskJiraClientService;

import io.github.millij.poi.SpreadsheetReadException;

@Controller
@Slf4j
public class ZendeskController {
  
  @Autowired
  ZendeskJiraClientService zendeskJiraClientService; 
  
  @PostMapping("/report/{filename:.+}")
  public ResponseEntity<Resource> zendeskReportWithJiraDetails(@RequestHeader("Authorization") String auth,
		  @RequestParam("file") MultipartFile file) {
		log.info("Entering Zendesk Report controller");
		Path path;
		ByteArrayResource resource = null;
		try {
			path = Paths.get(zendeskJiraClientService.createZendeskReport(auth, file).getAbsolutePath());
			resource = new ByteArrayResource(Files.readAllBytes(path));
		} catch (IOException | SpreadsheetReadException e) {
			e.printStackTrace();
		}
	    log.info("Exiting Zendesk Report controller");
		return ResponseEntity.ok()
	               .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() )
	               .body(resource);
  }


}

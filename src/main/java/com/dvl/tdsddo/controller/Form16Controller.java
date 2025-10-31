package com.dvl.tdsddo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dvl.tdsddo.request.Form16UploadRequest;
import com.dvl.tdsddo.request.FormUploadRequest;
import com.dvl.tdsddo.response.FileDownloadResponse;
import com.dvl.tdsddo.service.Form16Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/tds/form16")
public class Form16Controller {

	@Autowired
	private Form16Service form16Service;

	@PostMapping("/addForm16ByDDO")
	public Map<String, Object> addForm16ByDDO(@RequestPart String requestData, @RequestPart MultipartFile file) {
		return form16Service.uploadForm16(requestData, file);
	}

	@GetMapping("/getLastThreeYearsForm16/{panNumber}")
	public Map<String, Object> getLastThreeYearsForm16(@PathVariable String panNumber) {
		return form16Service.getLastThreeYearsForm16(panNumber);
	}

	@GetMapping("/generateDownloadLink/{fileName}")
	public Map<String, Object> generateDownloadLink(@PathVariable String fileName) {
		return form16Service.generateDownloadLink(fileName);
	}

	@GetMapping("/downloadPDF/{fileName}")
	public ResponseEntity<Resource> downloadForm16PDF(@PathVariable String fileName) {
		return form16Service.downloadPDF(fileName);
	}

	@GetMapping("/getForm16Links")
	public ResponseEntity<List<FileDownloadResponse>> getDownloadLinks(@RequestParam String financialYear,
			@RequestParam String panNumber) {

		List<FileDownloadResponse> responseList = form16Service.getForm16Files(financialYear, panNumber);

		if (responseList.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseList);
		}

		return ResponseEntity.ok(responseList);
	}

	@GetMapping("/downloadPDF/{year}/{fileName:.+}")
	public ResponseEntity<Resource> downloadPDF(@PathVariable String year, @PathVariable String fileName) {
		String fullFileName = year + "/" + fileName;

		return form16Service.downloadPDFNew(fullFileName);
	}

	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Map<String, Object> uploadFormFiles(@RequestPart("formData") String formDataJson,
			@RequestPart("files") MultipartFile files) {

		ObjectMapper objectMapper = new ObjectMapper();
		FormUploadRequest formData;

		try {
			formData = objectMapper.readValue(formDataJson, FormUploadRequest.class);
		} catch (JsonProcessingException e) {
			return Map.of("status", "error", "message", "Invalid formData JSON: " + e.getMessage());
		}

		return form16Service.uploadForm16FilesWithZipFolder(formData.getFinancialYear(), formData.getTanNumber(),
				formData.getType(), files);
	}

	@PostMapping("/getUploadedForms")
	public Map<String, Object> getUploadedForms(@RequestBody Form16UploadRequest request) {
		return form16Service.getUploadedFormNames(request.getFinancialYear(), request.getTanNumber(),
				request.getFormType());
	}

	@PostMapping("/download")
	public ResponseEntity<Resource> downloadFormsAsZip(@RequestBody Form16UploadRequest request) {
		return form16Service.downloadFormsAsZip(request.getFinancialYear(), request.getTanNumber(),
				request.getFormType(), request.getFileNames());
	}

	@PostMapping("/searchByPan")
	public ResponseEntity<Map<String, Object>> searchByPanNumber(@RequestBody Form16UploadRequest req) {

		Map<String, Object> result = form16Service.searchFilesByPan(req.getFinancialYear(), req.getTanNumber(),
				req.getFormType(), req.getPanNumber());
		return ResponseEntity.ok(result);
	}

	@PostMapping(value = "/uploadFormFilesWithZIPFolder", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Map<String, Object> uploadFormFilesWithZIPFolder(@RequestPart("formData") String formDataJson,
			@RequestPart("zipFile") MultipartFile file) {

		ObjectMapper objectMapper = new ObjectMapper();
		FormUploadRequest formData;

		try {
			formData = objectMapper.readValue(formDataJson, FormUploadRequest.class);
		} catch (JsonProcessingException e) {
			return Map.of("status", "error", "message", "Invalid formData JSON: " + e.getMessage());
		}

		return form16Service.uploadForm16FilesWithZipFolder(formData.getFinancialYear(), formData.getTanNumber(),
				formData.getType(), file);
	}
}

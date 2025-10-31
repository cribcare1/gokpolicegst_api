package com.dvl.tdsddo.service;

import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.dvl.tdsddo.response.FileDownloadResponse;

@Service
public interface Form16Service {
	public Map<String, Object> uploadForm16(String form16Request, MultipartFile file);

	public Map<String, Object> getLastThreeYearsForm16(String panNumber);

	public Map<String, Object> generateDownloadLink(String fileName);

	public ResponseEntity<Resource> downloadPDF(String fileName);

	public List<FileDownloadResponse> getForm16Files(String financialYear, String panNumber);

	public ResponseEntity<Resource> downloadPDFNew(String fileName);

	Map<String, Object> uploadForm16Files(String financialYear, String tanNumber, String type, MultipartFile[] files);

	public Map<String, Object> getUploadedFormNames(String financialYear, String tanNumber, String type);

	public ResponseEntity<Resource> downloadFormsAsZip(String financialYear, String tanNumber, String type,
			List<String> fileNames);

	public Map<String, Object> searchFilesByPan(String financialYear, String tanNumber, String formType,
			String panNumber);

	public Map<String, Object> uploadForm16FilesWithZipFolder(String financialYear, String tanNumber, String type,
			MultipartFile zipFile);

}

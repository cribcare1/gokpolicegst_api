package com.dvl.tdsddo.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;

@Service
public class FileServiceUtil {
	@Value("${folder.user.document.path}")
	private String documentPath;

	@Value("${folder.user.document.financialYear}")
	private String financialYearPath;

	public String uploadFile(MultipartFile file, String fileType) throws IOException {
		String fileName = file.getOriginalFilename();
		if (fileType.equals(TdsDdoConstant.GST)) {
			// Get current date and time
			LocalDateTime now = LocalDateTime.now();

			// Format the date and time in a readable and filesystem-safe format
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
			fileName = now.format(formatter) + "_" + fileName;
			String storePath = documentPath.concat(fileName);
			Long res = Files.copy(file.getInputStream(), Paths.get(storePath));
			if (res != 0) {
				return fileName;
			} else
				return null;
		}
		return null;
	}

	public String uploadFile(MultipartFile file, String name, String panNumber, String financialYear)
			throws IOException {
		String extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
		LocalDateTime now = LocalDateTime.now();

		// Format the date and time in a readable and filesystem-safe format
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
		String fileName = now.format(formatter) + "_" + name + "_" + panNumber + "_" + financialYear + extension;

		String storePath = documentPath.concat(fileName);
		Long res = Files.copy(file.getInputStream(), Paths.get(storePath));

		if (res != 0) {
			return fileName; // Return the saved file name
		} else {
			return null;
		}
	}

//TODO This method is in progress.
	public byte[] fetchImage(String imageName, String fileType) throws IOException {
		// Determine the base directory based on the file type
		String basePath;
		switch (fileType) {
		case TdsDdoConstant.FORM16, TdsDdoConstant.GST:
			basePath = documentPath;
			break;
            default:
			throw new IllegalArgumentException("Invalid file type: " + fileType);
		}

		// Construct the full file path
		String filePath = Paths.get(basePath, imageName).toString();

		// Log the file path for debugging
		System.out.println("Fetching file from path: " + filePath);

		// Check if the file exists
		File file = new File(filePath);
		if (!file.exists()) {
			throw new IOException("File not found: " + filePath);
		}

		// Read and return the file's contents as a byte array
		return Files.readAllBytes(file.toPath());
	}

    public Resource fetchImages(String folderName, String fileName) {
        if (folderName == null || folderName.isBlank() || fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Folder name and file name must be provided.");
        }

        String folderPath = resolveFolderPath(folderName);

        File file = new File(folderPath, fileName);
        if (!file.exists()) {
            throw new RuntimeException("File not found: " + file.getAbsolutePath());
        }

        return new FileSystemResource(file);
    }


    private String resolveFolderPath(String folderName) {
        return switch (folderName.toLowerCase()) {
            case "gst" -> documentPath;

            default -> throw new IllegalArgumentException("Invalid folder name: " + folderName);
        };
    }

}

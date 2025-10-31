package com.dvl.tdsddo.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class FolderCreatorBean {
	@Value("${folder.user.document.path}")
	private String documentPath;

	@Value("${folder.user.document.financialYear}")
	private String financialYearPath;

	@PostConstruct
	public void createFolders() {
		createFolder(documentPath);
		createFolder(financialYearPath);
	}

	private void createFolder(String folderPath) {
		File folder = new File(folderPath);
		if (!folder.exists()) {
			folder.mkdirs();
		}
	}

	public void createRemoteFolder(String server, int port, String user, String pass, String remotePath)
			throws IOException {
		FTPClient ftp = new FTPClient();
		try {
			ftp.connect(server, port);
			ftp.login(user, pass);
			ftp.enterLocalPassiveMode(); // important for many FTP servers

			String[] folders = remotePath.split("/");
			String path = "";
			for (String folder : folders) {
				if (folder.isEmpty())
					continue;
				path += "/" + folder;
				ftp.makeDirectory(path);
			}
		} finally {
			if (ftp.isConnected()) {
				ftp.logout();
				ftp.disconnect();
			}
		}
	}

	public void uploadFileToFTP(String server, int port, String user, String pass, String remoteDir, String fileName,
			InputStream inputStream) throws IOException {
		FTPClient ftp = new FTPClient();
		try {
			ftp.connect(server, port);
			ftp.login(user, pass);
			ftp.enterLocalPassiveMode();
			ftp.setFileType(FTP.BINARY_FILE_TYPE);

// Change to remote directory
			ftp.changeWorkingDirectory(remoteDir);
			ftp.storeFile(fileName, inputStream);
		} finally {
			if (ftp.isConnected()) {
				ftp.logout();
				ftp.disconnect();
			}
			inputStream.close();
		}
	}

}

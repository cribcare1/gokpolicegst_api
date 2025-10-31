package com.dvl.tdsddo;

import java.security.Security;
import java.util.Set;

public class KeystoreTest {
	public static void main(String[] args) {
		Set<String> keystoreTypes = Security.getAlgorithms("KeyStore");
		System.out.println("Supported keystore types: " + keystoreTypes);
	}
}

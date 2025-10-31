package com.dvl.tdsddo.security;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class AuthorityUtil {
	public static List<GrantedAuthority> convertRolesToAuthorities(List<String> roleNames) {
		return roleNames.stream().filter(roleName -> roleName != null && !roleName.trim().isEmpty())
				.map(roleName -> new SimpleGrantedAuthority("ROLE_" + roleName)).collect(Collectors.toList());
	}
}

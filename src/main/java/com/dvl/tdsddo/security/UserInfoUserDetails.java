package com.dvl.tdsddo.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.dvl.tdsddo.model.User;

public class UserInfoUserDetails implements UserDetails {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String userName;
	private String passwordString;
	private List<GrantedAuthority> authorities;

	public UserInfoUserDetails(User user) {
		userName = user.getUserName();
		passwordString = user.getPassword();
		List<String> roles = Arrays.asList(user.getRole());

		authorities = AuthorityUtil.convertRolesToAuthorities(roles);

	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return passwordString;
	}

	@Override
	public String getUsername() {
		return userName;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

}
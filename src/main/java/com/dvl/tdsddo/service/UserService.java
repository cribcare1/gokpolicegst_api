package com.dvl.tdsddo.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.dvl.tdsddo.model.User;
import com.dvl.tdsddo.request.AuthRequest;
import com.dvl.tdsddo.request.EditDDORequest;
import com.dvl.tdsddo.response.DashBoardresponse;

@Service
public interface UserService {
	public Map<String, Object> addAdmin(User user);

	public Map<String, Object> getAllActiveDDOs(Integer adminId, int page, int size);

	public Map<String, Object> getAllActiveDDOs();

	public Map<String, Object> addDDO(User user, Integer adminId);

	public Map<String, Object> createUserWithAdminCheck(User user, Integer adminId);

	public Map<String, Object> loginUsingUserNamePassword(AuthRequest request);

	public Map<String, Object> viewDdoDetailsUsingId(Integer ddoId);

	public DashBoardresponse viewDashBoard(String tanNumber);

	public Map<String, Object> editDdoDetails(Integer ddoId, EditDDORequest request);
}

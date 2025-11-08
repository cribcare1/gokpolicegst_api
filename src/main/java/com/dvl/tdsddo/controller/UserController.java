package com.dvl.tdsddo.controller;

import java.util.Map;

import com.dvl.tdsddo.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dvl.tdsddo.request.EditDDORequest;
import com.dvl.tdsddo.request.UserSignUpRequest;
import com.dvl.tdsddo.response.DashBoardresponse;
import com.dvl.tdsddo.service.UserService;

@RestController
@RequestMapping("/tds/user")
public class UserController {
	@Autowired
	private UserService userService;

	@PostMapping("/addDDOByGSTIN")
	public Map<String, Object> addDDOByAdmin(@RequestBody UserSignUpRequest user) {
		return userService.createUserWithAdminCheck(user.getUser(), user.getAdminId());
	}

	@GetMapping("/getAllDDOByAdmin")
	public Map<String, Object> getAllDDOByAdmin(@RequestParam Integer adminId,
			@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "10") Integer size) {
		return userService.getAllActiveDDOs(adminId, page, size);
	}

	@GetMapping("/getAllDDOByAdminForDropDown")
	public Map<String, Object> getAllDDOByAdmin() {
		return userService.getAllActiveDDOs();
	}

	@GetMapping("/viewDDODetailsUsingId/{ddoId}")
	public Map<String, Object> viewDDODetailsUsingId(@PathVariable Integer ddoId) {
		return userService.viewDdoDetailsUsingId(ddoId);
	}

	@GetMapping("/viewDashboardData")
	public DashBoardresponse viewDDODetailsUsingId(@RequestParam(required = false) String tanNumber) {

		DashBoardresponse response = userService.viewDashBoard(tanNumber);
		return response;
	}

	@PostMapping("/editDdo/{ddoId}")
	public ResponseEntity<Map<String, Object>> editDdo(@PathVariable Integer ddoId,
			@RequestBody EditDDORequest request) {
		Map<String, Object> response = userService.editDdoDetails(ddoId, request);
		return ResponseEntity.ok(response);
	}


        @PostMapping("/editAdmin")
    public ResponseEntity<Map<String, Object>> editAdmin(
                                                       @RequestBody User request) {
        Map<String, Object> response = userService.editAdmin( request);
        return ResponseEntity.ok(response);
    }

}

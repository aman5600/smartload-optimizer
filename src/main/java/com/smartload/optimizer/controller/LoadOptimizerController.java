package com.smartload.optimizer.controller;

import com.smartload.optimizer.dto.OptimizeLoadRequest;
import com.smartload.optimizer.dto.OptimizeLoadResponse;
import com.smartload.optimizer.service.LoadOptimizerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/load-optimizer")
public class LoadOptimizerController {
	private final LoadOptimizerService loadOptimizerService;

	public LoadOptimizerController(LoadOptimizerService loadOptimizerService) {
		this.loadOptimizerService = loadOptimizerService;
	}

	@PostMapping("/optimize")
	public ResponseEntity<OptimizeLoadResponse> optimize(@Valid @RequestBody OptimizeLoadRequest request) {
		return ResponseEntity.ok(loadOptimizerService.optimize(request));
	}
}

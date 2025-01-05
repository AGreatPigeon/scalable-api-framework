package com.example.api.controller;

import com.example.api.model.ApiRequest;
import com.example.api.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private ApiService apiService;

    @GetMapping("/request/{requestId}")
    public ApiRequest getRequest(@PathVariable String requestId){
        return apiService.getRequest(requestId);
    }

    @PostMapping("/request")
    public void saveRequest(@RequestBody ApiRequest apiRequest){
        apiService.saveRequest(apiRequest);
    }
}

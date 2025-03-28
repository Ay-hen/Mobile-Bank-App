package com.example.demo.controller;

import com.example.demo.dto.BranchDto;
import com.example.demo.model.Branch;
import com.example.demo.repository.BranchRepo;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/branch")
public class BranchController {
    @Autowired
    private BranchRepo branchRepo;

    @GetMapping("/get-all-branches")
    public ResponseEntity<List<BranchDto>> getAllBranches() {
        List<Branch> branches = branchRepo.findAll();
        
        if (branches.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        List<BranchDto> branchDtos = branches.stream()
                .map(branch -> BranchDto.builder()
                        .id(branch.getBranchId())
                        .name(branch.getBranchName())
                        .address(branch.getBranchAddress())
                        .code(branch.getBranchCode())
                        .build())
                .collect(Collectors.toList());

        return new ResponseEntity<>(branchDtos, HttpStatus.OK);
    }
}
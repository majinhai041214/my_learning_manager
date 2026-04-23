package com.majinhai.website.controller;

import com.majinhai.website.model.dto.ApiResponse;
import com.majinhai.website.model.dto.CheckInRequest;
import com.majinhai.website.model.dto.CheckInResponse;
import com.majinhai.website.model.dto.CheckInTagsUpdateRequest;
import com.majinhai.website.service.CheckInService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkins")
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @GetMapping
    public ApiResponse<List<CheckInResponse>> list() {
        return ApiResponse.success(
                "CHECKIN_LIST_OK",
                "获取打卡列表成功",
                checkInService.listAll()
        );
    }

    @PostMapping
    public ApiResponse<CheckInResponse> create(@Valid @RequestBody CheckInRequest request) {
        return ApiResponse.success(
                "CHECKIN_CREATED",
                "创建打卡记录成功",
                checkInService.create(request)
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<CheckInResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CheckInRequest request
    ) {
        return ApiResponse.success(
                "CHECKIN_UPDATED",
                "更新打卡记录成功",
                checkInService.update(id, request)
        );
    }

    @PutMapping("/{id}/tags")
    public ApiResponse<CheckInResponse> updateTags(
            @PathVariable Long id,
            @RequestBody CheckInTagsUpdateRequest request
    ) {
        return ApiResponse.success(
                "CHECKIN_TAGS_UPDATED",
                "更新打卡标签成功",
                checkInService.updateTags(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        checkInService.delete(id);
        return ApiResponse.success(
                "CHECKIN_DELETED",
                "删除打卡记录成功",
                null
        );
    }
}

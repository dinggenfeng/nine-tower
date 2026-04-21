package com.ansible.tag.controller;

import com.ansible.common.Result;
import com.ansible.tag.dto.CreateTagRequest;
import com.ansible.tag.dto.TagResponse;
import com.ansible.tag.dto.UpdateTagRequest;
import com.ansible.tag.service.TagService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TagController {

  private final TagService tagService;

  @PostMapping("/projects/{projectId}/tags")
  public Result<TagResponse> createTag(
      @PathVariable Long projectId,
      @Valid @RequestBody CreateTagRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.valueOf(userDetails.getUsername());
    return Result.success(tagService.createTag(projectId, request, userId));
  }

  @GetMapping("/projects/{projectId}/tags")
  public Result<List<TagResponse>> listTags(
      @PathVariable Long projectId, @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.valueOf(userDetails.getUsername());
    return Result.success(tagService.listTags(projectId, userId));
  }

  @PutMapping("/tags/{tagId}")
  public Result<TagResponse> updateTag(
      @PathVariable Long tagId,
      @Valid @RequestBody UpdateTagRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.valueOf(userDetails.getUsername());
    return Result.success(tagService.updateTag(tagId, request, userId));
  }

  @DeleteMapping("/tags/{tagId}")
  public Result<Void> deleteTag(
      @PathVariable Long tagId, @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.valueOf(userDetails.getUsername());
    tagService.deleteTag(tagId, userId);
    return Result.success();
  }
}

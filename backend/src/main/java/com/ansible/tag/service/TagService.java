package com.ansible.tag.service;

import com.ansible.security.ProjectAccessChecker;
import com.ansible.tag.dto.CreateTagRequest;
import com.ansible.tag.dto.TagResponse;
import com.ansible.tag.dto.UpdateTagRequest;
import com.ansible.tag.entity.Tag;
import com.ansible.tag.repository.TagRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TagService {

  private final TagRepository tagRepository;
  private final ProjectAccessChecker accessChecker;

  public TagResponse createTag(Long projectId, CreateTagRequest request, Long userId) {
    accessChecker.checkMembership(projectId, userId);
    if (tagRepository.existsByProjectIdAndName(projectId, request.name())) {
      throw new IllegalArgumentException(
          "Tag with name '" + request.name() + "' already exists in this project");
    }
    Tag tag = new Tag();
    tag.setProjectId(projectId);
    tag.setName(request.name());
    tag.setCreatedBy(userId);
    return new TagResponse(tagRepository.save(tag));
  }

  @Transactional(readOnly = true)
  public List<TagResponse> listTags(Long projectId, Long userId) {
    accessChecker.checkMembership(projectId, userId);
    return tagRepository.findByProjectIdOrderByIdAsc(projectId).stream()
        .map(TagResponse::new)
        .toList();
  }

  public TagResponse updateTag(Long tagId, UpdateTagRequest request, Long userId) {
    Tag tag =
        tagRepository
            .findById(tagId)
            .orElseThrow(() -> new IllegalArgumentException("Tag not found"));
    accessChecker.checkOwnerOrAdmin(tag.getProjectId(), tag.getCreatedBy(), userId);
    if (tagRepository.existsByProjectIdAndNameAndIdNot(
        tag.getProjectId(), request.name(), tagId)) {
      throw new IllegalArgumentException(
          "Tag with name '" + request.name() + "' already exists in this project");
    }
    tag.setName(request.name());
    return new TagResponse(tagRepository.save(tag));
  }

  public void deleteTag(Long tagId, Long userId) {
    Tag tag =
        tagRepository
            .findById(tagId)
            .orElseThrow(() -> new IllegalArgumentException("Tag not found"));
    accessChecker.checkOwnerOrAdmin(tag.getProjectId(), tag.getCreatedBy(), userId);
    tagRepository.delete(tag);
  }
}

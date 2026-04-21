package com.ansible.tag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.security.ProjectAccessChecker;
import com.ansible.tag.dto.CreateTagRequest;
import com.ansible.tag.dto.TagResponse;
import com.ansible.tag.dto.UpdateTagRequest;
import com.ansible.tag.entity.Tag;
import com.ansible.tag.repository.TagRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

  @Mock private TagRepository tagRepository;
  @Mock private ProjectAccessChecker accessChecker;

  @InjectMocks private TagService tagService;

  private Tag createTag(Long id, Long projectId, String name) {
    Tag tag = new Tag();
    ReflectionTestUtils.setField(tag, "id", id);
    tag.setProjectId(projectId);
    tag.setName(name);
    return tag;
  }

  @Test
  void createTag_success() {
    when(tagRepository.existsByProjectIdAndName(1L, "web")).thenReturn(false);
    when(tagRepository.save(any(Tag.class)))
        .thenAnswer(
            inv -> {
              Tag t = inv.getArgument(0);
              ReflectionTestUtils.setField(t, "id", 1L);
              return t;
            });

    TagResponse response = tagService.createTag(1L, new CreateTagRequest("web"), 100L);

    assertThat(response.name()).isEqualTo("web");
    verify(tagRepository).save(any(Tag.class));
  }

  @Test
  void createTag_duplicateName_throws() {
    when(tagRepository.existsByProjectIdAndName(1L, "web")).thenReturn(true);

    assertThatThrownBy(() -> tagService.createTag(1L, new CreateTagRequest("web"), 100L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void listTags_success() {
    when(tagRepository.findByProjectIdOrderByIdAsc(1L))
        .thenReturn(List.of(createTag(1L, 1L, "web"), createTag(2L, 1L, "db")));

    List<TagResponse> list = tagService.listTags(1L, 100L);

    assertThat(list).hasSize(2);
  }

  @Test
  void updateTag_success() {
    Tag tag = createTag(1L, 1L, "web");
    when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
    when(tagRepository.existsByProjectIdAndNameAndIdNot(1L, "api", 1L)).thenReturn(false);
    when(tagRepository.save(any(Tag.class))).thenReturn(tag);

    TagResponse response = tagService.updateTag(1L, new UpdateTagRequest("api"), 100L);

    assertThat(response.name()).isEqualTo("api");
  }

  @Test
  void updateTag_duplicateName_throws() {
    Tag tag = createTag(1L, 1L, "web");
    when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
    when(tagRepository.existsByProjectIdAndNameAndIdNot(1L, "db", 1L)).thenReturn(true);

    assertThatThrownBy(() -> tagService.updateTag(1L, new UpdateTagRequest("db"), 100L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void deleteTag_success() {
    Tag tag = createTag(1L, 1L, "web");
    when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));

    tagService.deleteTag(1L, 100L);

    verify(tagRepository).delete(tag);
  }

  @Test
  void deleteTag_notFound_throws() {
    when(tagRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tagService.deleteTag(999L, 100L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not found");
  }
}

package com.analoggpixel.nowinwiki.controller;

import com.analoggpixel.nowinwiki.parameter.dto.Result;
import com.analoggpixel.nowinwiki.parameter.folder.FolderCreateRequest;
import com.analoggpixel.nowinwiki.parameter.folder.FolderPatchRequest;
import com.analoggpixel.nowinwiki.parameter.folder.FolderVO;
import com.analoggpixel.nowinwiki.service.BookmarkFolderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Bookmark Folders")
@RestController
@RequestMapping("/api/v1/folders")
@RequiredArgsConstructor
public class BookmarkFolderController {

    private final BookmarkFolderService bookmarkFolderService;

    @Operation(summary = "新建收藏夹")
    @PostMapping
    public Result<FolderVO> create(@Valid @RequestBody FolderCreateRequest request) {
        return bookmarkFolderService.create(request);
    }

    @Operation(summary = "修改收藏夹")
    @PatchMapping("/{id}")
    public Result<FolderVO> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody FolderPatchRequest request
    ) {
        return bookmarkFolderService.update(id, request);
    }

    @Operation(summary = "删除收藏夹（软删，默认夹不可删）")
    @DeleteMapping("/{id}")
    public Result<FolderVO> delete(@PathVariable("id") Long id) {
        return bookmarkFolderService.delete(id);
    }
}

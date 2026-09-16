package com.analoggpixel.nowinwiki.service;

import com.analoggpixel.nowinwiki.parameter.dto.Result;
import com.analoggpixel.nowinwiki.parameter.folder.FolderCreateRequest;
import com.analoggpixel.nowinwiki.parameter.folder.FolderPatchRequest;
import com.analoggpixel.nowinwiki.parameter.folder.FolderVO;

public interface BookmarkFolderService {

    Result<FolderVO> create(FolderCreateRequest request);

    Result<FolderVO> update(Long folderId, FolderPatchRequest request);

    Result<FolderVO> delete(Long folderId);
}

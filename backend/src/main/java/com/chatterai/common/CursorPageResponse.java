package com.chatterai.common;

import lombok.Getter;

import java.util.List;

@Getter
public class CursorPageResponse<T> {

    private final List<T> content;
    private final boolean hasNext;
    private final Long nextCursorId;
    private final int size;

    public CursorPageResponse(List<T> content, boolean hasNext, Long nextCursorId) {
        this.content = content;
        this.hasNext = hasNext;
        this.nextCursorId = nextCursorId;
        this.size = content.size();
    }
}

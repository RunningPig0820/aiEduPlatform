package com.ai.edu.application.dto.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 通用分页响应（items + total + page + size），对齐前端分页契约。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageDTO<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<T> items;

    private Long total;

    private Integer page;

    private Integer size;
}

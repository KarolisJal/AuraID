package com.aura.auraid.dto;

import lombok.Data;
import java.util.List;

@Data
public class PageResponseDTO<T> {
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;
    private boolean first;
    
    public static <T> PageResponseDTO<T> of(List<T> content, int pageNumber, int pageSize, 
                                          long totalElements, int totalPages) {
        PageResponseDTO<T> response = new PageResponseDTO<>();
        response.setContent(content);
        response.setPageNumber(pageNumber);
        response.setPageSize(pageSize);
        response.setTotalElements(totalElements);
        response.setTotalPages(totalPages);
        response.setFirst(pageNumber == 0);
        response.setLast(pageNumber == totalPages - 1);
        return response;
    }
} 
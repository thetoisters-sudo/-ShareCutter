package com.sharecutter.backend.dto.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PagedResponse<T>(

        List<T> content,

        int page,

        int size,

        long totalElements,

        int totalPages,

        boolean first,

        boolean last,

        boolean hasNext,

        boolean hasPrevious

) {

    public static <T> PagedResponse<T> from(
            Page<T> source
    ) {
        return new PagedResponse<>(
                List.copyOf(source.getContent()),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.isFirst(),
                source.isLast(),
                source.hasNext(),
                source.hasPrevious()
        );
    }

    public static <S, T> PagedResponse<T> from(
            Page<S> source,
            Function<S, T> mapper
    ) {
        List<T> mappedContent =
                source.getContent()
                        .stream()
                        .map(mapper)
                        .toList();

        return new PagedResponse<>(
                mappedContent,
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.isFirst(),
                source.isLast(),
                source.hasNext(),
                source.hasPrevious()
        );
    }
}

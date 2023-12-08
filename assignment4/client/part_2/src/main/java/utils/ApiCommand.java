package utils;

import io.swagger.client.ApiResponse;

@FunctionalInterface
public interface ApiCommand<T> {
    ApiResponse<T> execute() throws Exception;
}


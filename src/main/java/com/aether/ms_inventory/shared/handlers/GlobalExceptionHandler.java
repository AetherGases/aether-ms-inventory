package com.aether.ms_inventory.shared.handlers;

import com.aether.ms_inventory.shared.exceptions.CoreApiException;
import com.aether.ms_inventory.shared.handlers.dto.output.ExceptionOutputDTO;
import com.aether.ms_inventory.shared.services.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
  private final MessageService messageService;
  @ExceptionHandler(CoreApiException.class)
  public ResponseEntity<ExceptionOutputDTO> handleCoreApiException(CoreApiException ex){
    return new ResponseEntity<>(
        new ExceptionOutputDTO(
            messageService.getMessage(ex.getMessage()),
            ex.getStatus()
        ),
        ex.getStatus()
    );
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ExceptionOutputDTO> handleNoResourceFoundException(NoResourceFoundException ex){
    return new ResponseEntity<>(
        new ExceptionOutputDTO(
            messageService.getMessage("exception.route.not-found"),
            HttpStatus.NOT_FOUND
        ),
        HttpStatus.NOT_FOUND
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ExceptionOutputDTO> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException ex
  ) {
    List<String> errors = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(fieldError -> {

          if (fieldError.getCode() != null
              && fieldError.getCode().equals("typeMismatch")) {

            Class<?> requiredType = ex.getBindingResult()
                .getFieldType(fieldError.getField());

            String allowedValues = Arrays.stream(requiredType.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", "));

            return messageService.getMessage(
                "exception.invalid.enum",
                fieldError.getField(),
                fieldError.getRejectedValue(),
                allowedValues
            );
          }

          return fieldError.getDefaultMessage();
        })
        .toList();

    return new ResponseEntity<>(
        new ExceptionOutputDTO(
            String.join("\n", errors),
            HttpStatus.BAD_REQUEST
        ),
        HttpStatus.BAD_REQUEST
    );
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ExceptionOutputDTO> handleMethodArgumentTypeMismatch(
      MethodArgumentTypeMismatchException exception
  ) {

    if (exception.getRequiredType() != null
        && exception.getRequiredType().isEnum()) {

      String field = exception.getName();
      String invalidValue = String.valueOf(exception.getValue());

      String allowedValues = Arrays.stream(
              exception.getRequiredType().getEnumConstants()
          )
          .map(Object::toString)
          .collect(Collectors.joining(", "));

      String message = messageService.getMessage(
          "exception.invalid.enum",
          field,
          invalidValue,
          allowedValues
      );

      return ResponseEntity.badRequest().body(
          new ExceptionOutputDTO(
              message,
              HttpStatus.BAD_REQUEST
          )
      );
    }

    return ResponseEntity.badRequest().body(
        new ExceptionOutputDTO(
            messageService.getMessage(
                "exception.invalid.request.parameter"
            ),
            HttpStatus.BAD_REQUEST
        )
    );
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ExceptionOutputDTO> handleAuthenticationException(AuthenticationException ex){
    return new ResponseEntity<>(
        new ExceptionOutputDTO(
            ex.getMessage(),
            HttpStatus.FORBIDDEN
        ),
        HttpStatus.FORBIDDEN
    );
  }
}

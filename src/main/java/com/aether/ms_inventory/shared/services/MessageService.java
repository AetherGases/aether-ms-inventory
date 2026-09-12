package com.aether.ms_inventory.shared.services;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MessageService {

  private final MessageSource messageSource;

  public String getMessage(String key) {
    Locale locale = LocaleContextHolder.getLocale();

    return messageSource.getMessage(key, null, locale);
  }

  public String getMessage(String key, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();

    return messageSource.getMessage(key, args, locale);
  }
}
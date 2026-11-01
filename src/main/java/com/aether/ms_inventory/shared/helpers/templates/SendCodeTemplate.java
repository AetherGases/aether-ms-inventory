package com.aether.ms_inventory.shared.helpers.templates;

import com.aether.ms_inventory.shared.helpers.interfaces.BrevoTemplate;

import java.util.Map;

public class SendCodeTemplate implements BrevoTemplate {

  private final String name;
  private final String code;

  public SendCodeTemplate(String name, String code) {
    this.name = name;
    this.code = code;
  }
  @Override
  public int template() {
    return 1;
  }

  @Override
  public Map<String, Object> params() {
    return Map.of(
        "name", this.name,
        "code", this.code
    );
  }
}

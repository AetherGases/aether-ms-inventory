package com.aether.ms_inventory.worker;

import com.aether.ms_inventory.shared.exceptions.InventoryProcessingException;
import com.aether.ms_inventory.shared.services.XlsxMarkdownConverter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("XlsxMarkdownConverter Tests")
class XlsxMarkdownConverterTests {

  private final XlsxMarkdownConverter converter = new XlsxMarkdownConverter(400_000);

  private byte[] workbook(java.util.function.Consumer<XSSFWorkbook> fill) throws IOException {
    try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      fill.accept(wb);
      wb.write(out);
      return out.toByteArray();
    }
  }

  @Test
  @DisplayName("Should convert each sheet into a markdown table")
  void convertsSheetToTable() throws IOException {
    byte[] file = workbook(wb -> {
      Sheet sheet = wb.createSheet("Escopo 1");
      Row header = sheet.createRow(0);
      header.createCell(0).setCellValue("Fonte");
      header.createCell(1).setCellValue("Qtd");
      Row row = sheet.createRow(1);
      row.createCell(0).setCellValue("Diesel | gerador");
      row.createCell(1).setCellValue(10.5);
    });

    String markdown = converter.convert(file);

    assertTrue(markdown.contains("## Escopo 1"));
    assertTrue(markdown.contains("| Fonte | Qtd |"));
    assertTrue(markdown.contains("| --- | --- |"));
    assertTrue(markdown.contains("| Diesel \\| gerador | 10.5 |"));
  }

  @Test
  @DisplayName("Should skip blank rows, empty sheets and evaluate formulas")
  void skipsBlankAndEvaluatesFormulas() throws IOException {
    byte[] file = workbook(wb -> {
      wb.createSheet("Vazia");
      Sheet sheet = wb.createSheet("Dados");
      sheet.createRow(0).createCell(0).setCellValue("Total");
      sheet.createRow(1);
      sheet.createRow(2).createCell(0).setCellFormula("1+2");
    });

    String markdown = converter.convert(file);

    assertFalse(markdown.contains("Vazia"));
    assertTrue(markdown.contains("| Total |"));
    assertTrue(markdown.contains("| 3 |"));
  }

  @Test
  @DisplayName("Should fail without retry when the file is not a valid xlsx")
  void invalidFile() {
    InventoryProcessingException ex = assertThrows(
        InventoryProcessingException.class,
        () -> converter.convert("não sou um xlsx".getBytes())
    );

    assertFalse(ex.isRetryable());
  }

  @Test
  @DisplayName("Should fail when the markdown exceeds the configured limit")
  void tooLarge() throws IOException {
    byte[] file = workbook(wb -> {
      Sheet sheet = wb.createSheet("Dados");
      for (int i = 0; i < 50; i++) {
        sheet.createRow(i).createCell(0).setCellValue("linha de teste " + i);
      }
    });

    InventoryProcessingException ex = assertThrows(
        InventoryProcessingException.class,
        () -> new XlsxMarkdownConverter(100).convert(file)
    );

    assertFalse(ex.isRetryable());
  }
}

package com.aether.ms_inventory.shared.services;

import com.aether.ms_inventory.shared.exceptions.InventoryProcessingException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class XlsxMarkdownConverter {

  private final int maxChars;

  public XlsxMarkdownConverter(
      @Value("${inventory.worker.max-markdown-chars:400000}") int maxChars
  ) {
    this.maxChars = maxChars;
  }

  public String convert(byte[] xlsx) {
    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(xlsx))) {
      DataFormatter formatter = new DataFormatter(Locale.US);
      FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
      StringBuilder markdown = new StringBuilder();

      for (Sheet sheet : workbook) {
        List<List<String>> rows = readRows(sheet, formatter, evaluator);

        if (rows.isEmpty()) {
          continue;
        }

        markdown.append("## ").append(sheet.getSheetName().replace('\n', ' ')).append("\n\n");
        appendTable(markdown, rows);
        markdown.append('\n');

        if (markdown.length() > maxChars)
          throw new InventoryProcessingException(
              "Planilha muito grande para ser processada", false
          );
      }

      if (markdown.isEmpty())
        throw new InventoryProcessingException("A planilha enviada está vazia", false);


      return markdown.toString().stripTrailing();
    } catch (IOException | RuntimeException e) {
      if (e instanceof InventoryProcessingException ex)
        throw ex;

      throw new InventoryProcessingException(
          "Não foi possível ler o arquivo .xlsx", e, false
      );
    }
  }

  private List<List<String>> readRows(
      Sheet sheet,
      DataFormatter formatter,
      FormulaEvaluator evaluator
  ) {
    List<List<String>> rows = new ArrayList<>();
    int maxColumns = 0;

    for (Row row : sheet) {
      short lastCell = row.getLastCellNum();

      if (lastCell < 0) {
        continue;
      }

      List<String> values = new ArrayList<>(lastCell);
      int lastFilled = -1;

      for (int i = 0; i < lastCell; i++) {
        Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        String value = cell == null ? "" : format(cell, formatter, evaluator);

        if (!value.isEmpty()) {
          lastFilled = i;
        }

        values.add(value);
      }

      if (lastFilled < 0) {
        continue;
      }

      maxColumns = Math.max(maxColumns, lastFilled + 1);
      rows.add(values);
    }

    List<List<String>> normalized = new ArrayList<>(rows.size());

    for (List<String> row : rows) {
      List<String> padded = new ArrayList<>(maxColumns);

      for (int i = 0; i < maxColumns; i++) {
        padded.add(i < row.size() ? row.get(i) : "");
      }

      normalized.add(padded);
    }

    return normalized;
  }

  private String format(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
    String value;

    try {
      value = formatter.formatCellValue(cell, evaluator);
    } catch (RuntimeException e) {
      value = formatter.formatCellValue(cell);
    }

    return value
        .replace("\r\n", " ")
        .replace('\n', ' ')
        .replace('\r', ' ')
        .replace("|", "\\|")
        .trim();
  }

  private void appendTable(StringBuilder markdown, List<List<String>> rows) {
    int columns = rows.get(0).size();

    appendRow(markdown, rows.get(0));

    markdown.append('|');
    for (int i = 0; i < columns; i++) {
      markdown.append(" --- |");
    }
    markdown.append('\n');

    for (int i = 1; i < rows.size(); i++) {
      appendRow(markdown, rows.get(i));
    }
  }

  private void appendRow(StringBuilder markdown, List<String> row) {
    markdown.append('|');

    for (String cell : row) {
      markdown.append(' ').append(cell).append(" |");
    }

    markdown.append('\n');
  }
}

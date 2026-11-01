package com.aether.ms_inventory.worker;

import com.aether.ms_inventory.shared.services.InventoryGenerationSpreadsheetFiller;
import com.aether.ms_inventory.shared.workers.contexts.InventoryOutputContext;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InventoryGenerationSpreadsheetFiller Tests")
public class InventoryGenerationSpreadsheetFillerTests {

  private final InventoryGenerationSpreadsheetFiller filler =
      new InventoryGenerationSpreadsheetFiller("templates/planilha-inscricao.xlsx");

  @Test
  @DisplayName("Should fill the complete declaration by gas and category, keeping biogenic CO2 apart")
  void shouldFillCompleteDeclaration() throws IOException {
    InventoryOutputContext context = new InventoryOutputContext(
        null,
        null,
        null,
        List.of(),
        List.of(
            cell("1.1", "GAS", "CO2", "10.5"),
            cell("1.1", "GAS", "CH4", "2"),
            cell("1.1", "CATEGORIA", "combustao_movel", "12.5"),
            cell("1.5", "BIOGENICO_EMISSAO", "ESCOPO_I", "4"),
            cell("1.2", "GAS", "CO2", "3"),
            cell("1.2", "CATEGORIA", "energia_termica", "3"),
            cell("1.5", "BIOGENICO_EMISSAO", "ESCOPO_II", "1.5"),
            cell("1.3", "GAS", "N2O", "7"),
            cell("1.3", "CATEGORIA", "transporte_distribuicao_downstream", "7"),
            cell("1.3", "BIOGENICO_EMISSAO", "viagens_a_negocios", "6"),
            cell("1.3", "CATEGORIA", "outras_escopo_iii", null)
        ),
        List.of()
    );

    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(filler.fill(context)))) {
      assertNull(workbook.getSheet("Declaração Simplificada"));
      assertEquals("Declaração Completa",
          workbook.getSheet("Informações Gerais").getRow(28).getCell(7).getStringCellValue());

      Sheet sheet = workbook.getSheet("Declaração Completa");

      // Quadro 1.1
      assertEquals(10.5, numeric(sheet, "C8"));
      assertEquals(2.0, numeric(sheet, "C9"));
      assertEquals(12.5, numeric(sheet, "E8"));
      // Quadro 1.2
      assertEquals(3.0, numeric(sheet, "H8"));
      assertEquals(3.0, numeric(sheet, "J9"));
      // Quadro 1.3
      assertEquals(7.0, numeric(sheet, "M10"));
      assertEquals(7.0, numeric(sheet, "O13"));
      assertEquals(6.0, numeric(sheet, "P11"));
      assertEquals(0.0, numeric(sheet, "O11"));
      // Quadro 1.5
      assertEquals(4.0, numeric(sheet, "J19"));
      assertEquals(1.5, numeric(sheet, "K19"));
    }
  }

  @Test
  @DisplayName("Should fill every empty emission cell with 0 when there are no emissions")
  void shouldFillEmptyCellsWithZero() throws IOException {
    InventoryOutputContext context = new InventoryOutputContext(null, null, null, List.of(), null, List.of());

    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(filler.fill(context)))) {
      Sheet sheet = workbook.getSheet("Declaração Completa");

      for (String address : List.of(
          "C8", "C43", "E8", "E13", "H8", "H14", "J8", "J10", "M8", "M14", "O8", "O14",
          "P8", "P14", "Q8", "Q14", "J19", "K19", "J20", "K20"
      )) {
        assertEquals(0.0, numeric(sheet, address), "Célula " + address);
      }
    }
  }

  private static InventoryOutputContext.DeclarationCellData cell(
      String frame,
      String axis,
      String code,
      String value
  ) {
    return new InventoryOutputContext.DeclarationCellData(
        frame, axis, code, value == null ? null : new BigDecimal(value)
    );
  }

  private static double numeric(Sheet sheet, String address) {
    Cell cell = cell(sheet, address);
    assertNotNull(cell, "Célula " + address + " não preenchida");
    return cell.getNumericCellValue();
  }

  private static Cell cell(Sheet sheet, String address) {
    CellReference reference = new CellReference(address);
    Row row = sheet.getRow(reference.getRow());
    Cell cell = row == null ? null : row.getCell(reference.getCol());

    return cell == null || cell.getCellType() == CellType.BLANK ? null : cell;
  }
}

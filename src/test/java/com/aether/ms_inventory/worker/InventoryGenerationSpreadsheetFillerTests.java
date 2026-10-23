package com.aether.ms_inventory.worker;

import com.aether.ms_inventory.shared.services.InventoryGenerationSpreadsheetFiller;
import com.aether.ms_inventory.shared.workers.InventoryOutputContext;
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
            emission("Escopo I", "Combustão móvel", null, "CO2", false, "10.5"),
            emission("Escopo I", "Combustão móvel", null, "CH4", false, "2"),
            emission("Escopo I", "Combustão móvel", null, "CO2", true, "4"),
            emission("Escopo II", "Compra de Energia Térmica", null, "CO2", false, "3"),
            emission("Escopo II", "Compra de Energia Térmica", null, "CO2", true, "1.5"),
            emission("Escopo III", "Transporte e distribuição", "DOWNSTREAM", "N2O", false, "7"),
            emission("Escopo III", "Viagens a negócios", null, "CO2", true, "6")
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
      assertNull(cell(sheet, "O11"));
      // Quadro 1.5
      assertEquals(4.0, numeric(sheet, "J19"));
      assertEquals(1.5, numeric(sheet, "K19"));
    }
  }

  private static InventoryOutputContext.EmissionData emission(
      String scope,
      String category,
      String classification,
      String gasFormula,
      boolean biogenic,
      String quantityCo2e
  ) {
    return new InventoryOutputContext.EmissionData(
        scope, category, classification, gasFormula, gasFormula, biogenic,
        BigDecimal.ONE, new BigDecimal(quantityCo2e), null, null
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

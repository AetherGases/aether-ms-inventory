package com.aether.ms_inventory.shared.services;

import com.aether.ms_inventory.shared.workers.InventoryOutputContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
public class InventoryGenerationSpreadsheetFiller {

  private static final String SHEET_INFORMACOES_GERAIS = "Informações Gerais";
  private static final String SHEET_UNIDADES = "Unidades";
  private static final String SHEET_DECLARACAO_COMPLETA = "Declaração Completa";
  private static final String DECLARACAO_COMPLETA_LABEL = "Declaração Completa";
  private static final int UNIDADES_FIRST_ROW = 4;

  // Seção "INFORMAÇÕES DA DIREÇÃO DA ORGANIZAÇÃO": não há dado correspondente no banco
  // (ResponsibleData modela apenas um responsável, não um segundo contato da direção).
  private static final String[] DIRECAO_ORGANIZACAO_CELLS = {
      "D18", "D19", "G19", "F19", "H19", "D20", "G20", "F20", "H20", "D21", "F21"
  };

  // As linhas são indexadas pelos códigos devolvidos por fn_ghg_complete_declaration (coluna "codigo").
  private static final Map<String, Integer> SCOPE_I_GAS_ROWS = rows(
      "CO2", 8, "CH4", 9, "N2O", 10, "HFCs", 11, "HFC-23", 12, "HFC-32", 13, "HFC-41", 14,
      "HFC-125", 15, "HFC-134", 16, "HFC-134a", 17, "HFC-143", 18, "HFC-143a", 19,
      "HFC-152", 20, "HFC-152a", 21, "HFC-161", 22, "HFC-227ea", 23, "HFC-236cb", 24,
      "HFC-236ea", 25, "HFC-236fa", 26, "HFC-245ca", 27, "HFC-245fa", 28, "HFC-365mfc", 29,
      "HFC-43-10mee", 30, "PFCs", 31, "PFC-14", 32, "PFC-116", 33, "PFC-218", 34,
      "PFC-318", 35, "PFC-3-1-10", 36, "PFC-4-1-12", 37, "PFC-5-1-14", 38, "PFC-9-1-18", 39,
      "trifluorometil_pentafluoreto_enxofre", 40, "perfluorociclopropano", 41, "SF6", 42, "NF3", 43
  );

  private static final Map<String, Integer> SCOPE_I_CATEGORY_ROWS = rows(
      "combustao_movel", 8, "combustao_estacionaria", 9, "emissoes_fugitivas", 10,
      "processos_industriais", 11, "atividades_agricolas_e_mudancas_uso_solo", 12, "residuos", 13
  );

  private static final Map<String, Integer> SCOPE_II_III_GAS_ROWS = rows(
      "CO2", 8, "CH4", 9, "N2O", 10, "HFC", 11, "PFC", 12, "SF6", 13, "NF3", 14
  );

  private static final Map<String, Integer> SCOPE_II_CATEGORY_ROWS = rows(
      "energia_eletrica_localizacao", 8, "energia_termica", 9, "perdas_transmissao_distribuicao", 10
  );

  private static final Map<String, Integer> SCOPE_III_CATEGORY_ROWS = rows(
      "transporte_distribuicao_upstream", 8, "residuos_solidos_operacao", 9, "efluentes_liquidos", 10,
      "viagens_a_negocios", 11, "deslocamento_casa_trabalho", 12,
      "transporte_distribuicao_downstream", 13, "outras_escopo_iii", 14
  );

  // Layout de cada quadro da Declaração Completa. CO2 biogênico não entra nos totais por gás/categoria:
  // nos Escopos I e II vai para o Quadro 1.5 (célula única) e no Escopo III para a coluna P, na linha da categoria.
  private static final DeclarationScope SCOPE_I = new DeclarationScope(
      "C", SCOPE_I_GAS_ROWS, "E", SCOPE_I_CATEGORY_ROWS, "J19", null, "J20", null
  );

  private static final DeclarationScope SCOPE_II = new DeclarationScope(
      "H", SCOPE_II_III_GAS_ROWS, "J", SCOPE_II_CATEGORY_ROWS, "K19", null, "K20", null
  );

  private static final DeclarationScope SCOPE_III = new DeclarationScope(
      "M", SCOPE_II_III_GAS_ROWS, "O", SCOPE_III_CATEGORY_ROWS, null, "P", null, "Q"
  );

  private record DeclarationScope(
      String gasColumn,
      Map<String, Integer> gasRows,
      String categoryColumn,
      Map<String, Integer> categoryRows,
      String biogenicCell,
      String biogenicCategoryColumn,
      String removalCell,
      String removalCategoryColumn
  ) {
    // Todas as células de entrada do quadro; vazias, o template as destaca em vermelho.
    List<String> inputCellAddresses() {
      List<String> addresses = new ArrayList<>();

      gasRows.values().forEach(row -> addresses.add(gasColumn + row));
      categoryRows.values().forEach(row -> addresses.add(categoryColumn + row));

      if (biogenicCell != null) {
        addresses.add(biogenicCell);
      }

      if (removalCell != null) {
        addresses.add(removalCell);
      }

      categoryRows.values().forEach(row -> {
        if (biogenicCategoryColumn != null) {
          addresses.add(biogenicCategoryColumn + row);
        }

        if (removalCategoryColumn != null) {
          addresses.add(removalCategoryColumn + row);
        }
      });

      return addresses;
    }
  }

  private final String templatePath;

  public InventoryGenerationSpreadsheetFiller(
      @Value("${inventory.generation-worker.template-path}") String templatePath
  ) {
    this.templatePath = templatePath;
  }

  public byte[] fill(InventoryOutputContext context) {
    try (InputStream template = new ClassPathResource(templatePath).getInputStream();
         Workbook workbook = WorkbookFactory.create(template)) {

      fillInformacoesGerais(workbook, context);
      fillUnidades(workbook, context);
      fillDeclaracaoCompleta(workbook, context);

      try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        workbook.write(out);
        return out.toByteArray();
      }
    } catch (IOException e) {
      throw new IllegalStateException("Falha ao preencher a planilha de inscrição", e);
    }
  }

  private void fillInformacoesGerais(Workbook workbook, InventoryOutputContext context) {
    Sheet sheet = workbook.getSheet(SHEET_INFORMACOES_GERAIS);

    if (sheet == null) {
      log.warn("Aba '{}' não encontrada no template", SHEET_INFORMACOES_GERAIS);
      return;
    }

    InventoryOutputContext.EnterpriseData enterprise = context.enterprise();

    if (enterprise != null) {
      InventoryOutputContext.AddressData address = enterprise.address();

      setCellValue(sheet, "F6", enterprise.legalName());
      setCellValue(sheet, "F7", enterprise.cnpj());
      setCellValue(sheet, "H9", enterprise.cnae());

      if (address != null) {
        setCellValue(sheet, "H7", address.city());
        setCellValue(sheet, "F8", formatStreetAddress(address));
        setCellValue(sheet, "H8", address.number() == null ? null : address.number().toString());
        setCellValue(sheet, "F9", address.zipCode());
      }
    }

    InventoryOutputContext.ResponsibleData responsible = context.responsible();

    if (responsible != null) {
      setCellValue(sheet, "F15", responsible.name());
      setCellValue(sheet, "H15", responsible.phone());
      setCellValue(sheet, "F17", responsible.email());
    }

    setCellValue(sheet, "H29", DECLARACAO_COMPLETA_LABEL);

    for (String address : DIRECAO_ORGANIZACAO_CELLS) {
      clearCell(sheet, address);
    }
  }

  private void fillUnidades(Workbook workbook, InventoryOutputContext context) {
    Sheet sheet = workbook.getSheet(SHEET_UNIDADES);
    List<InventoryOutputContext.UnitData> units = context.units();

    if (sheet == null) {
      log.warn("Aba '{}' não encontrada no template", SHEET_UNIDADES);
      return;
    }

    if (units == null) {
      return;
    }

    int row = UNIDADES_FIRST_ROW;

    for (InventoryOutputContext.UnitData unit : units) {
      setCellValue(sheet, "A" + row, unit.cnpj());
      setCellValue(sheet, "B" + row, unit.legalName());
      setCellValue(sheet, "C" + row, unit.fullAddress());
      setCellValue(sheet, "H" + row, unit.city());
      row++;
    }
  }

  private void fillDeclaracaoCompleta(Workbook workbook, InventoryOutputContext context) {
    Sheet sheet = workbook.getSheet(SHEET_DECLARACAO_COMPLETA);
    List<InventoryOutputContext.DeclarationCellData> cells = context.declarationCells();

    if (sheet == null) {
      log.warn("Aba '{}' não encontrada no template", SHEET_DECLARACAO_COMPLETA);
      return;
    }

    // Gases/categorias sem emissão devem constar como 0 (e não vazios, que o template pinta de vermelho).
    Map<String, BigDecimal> valuesByCellAddress = new LinkedHashMap<>();

    for (DeclarationScope scope : List.of(SCOPE_I, SCOPE_II, SCOPE_III)) {
      scope.inputCellAddresses().forEach(address -> valuesByCellAddress.put(address, BigDecimal.ZERO));
    }

    for (InventoryOutputContext.DeclarationCellData cell
        : cells == null ? List.<InventoryOutputContext.DeclarationCellData>of() : cells) {
      String address = resolveCellAddress(cell);

      if (address == null) {
        log.warn("Célula da Declaração Completa não mapeada (quadro={}, eixo={}, código={})",
            cell.frame(), cell.axis(), cell.code());
        continue;
      }

      if (cell.value() != null) {
        valuesByCellAddress.put(address, cell.value());
      }
    }

    valuesByCellAddress.forEach((address, value) -> setCellValue(sheet, address, value));
  }

  // Traduz (quadro, eixo, código) de fn_ghg_complete_declaration para o endereço da célula no template.
  private static String resolveCellAddress(InventoryOutputContext.DeclarationCellData cell) {
    if (cell.frame() == null || cell.axis() == null || cell.code() == null) {
      return null;
    }

    boolean summaryFrame = "1.5".equals(cell.frame());
    DeclarationScope scope = switch (cell.frame()) {
      case "1.1" -> SCOPE_I;
      case "1.2" -> SCOPE_II;
      case "1.3" -> SCOPE_III;
      case "1.5" -> switch (cell.code()) {
        case "ESCOPO_I" -> SCOPE_I;
        case "ESCOPO_II" -> SCOPE_II;
        default -> null;
      };
      default -> null;
    };

    if (scope == null) {
      return null;
    }

    String code = normalize(cell.code());

    return switch (cell.axis()) {
      case "GAS" -> addressOf(scope.gasColumn(), scope.gasRows().get(code));
      case "CATEGORIA" -> addressOf(scope.categoryColumn(), scope.categoryRows().get(code));
      case "BIOGENICO_EMISSAO" -> summaryFrame
          ? scope.biogenicCell()
          : addressOf(scope.biogenicCategoryColumn(), scope.categoryRows().get(code));
      case "BIOGENICO_REMOCAO" -> summaryFrame
          ? scope.removalCell()
          : addressOf(scope.removalCategoryColumn(), scope.categoryRows().get(code));
      default -> null;
    };
  }

  private static String addressOf(String column, Integer row) {
    return column == null || row == null ? null : column + row;
  }

  private static String formatStreetAddress(InventoryOutputContext.AddressData address) {
    StringBuilder builder = new StringBuilder();

    appendPart(builder, address.street());
    appendPart(builder, address.neighborhood());

    if (address.complement() != null && !address.complement().isBlank()) {
      appendPart(builder, address.complement());
    }

    return builder.isEmpty() ? null : builder.toString();
  }

  private static void appendPart(StringBuilder builder, String part) {
    if (part == null || part.isBlank()) {
      return;
    }

    if (!builder.isEmpty()) {
      builder.append(", ");
    }

    builder.append(part);
  }

  private static String normalize(String value) {
    if (value == null) {
      return "";
    }

    String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "");

    return withoutAccents.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
  }

  private static Map<String, Integer> rows(Object... labelsAndRows) {
    Map<String, Integer> map = new HashMap<>();

    for (int i = 0; i < labelsAndRows.length; i += 2) {
      map.put(normalize((String) labelsAndRows[i]), (Integer) labelsAndRows[i + 1]);
    }

    return map;
  }

  private static void clearCell(Sheet sheet, String address) {
    CellReference reference = new CellReference(address);
    Row row = sheet.getRow(reference.getRow());

    if (row == null) {
      return;
    }

    Cell cell = row.getCell(reference.getCol());

    if (cell != null) {
      cell.setBlank();
    }
  }

  private static void setCellValue(Sheet sheet, String address, Object value) {
    if (value == null) {
      return;
    }

    CellReference reference = new CellReference(address);
    int rowIndex = reference.getRow();
    int colIndex = reference.getCol();

    Row row = sheet.getRow(rowIndex);
    if (row == null) {
      row = sheet.createRow(rowIndex);
    }

    Cell cell = row.getCell(colIndex);
    if (cell == null) {
      cell = row.createCell(colIndex);
    }

    if (value instanceof BigDecimal decimal) {
      cell.setCellValue(decimal.doubleValue());
    } else if (value instanceof LocalDate date) {
      cell.setCellValue(date);
    } else {
      String text = value.toString();

      if (!text.isBlank()) {
        cell.setCellValue(text);
      }
    }
  }
}

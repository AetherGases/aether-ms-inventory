package com.aether.ms_inventory.shared.services;

import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.helpers.NormalizeOutput;
import com.aether.ms_inventory.shared.helpers.interfaces.BrevoTemplate;
import com.aether.ms_inventory.shared.helpers.templates.InventoryPendingReviewTemplate;
import com.aether.ms_inventory.shared.helpers.templates.InventoryReviewedTemplate;
import com.aether.ms_inventory.shared.persistence.mongo.documents.NotificationConfigDocument;
import com.aether.ms_inventory.shared.persistence.mongo.documents.NotificationDocument;
import com.aether.ms_inventory.shared.persistence.mongo.repository.NotificationConfigRepository;
import com.aether.ms_inventory.shared.persistence.mongo.repository.NotificationRepository;
import com.aether.ms_inventory.shared.persistence.postgres.entities.EmployeeEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.InventoryEntity;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.EmployeeRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryNotificationService {
  static final String APPROVE_URI_FORMAT = "/api/inventories/pending/%d/approve";

  private final InventoryRepository inventoryRepository;
  private final EmployeeRepository employeeRepository;
  private final NotificationConfigRepository notificationConfigRepository;
  private final NotificationRepository notificationRepository;
  private final BrevoService brevoService;
  private final PushNotificationService pushNotificationService;
  private final Executor applicationTaskExecutor;

  public void notifyApproversPendingReview(Integer inventoryId) {
    try {
      inventoryRepository.findById(inventoryId).ifPresent(this::notifyApprovers);
    } catch (RuntimeException e) {
      log.error("Nao foi possivel notificar os aprovadores do inventario {}", inventoryId, e);
    }
  }

  public void notifyOwnerReviewed(InventoryEntity inventory) {
    try {
      EmployeeEntity owner = inventory.getOwnerEmployee();
      EmployeeEntity validator = inventory.getValidatorEmployee();

      if (owner == null || validator == null) {
        return;
      }

      boolean approved = inventory.getStatus() == InventoryStatusEnum.APPROVED;
      String message = inventory.getStatusMessage() == null ? "" : inventory.getStatusMessage();

      NotificationDocument notification = newNotification(inventory, owner);
      notification.setValidatorEmployee(new NotificationDocument.Employee(validator.getId(), validator.getName()));
      notification.setTitle(approved ? "Inventário aprovado" : "Inventário reprovado");
      notification.setDescription(
          "O inventário \"%s\" foi %s por %s.%s".formatted(
              inventory.getName(),
              approved ? "aprovado" : "reprovado",
              validator.getName(),
              message.isBlank() ? "" : " Mensagem: " + message
          )
      );

      NotificationConfigDocument config = findConfig(owner.getId());

      deliver(notification, config, owner.getEmail(), new InventoryReviewedTemplate(
          validator.getName(),
          message,
          inventory.getName(),
          owner.getName(),
          inventory.getStatus()
      ));
    } catch (RuntimeException e) {
      log.error("Nao foi possivel notificar o dono do inventario {}", inventory.getId(), e);
    }
  }

  private void notifyApprovers(InventoryEntity inventory) {
    if (inventory.getDepartment() == null || inventory.getOwnerEmployee() == null) {
      return;
    }

    List<EmployeeEntity> approvers = employeeRepository.findApproversByDepartment(
        inventory.getDepartment().getId(),
        inventory.getOwnerEmployee().getId(),
        APPROVE_URI_FORMAT.formatted(inventory.getId())
    );

    String ownerName = inventory.getOwnerEmployee().getName();

    Map<Integer, NotificationConfigDocument> configs = loadConfigs(approvers);

    for (EmployeeEntity approver : approvers) {
      try {
        NotificationConfigDocument config = configs.get(approver.getId());
        applicationTaskExecutor.execute(() -> notifyApprover(inventory, ownerName, approver, config));
      } catch (RuntimeException e) {
        log.error("Falha ao agendar a notificacao do aprovador {} do inventario {}",
            approver.getId(), inventory.getId(), e);
      }
    }
  }

  private NotificationConfigDocument findConfig(Integer userId) {
    try {
      return notificationConfigRepository.findByUserId(userId).orElse(null);
    } catch (RuntimeException e) {
      log.error("Falha ao buscar a configuracao de notificacao do usuario {}", userId, e);
      return null;
    }
  }

  /**
   * Falha na consulta nao impede a notificacao interna; apenas e-mail e push deixam de sair.
   */
  private Map<Integer, NotificationConfigDocument> loadConfigs(List<EmployeeEntity> approvers) {
    try {
      List<Integer> userIds = approvers.stream().map(EmployeeEntity::getId).toList();

      return notificationConfigRepository.findByUserIdIn(userIds).stream()
          .collect(Collectors.toMap(
              NotificationConfigDocument::getUserId,
              Function.identity(),
              (first, duplicate) -> first
          ));
    } catch (RuntimeException e) {
      log.error("Falha ao buscar as configuracoes de notificacao dos aprovadores", e);
      return Map.of();
    }
  }

  private void notifyApprover(
      InventoryEntity inventory,
      String ownerName,
      EmployeeEntity approver,
      NotificationConfigDocument config
  ) {
    try {
      NotificationDocument notification = newNotification(inventory, approver);
      notification.setTitle("Inventário pendente de revisão");
      notification.setDescription(
          "O inventário \"%s\" de %s aguarda a sua revisão."
              .formatted(inventory.getName(), ownerName)
      );

      deliver(notification, config, approver.getEmail(), new InventoryPendingReviewTemplate(
          NormalizeOutput.name(approver.getName()),
          NormalizeOutput.name(inventory.getName()),
          NormalizeOutput.name(ownerName)
      ));
    } catch (RuntimeException e) {
      log.error("Falha ao notificar o aprovador {} do inventario {}",
          approver.getId(), inventory.getId(), e);
    }
  }

  private NotificationDocument newNotification(InventoryEntity inventory, EmployeeEntity receiver) {
    NotificationDocument notification = new NotificationDocument();
    notification.setUserId(receiver.getId());
    notification.setInventory(new NotificationDocument.Inventory(
        inventory.getId(), inventory.getStatus(), inventory.getType()
    ));
    notification.setCreatedAt(Instant.now());
    return notification;
  }

  private void deliver(
      NotificationDocument notification,
      NotificationConfigDocument config,
      String email,
      BrevoTemplate template
  ) {
    try {
      notificationRepository.save(notification);
    } catch (RuntimeException e) {
      log.error("Falha ao salvar a notificacao do usuario {}", notification.getUserId(), e);
    }

    if (config == null || config.getNotify() == null) {
      return;
    }

    if (Boolean.TRUE.equals(config.getNotify().getMobile())) {
      pushNotificationService.send(
          notification.getUserId(),
          config.getTokens(),
          notification.getTitle(),
          notification.getDescription()
      );
    }

    if (Boolean.TRUE.equals(config.getNotify().getEmail())) {
      brevoService.send(template, email);
    }
  }
}

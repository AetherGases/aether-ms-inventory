package com.aether.ms_inventory.notification;

import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.enums.InventoryTypeEnum;
import com.aether.ms_inventory.shared.helpers.interfaces.BrevoTemplate;
import com.aether.ms_inventory.shared.persistence.mongo.documents.NotificationConfigDocument;
import com.aether.ms_inventory.shared.persistence.mongo.documents.NotificationDocument;
import com.aether.ms_inventory.shared.persistence.mongo.repository.NotificationConfigRepository;
import com.aether.ms_inventory.shared.persistence.mongo.repository.NotificationRepository;
import com.aether.ms_inventory.shared.persistence.postgres.entities.DepartmentEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.EmployeeEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.InventoryEntity;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.EmployeeRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.InventoryRepository;
import com.aether.ms_inventory.shared.services.BrevoService;
import com.aether.ms_inventory.shared.services.InventoryNotificationService;
import com.aether.ms_inventory.shared.services.PushNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryNotificationService Tests")
class InventoryNotificationServiceTests {

  @Mock private InventoryRepository inventoryRepository;
  @Mock private EmployeeRepository employeeRepository;
  @Mock private NotificationConfigRepository notificationConfigRepository;
  @Mock private NotificationRepository notificationRepository;
  @Mock private BrevoService brevoService;
  @Mock private PushNotificationService pushNotificationService;

  private InventoryNotificationService service;

  private InventoryEntity inventory;
  private EmployeeEntity owner;
  private EmployeeEntity approver;

  @BeforeEach
  void setUp() {
    // Executor sincrono: mantem os testes deterministicos
    service = new InventoryNotificationService(
        inventoryRepository,
        employeeRepository,
        notificationConfigRepository,
        notificationRepository,
        brevoService,
        pushNotificationService,
        Runnable::run
    );

    DepartmentEntity department = new DepartmentEntity();
    department.setId(5);

    owner = new EmployeeEntity();
    owner.setId(1);
    owner.setName("Dono");
    owner.setEmail("dono@test.com");

    inventory = new InventoryEntity();
    inventory.setId(9);
    inventory.setName("Inventario 2025");
    inventory.setType(InventoryTypeEnum.INPUT);
    inventory.setStatus(InventoryStatusEnum.UNDER_REVIEW);
    inventory.setDepartment(department);
    inventory.setOwnerEmployee(owner);

    approver = new EmployeeEntity();
    approver.setId(2);
    approver.setName("Aprovador");
    approver.setEmail("aprovador@test.com");
  }

  private void givenApprovers() {
    when(inventoryRepository.findById(9)).thenReturn(Optional.of(inventory));
    when(employeeRepository.findApproversByDepartment(5, 1, "/api/inventories/pending/9/approve"))
        .thenReturn(List.of(approver));
  }

  private void givenConfig(Integer userId, Boolean email, Boolean mobile) {
    NotificationConfigDocument config = new NotificationConfigDocument(userId);
    config.setNotify(new NotificationConfigDocument.Notify(email, mobile));
    when(notificationConfigRepository.findByUserId(userId)).thenReturn(Optional.of(config));
  }

  private void givenApproverConfig(Integer userId, Boolean email, Boolean mobile) {
    NotificationConfigDocument config = new NotificationConfigDocument(userId);
    config.setNotify(new NotificationConfigDocument.Notify(email, mobile));
    when(notificationConfigRepository.findByUserIdIn(any())).thenReturn(List.of(config));
  }

  private void givenReviewed(InventoryStatusEnum status, String message) {
    inventory.setStatus(status);
    inventory.setStatusMessage(message);

    EmployeeEntity validator = new EmployeeEntity();
    validator.setId(3);
    validator.setName("Validador");
    inventory.setValidatorEmployee(validator);
  }

  // ---------------------------------------------------------------------------
  // notifyApproversPendingReview
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should always save the notification and send email when email is active")
  void notifiesApproversWithEmail() {
    givenApprovers();
    givenApproverConfig(2, true, false);

    service.notifyApproversPendingReview(9);

    ArgumentCaptor<NotificationDocument> doc = ArgumentCaptor.forClass(NotificationDocument.class);
    verify(notificationRepository).save(doc.capture());
    assertEquals(2, doc.getValue().getUserId());
    assertEquals(9, doc.getValue().getInventory().getId());
    assertEquals(InventoryStatusEnum.UNDER_REVIEW, doc.getValue().getInventory().getStatus());

    ArgumentCaptor<BrevoTemplate> template = ArgumentCaptor.forClass(BrevoTemplate.class);
    verify(brevoService).send(template.capture(), eq("aprovador@test.com"));
    assertEquals(2, template.getValue().template());
  }

  @Test
  @DisplayName("Should send push with the user tokens when mobile is active")
  void sendsPushWhenMobileActive() {
    givenApprovers();
    NotificationConfigDocument config = new NotificationConfigDocument(2);
    config.setNotify(new NotificationConfigDocument.Notify(false, true));
    config.setTokens(List.of(new NotificationConfigDocument.Token("t1", null, null)));
    when(notificationConfigRepository.findByUserIdIn(any())).thenReturn(List.of(config));

    service.notifyApproversPendingReview(9);

    verify(pushNotificationService).send(
        eq(2), eq(config.getTokens()), eq("Inventário pendente de revisão"), anyString());
    verify(brevoService, never()).send(any(), anyString());
  }

  @Test
  @DisplayName("Should not send push when mobile is inactive")
  void skipsPushWhenMobileInactive() {
    givenApprovers();
    givenApproverConfig(2, true, false);

    service.notifyApproversPendingReview(9);

    verify(pushNotificationService, never()).send(any(), any(), any(), any());
  }

  @Test
  @DisplayName("Should save the notification but not send email when email is inactive")
  void savesWithoutEmail() {
    givenApprovers();
    givenApproverConfig(2, false, true);

    service.notifyApproversPendingReview(9);

    verify(notificationRepository).save(any());
    verify(brevoService, never()).send(any(), anyString());
  }

  @Test
  @DisplayName("Should save the notification even when the user has no notification config")
  void savesWithoutConfig() {
    givenApprovers();
    when(notificationConfigRepository.findByUserIdIn(any())).thenReturn(List.of());

    service.notifyApproversPendingReview(9);

    verify(notificationRepository).save(any());
    verify(brevoService, never()).send(any(), anyString());
  }

  @Test
  @DisplayName("Should keep notifying other approvers when one fails and never throw")
  void isolatesFailures() {
    EmployeeEntity second = new EmployeeEntity();
    second.setId(3);
    second.setName("Outro");
    second.setEmail("outro@test.com");

    when(inventoryRepository.findById(9)).thenReturn(Optional.of(inventory));
    when(employeeRepository.findApproversByDepartment(5, 1, "/api/inventories/pending/9/approve"))
        .thenReturn(List.of(approver, second));
    NotificationConfigDocument config1 = new NotificationConfigDocument(2);
    config1.setNotify(new NotificationConfigDocument.Notify(true, false));
    NotificationConfigDocument config2 = new NotificationConfigDocument(3);
    config2.setNotify(new NotificationConfigDocument.Notify(true, false));
    when(notificationConfigRepository.findByUserIdIn(any())).thenReturn(List.of(config1, config2));
    doThrow(new RuntimeException("brevo down")).when(brevoService).send(any(), eq("aprovador@test.com"));

    service.notifyApproversPendingReview(9);

    verify(notificationRepository, times(2)).save(any());
    verify(brevoService).send(any(), eq("outro@test.com"));
  }

  @Test
  @DisplayName("Should query the configs of all approvers in a single batch")
  void loadsConfigsInBatch() {
    EmployeeEntity second = new EmployeeEntity();
    second.setId(3);
    second.setName("Outro");
    second.setEmail("outro@test.com");

    when(inventoryRepository.findById(9)).thenReturn(Optional.of(inventory));
    when(employeeRepository.findApproversByDepartment(5, 1, "/api/inventories/pending/9/approve"))
        .thenReturn(List.of(approver, second));
    when(notificationConfigRepository.findByUserIdIn(any())).thenReturn(List.of());

    service.notifyApproversPendingReview(9);

    verify(notificationConfigRepository).findByUserIdIn(List.of(2, 3));
    verify(notificationConfigRepository, never()).findByUserId(any());
    verify(notificationRepository, times(2)).save(any());
  }

  @Test
  @DisplayName("Should still save notifications when the batch config query fails")
  void savesWhenBatchConfigFails() {
    givenApprovers();
    when(notificationConfigRepository.findByUserIdIn(any())).thenThrow(new RuntimeException("mongo down"));

    service.notifyApproversPendingReview(9);

    verify(notificationRepository).save(any());
    verify(brevoService, never()).send(any(), anyString());
  }

  @Test
  @DisplayName("Should still send email when saving the notification fails")
  void sendsEmailWhenSaveFails() {
    givenApprovers();
    givenApproverConfig(2, true, false);
    when(notificationRepository.save(any())).thenThrow(new RuntimeException("mongo down"));

    service.notifyApproversPendingReview(9);

    verify(brevoService).send(any(), eq("aprovador@test.com"));
  }

  @Test
  @DisplayName("Should not throw when the inventory lookup fails")
  void swallowsLookupFailure() {
    when(inventoryRepository.findById(9)).thenThrow(new RuntimeException("db down"));

    service.notifyApproversPendingReview(9);

    verify(employeeRepository, never()).findApproversByDepartment(any(), any(), anyString());
  }

  // ---------------------------------------------------------------------------
  // notifyOwnerReviewed
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should save the notification with the validator and send the reviewed email")
  void notifiesOwnerApproved() {
    givenReviewed(InventoryStatusEnum.APPROVED, "Tudo certo");
    givenConfig(1, true, true);

    service.notifyOwnerReviewed(inventory);

    ArgumentCaptor<NotificationDocument> doc = ArgumentCaptor.forClass(NotificationDocument.class);
    verify(notificationRepository).save(doc.capture());
    assertEquals(1, doc.getValue().getUserId());
    assertEquals(InventoryStatusEnum.APPROVED, doc.getValue().getInventory().getStatus());
    assertEquals(3, doc.getValue().getValidatorEmployee().getId());
    assertEquals("Validador", doc.getValue().getValidatorEmployee().getName());

    ArgumentCaptor<BrevoTemplate> template = ArgumentCaptor.forClass(BrevoTemplate.class);
    verify(brevoService).send(template.capture(), eq("dono@test.com"));
    assertEquals(3, template.getValue().template());

    Map<String, Object> params = template.getValue().params();
    assertEquals("aprovado", params.get("inventoryStatus"));
    assertEquals("Tudo certo", params.get("validationMessage"));
    assertEquals("Validador", params.get("approverName"));
  }

  @Test
  @DisplayName("Should notify the owner of a rejected inventory")
  void notifiesOwnerRejected() {
    givenReviewed(InventoryStatusEnum.REJECTED, "Dados inconsistentes");
    givenConfig(1, true, false);

    service.notifyOwnerReviewed(inventory);

    ArgumentCaptor<BrevoTemplate> template = ArgumentCaptor.forClass(BrevoTemplate.class);
    verify(brevoService).send(template.capture(), eq("dono@test.com"));
    assertEquals("reprovado", template.getValue().params().get("inventoryStatus"));
  }

  @Test
  @DisplayName("Should accept a null status message")
  void acceptsNullMessage() {
    givenReviewed(InventoryStatusEnum.APPROVED, null);
    givenConfig(1, true, false);

    service.notifyOwnerReviewed(inventory);

    ArgumentCaptor<BrevoTemplate> template = ArgumentCaptor.forClass(BrevoTemplate.class);
    verify(brevoService).send(template.capture(), eq("dono@test.com"));
    assertEquals("", template.getValue().params().get("validationMessage"));
  }

  @Test
  @DisplayName("Should save the notification but not send email when the owner has no config")
  void ownerWithoutEmail() {
    givenReviewed(InventoryStatusEnum.APPROVED, "ok");
    when(notificationConfigRepository.findByUserId(1)).thenReturn(Optional.empty());

    service.notifyOwnerReviewed(inventory);

    verify(notificationRepository).save(any());
    verify(brevoService, never()).send(any(), anyString());
  }

  @Test
  @DisplayName("Should never throw when notifying the owner fails")
  void ownerFailureIsSwallowed() {
    givenReviewed(InventoryStatusEnum.APPROVED, "ok");
    when(notificationRepository.save(any())).thenThrow(new RuntimeException("mongo down"));
    when(notificationConfigRepository.findByUserId(1)).thenThrow(new RuntimeException("mongo down"));

    service.notifyOwnerReviewed(inventory);

    verify(brevoService, never()).send(any(), anyString());
  }
}

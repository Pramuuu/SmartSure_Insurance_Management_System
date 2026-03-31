package com.smartSure.PolicyService.service;

import com.smartSure.PolicyService.dto.policytype.PolicyTypeRequest;
import com.smartSure.PolicyService.dto.policytype.PolicyTypeResponse;
import com.smartSure.PolicyService.entity.PolicyType;
import com.smartSure.PolicyService.exception.PolicyTypeNotFoundException;
import com.smartSure.PolicyService.mapper.PolicyTypeMapper;
import com.smartSure.PolicyService.repository.PolicyTypeRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyTypeService Unit Tests")
class PolicyTypeServiceTest {

    @Mock private PolicyTypeRepository policyTypeRepository;
    @Mock private PolicyTypeMapper     policyTypeMapper;

    @InjectMocks
    private PolicyTypeService policyTypeService;

    private PolicyType     activePolicyType;
    private PolicyTypeResponse policyTypeResponse;

    @BeforeEach
    void setUp() {
        activePolicyType = PolicyType.builder()
                .id(1L)
                .name("Health Insurance")
                .category(PolicyType.InsuranceCategory.HEALTH)
                .basePremium(new BigDecimal("500.00"))
                .maxCoverageAmount(new BigDecimal("1000000.00"))
                .deductibleAmount(new BigDecimal("5000.00"))
                .termMonths(12)
                .minAge(18)
                .maxAge(65)
                .status(PolicyType.PolicyTypeStatus.ACTIVE)
                .build();

        policyTypeResponse = PolicyTypeResponse.builder()
                .id(1L)
                .name("Health Insurance")
                .category("HEALTH")
                .status("ACTIVE")
                .build();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // getAllActivePolicyTypes
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAllActivePolicyTypes()")
    class GetAllActiveTests {

        @Test
        @DisplayName("should return list of active policy types")
        void getAllActive_returnsActiveTypes() {
            when(policyTypeRepository.findByStatusOrderByCategory(PolicyType.PolicyTypeStatus.ACTIVE))
                    .thenReturn(List.of(activePolicyType));
            when(policyTypeMapper.toResponse(activePolicyType))
                    .thenReturn(policyTypeResponse);

            List<PolicyTypeResponse> result = policyTypeService.getAllActivePolicyTypes();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Health Insurance");
        }

        @Test
        @DisplayName("should return empty list when no active types exist")
        void getAllActive_noTypes_returnsEmptyList() {
            when(policyTypeRepository.findByStatusOrderByCategory(PolicyType.PolicyTypeStatus.ACTIVE))
                    .thenReturn(List.of());

            List<PolicyTypeResponse> result = policyTypeService.getAllActivePolicyTypes();

            assertThat(result).isEmpty();
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // getPolicyTypeById
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getPolicyTypeById()")
    class GetByIdTests {

        @Test
        @DisplayName("should return policy type when found")
        void getById_found_returnsPolicyType() {
            when(policyTypeRepository.findById(1L)).thenReturn(Optional.of(activePolicyType));
            when(policyTypeMapper.toResponse(activePolicyType)).thenReturn(policyTypeResponse);

            PolicyTypeResponse result = policyTypeService.getPolicyTypeById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Health Insurance");
        }

        @Test
        @DisplayName("should throw PolicyTypeNotFoundException when not found")
        void getById_notFound_throwsException() {
            when(policyTypeRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> policyTypeService.getPolicyTypeById(99L))
                    .isInstanceOf(PolicyTypeNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // getByCategory
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getByCategory()")
    class GetByCategoryTests {

        @Test
        @DisplayName("should return only ACTIVE types for given category")
        void getByCategory_returnsOnlyActive() {
            PolicyType inactiveType = PolicyType.builder()
                    .id(2L).name("Old Health")
                    .category(PolicyType.InsuranceCategory.HEALTH)
                    .status(PolicyType.PolicyTypeStatus.DISCONTINUED)
                    .build();

            when(policyTypeRepository.findByCategory(PolicyType.InsuranceCategory.HEALTH))
                    .thenReturn(List.of(activePolicyType, inactiveType));
            when(policyTypeMapper.toResponse(activePolicyType)).thenReturn(policyTypeResponse);

            List<PolicyTypeResponse> result =
                    policyTypeService.getByCategory(PolicyType.InsuranceCategory.HEALTH);

            // Only the ACTIVE one should be returned
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Health Insurance");
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // createPolicyType
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createPolicyType()")
    class CreatePolicyTypeTests {

        private PolicyTypeRequest buildValidRequest() {
            return PolicyTypeRequest.builder()
                    .name("Life Insurance")
                    .category(PolicyType.InsuranceCategory.LIFE)
                    .basePremium(new BigDecimal("800.00"))
                    .maxCoverageAmount(new BigDecimal("2000000.00"))
                    .deductibleAmount(new BigDecimal("10000.00"))
                    .termMonths(24)
                    .minAge(18)
                    .maxAge(70)
                    .build();
        }

        @Test
        @DisplayName("should create policy type successfully")
        void create_validRequest_createsType() {
            PolicyTypeRequest request = buildValidRequest();

            when(policyTypeRepository.existsByName("Life Insurance")).thenReturn(false);
            when(policyTypeRepository.save(any(PolicyType.class))).thenReturn(activePolicyType);
            when(policyTypeMapper.toResponse(any())).thenReturn(policyTypeResponse);

            PolicyTypeResponse result = policyTypeService.createPolicyType(request);

            assertThat(result).isNotNull();
            verify(policyTypeRepository, times(1)).save(any(PolicyType.class));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when name already exists")
        void create_duplicateName_throwsException() {
            PolicyTypeRequest request = buildValidRequest();
            when(policyTypeRepository.existsByName("Life Insurance")).thenReturn(true);

            assertThatThrownBy(() -> policyTypeService.createPolicyType(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");

            verify(policyTypeRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when minAge > maxAge")
        void create_invalidAgeRange_throwsException() {
            PolicyTypeRequest request = PolicyTypeRequest.builder()
                    .name("Bad Insurance")
                    .category(PolicyType.InsuranceCategory.LIFE)
                    .basePremium(new BigDecimal("800.00"))
                    .maxCoverageAmount(new BigDecimal("2000000.00"))
                    .deductibleAmount(new BigDecimal("10000.00"))
                    .termMonths(24)
                    .minAge(70)   // minAge > maxAge — invalid
                    .maxAge(18)
                    .build();

            when(policyTypeRepository.existsByName("Bad Insurance")).thenReturn(false);

            assertThatThrownBy(() -> policyTypeService.createPolicyType(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Min age cannot be greater than max age");

            verify(policyTypeRepository, never()).save(any());
        }

        @Test
        @DisplayName("should allow null minAge and maxAge (no age restriction)")
        void create_nullAgeRange_createsSuccessfully() {
            PolicyTypeRequest request = PolicyTypeRequest.builder()
                    .name("Universal Insurance")
                    .category(PolicyType.InsuranceCategory.LIFE)
                    .basePremium(new BigDecimal("800.00"))
                    .maxCoverageAmount(new BigDecimal("2000000.00"))
                    .deductibleAmount(new BigDecimal("10000.00"))
                    .termMonths(24)
                    .minAge(null)  // no age restriction
                    .maxAge(null)
                    .build();

            when(policyTypeRepository.existsByName("Universal Insurance")).thenReturn(false);
            when(policyTypeRepository.save(any())).thenReturn(activePolicyType);
            when(policyTypeMapper.toResponse(any())).thenReturn(policyTypeResponse);

            // Should NOT throw
            assertThatCode(() -> policyTypeService.createPolicyType(request))
                    .doesNotThrowAnyException();
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // updatePolicyType
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updatePolicyType()")
    class UpdatePolicyTypeTests {

        @Test
        @DisplayName("should update all fields of existing policy type")
        void update_existingType_updatesFields() {
            PolicyTypeRequest request = PolicyTypeRequest.builder()
                    .name("Updated Health")
                    .category(PolicyType.InsuranceCategory.HEALTH)
                    .basePremium(new BigDecimal("600.00"))
                    .maxCoverageAmount(new BigDecimal("1500000.00"))
                    .deductibleAmount(new BigDecimal("7000.00"))
                    .termMonths(18)
                    .minAge(18)
                    .maxAge(70)
                    .build();

            when(policyTypeRepository.findById(1L)).thenReturn(Optional.of(activePolicyType));
            when(policyTypeRepository.save(any())).thenReturn(activePolicyType);
            when(policyTypeMapper.toResponse(any())).thenReturn(policyTypeResponse);

            policyTypeService.updatePolicyType(1L, request);

            // Verify the entity fields were updated before save
            assertThat(activePolicyType.getName()).isEqualTo("Updated Health");
            assertThat(activePolicyType.getBasePremium()).isEqualByComparingTo("600.00");
            assertThat(activePolicyType.getTermMonths()).isEqualTo(18);
            verify(policyTypeRepository, times(1)).save(activePolicyType);
        }

        @Test
        @DisplayName("should throw PolicyTypeNotFoundException when updating non-existent type")
        void update_notFound_throwsException() {
            when(policyTypeRepository.findById(99L)).thenReturn(Optional.empty());

            PolicyTypeRequest request = PolicyTypeRequest.builder()
                    .name("X").category(PolicyType.InsuranceCategory.HEALTH)
                    .basePremium(BigDecimal.ONE).maxCoverageAmount(BigDecimal.TEN)
                    .deductibleAmount(BigDecimal.ONE).termMonths(12)
                    .build();

            assertThatThrownBy(() -> policyTypeService.updatePolicyType(99L, request))
                    .isInstanceOf(PolicyTypeNotFoundException.class);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // deletePolicyType (soft delete)
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deletePolicyType()")
    class DeletePolicyTypeTests {

        @Test
        @DisplayName("should soft-delete by setting status to DISCONTINUED")
        void delete_existingType_setsDiscontinued() {
            when(policyTypeRepository.findById(1L)).thenReturn(Optional.of(activePolicyType));
            when(policyTypeRepository.save(any())).thenReturn(activePolicyType);

            policyTypeService.deletePolicyType(1L);

            // Status must be DISCONTINUED — row is NOT physically deleted
            assertThat(activePolicyType.getStatus())
                    .isEqualTo(PolicyType.PolicyTypeStatus.DISCONTINUED);
            verify(policyTypeRepository, times(1)).save(activePolicyType);
        }

        @Test
        @DisplayName("should throw PolicyTypeNotFoundException when deleting non-existent type")
        void delete_notFound_throwsException() {
            when(policyTypeRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> policyTypeService.deletePolicyType(99L))
                    .isInstanceOf(PolicyTypeNotFoundException.class);

            verify(policyTypeRepository, never()).save(any());
        }
    }
}

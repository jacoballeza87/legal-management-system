package com.legal.cases;

import com.legal.cases.dto.*;
import com.legal.cases.exception.CaseNotFoundException;
import com.legal.cases.kafka.CaseEventProducer;
import com.legal.cases.mapper.CaseMapper;
import com.legal.cases.model.Case;
import com.legal.cases.repository.*;
import com.legal.cases.service.CaseService;
import com.legal.cases.service.QRCodeService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CaseService Tests")
class CaseServiceTest {

    @Mock private CaseRepository caseRepository;
    @Mock private CaseVersionRepository versionRepository;
    @Mock private CollaboratorRepository collaboratorRepository;
    @Mock private CaseMapper caseMapper;
    @Mock private QRCodeService qrCodeService;
    @Mock private CaseEventProducer eventProducer;

    @InjectMocks private CaseService caseService;

    private Case testCase;
    private CaseDTO testCaseDTO;

    @BeforeEach
    void setUp() {
        testCase = Case.builder()
                .id(1L).caseNumber("CASE-2024-00001").title("Caso Test")
                .status(Case.CaseStatus.CREATED).ownerId(10L)
                .currentVersion(1).billedHours(0.0)
                .build();
        testCaseDTO = CaseDTO.builder()
                .id(1L).caseNumber("CASE-2024-00001").title("Caso Test")
                .status(Case.CaseStatus.CREATED).build();
    }

    @Test
    @DisplayName("Obtener caso por ID exitosamente")
    void getCaseById_Found() {
        when(caseRepository.findById(1L)).thenReturn(Optional.of(testCase));
        when(caseMapper.toDTO(testCase)).thenReturn(testCaseDTO);

        CaseDTO result = caseService.getCaseById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getCaseNumber()).isEqualTo("CASE-2024-00001");
    }

    @Test
    @DisplayName("Caso no encontrado lanza excepción")
    void getCaseById_NotFound() {
        when(caseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> caseService.getCaseById(999L))
                .isInstanceOf(CaseNotFoundException.class);
    }

    @Test
    @DisplayName("Crear caso genera número automático y QR")
    void createCase_GeneratesCaseNumberAndQR() {
        CreateCaseRequest request = CreateCaseRequest.builder()
                .title("Nuevo Caso").caseType(Case.CaseType.CIVIL)
                .clientName("Cliente Test").build();

        when(caseMapper.toEntity(any())).thenReturn(testCase);
        when(caseRepository.count()).thenReturn(0L);
        when(caseRepository.existsByCaseNumber(anyString())).thenReturn(false);
        when(qrCodeService.generateQRCodeBase64(anyString(), anyLong())).thenReturn("data:image/png;base64,abc");
        when(caseRepository.save(any())).thenReturn(testCase);
        when(versionRepository.save(any())).thenReturn(null);
        when(caseMapper.toDTO(any())).thenReturn(testCaseDTO);

        CaseDTO result = caseService.createCase(request, 10L);

        assertThat(result).isNotNull();
        verify(qrCodeService).generateQRCodeBase64(anyString(), anyLong());
        verify(eventProducer).publishCaseCreated(any(), eq(10L));
    }

    @Test
    @DisplayName("Cambio de estado publica evento")
    void changeStatus_PublishesEvent() {
        when(caseRepository.findById(1L)).thenReturn(Optional.of(testCase));
        when(caseRepository.save(any())).thenReturn(testCase);
        when(caseMapper.toDTO(any())).thenReturn(testCaseDTO);

        caseService.changeStatus(1L, Case.CaseStatus.UPDATED, 10L);

        verify(eventProducer).publishStatusChanged(any(), eq(Case.CaseStatus.CREATED), eq(10L));
    }

    @Test
    @DisplayName("Eliminar caso publica evento de eliminación")
    void deleteCase_PublishesDeletedEvent() {
        when(caseRepository.findById(1L)).thenReturn(Optional.of(testCase));

        caseService.deleteCase(1L, 10L);

        verify(caseRepository).delete(testCase);
        verify(eventProducer).publishCaseDeleted(eq(1L), eq("CASE-2024-00001"), eq(10L));
    }
}

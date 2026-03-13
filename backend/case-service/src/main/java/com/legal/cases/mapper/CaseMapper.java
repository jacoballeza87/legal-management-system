package com.legal.cases.mapper;

import com.legal.cases.dto.*;
import com.legal.cases.model.*;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CaseMapper {

    // Case → CaseDTO
    @Mapping(target = "collaborators", source = "collaborators")
    CaseDTO toDTO(Case legalCase);

    List<CaseDTO> toDTOList(List<Case> cases);

    // CreateCaseRequest → Case
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "caseNumber", ignore = true)
    @Mapping(target = "status", constant = "CREATED")         // era OPEN — BR#35
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "qrCodeUrl", ignore = true)
    @Mapping(target = "currentVersion", constant = "1")
    @Mapping(target = "billedHours", constant = "0.0")
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "certifiedAt", ignore = true)
    @Mapping(target = "certifiedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "versions", ignore = true)
    @Mapping(target = "collaborators", ignore = true)
    @Mapping(target = "relatedCases", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Case toEntity(CreateCaseRequest request);

    // UpdateCaseRequest → Case (parcial)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "caseNumber", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "qrCodeUrl", ignore = true)
    @Mapping(target = "currentVersion", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "certifiedAt", ignore = true)
    @Mapping(target = "certifiedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "versions", ignore = true)
    @Mapping(target = "collaborators", ignore = true)
    @Mapping(target = "relatedCases", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateCaseRequest request, @MappingTarget Case legalCase);

    // CaseVersion → VersionDTO
    @Mapping(target = "caseId", source = "legalCase.id")
    VersionDTO toDTO(CaseVersion version);

    // CaseCollaborator → CollaboratorDTO
    CollaboratorDTO toDTO(CaseCollaborator collaborator);

    // VersionComment → CommentDTO
    @Mapping(target = "versionId", source = "version.id")
    CommentDTO toDTO(VersionComment comment);
}

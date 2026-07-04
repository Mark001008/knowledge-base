package com.ma.kb.service.system.impl;

import com.ma.kb.common.exception.BusinessException;
import com.ma.kb.common.response.ErrorCode;
import com.ma.kb.dal.mapper.auth.PermissionMapper;
import com.ma.kb.dal.model.auth.PermissionDO;
import com.ma.kb.manager.auth.PermissionManager;
import com.ma.kb.manager.auth.bo.PermissionBO;
import com.ma.kb.service.system.PermissionService;
import com.ma.kb.service.system.dto.CreatePermissionRequest;
import com.ma.kb.service.system.dto.PermissionDTO;
import com.ma.kb.service.system.dto.UpdatePermissionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限服务实现
 */
@Service
public class PermissionServiceImpl implements PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionServiceImpl.class);

    private final PermissionManager permissionManager;
    private final PermissionMapper permissionMapper;

    public PermissionServiceImpl(PermissionManager permissionManager, PermissionMapper permissionMapper) {
        this.permissionManager = permissionManager;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public List<String> getCurrentUserPermissionCodes(Long userId) {
        return permissionManager.getPermissionCodesByUserId(userId);
    }

    @Override
    public List<PermissionDTO> getAllPermissions() {
        List<PermissionBO> permissionBOList = permissionManager.getAll();
        return permissionBOList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PermissionDTO getPermissionById(Long id) {
        PermissionDO permissionDO = permissionMapper.selectById(id);
        if (permissionDO == null) {
            throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND);
        }
        return convertToDTO(permissionManager.getByPermissionCode(permissionDO.getPermissionCode()));
    }

    @Override
    public PermissionDTO createPermission(CreatePermissionRequest request) {
        // 检查权限编码是否已存在
        PermissionBO existing = permissionManager.getByPermissionCode(request.permissionCode());
        if (existing != null) {
            throw new BusinessException(ErrorCode.PERMISSION_CODE_EXISTS);
        }

        PermissionDO permissionDO = new PermissionDO();
        permissionDO.setPermissionCode(request.permissionCode());
        permissionDO.setPermissionName(request.permissionName());
        permissionDO.setModule(request.module());
        permissionDO.setDescription(request.description());
        permissionDO.setStatus("ENABLED");

        permissionMapper.insert(permissionDO);
        log.info("创建权限成功: {}", request.permissionCode());

        return convertToDTO(permissionManager.getByPermissionCode(request.permissionCode()));
    }

    @Override
    public PermissionDTO updatePermission(Long id, UpdatePermissionRequest request) {
        PermissionDO permissionDO = permissionMapper.selectById(id);
        if (permissionDO == null) {
            throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND);
        }

        permissionDO.setPermissionName(request.permissionName());
        permissionDO.setModule(request.module());
        permissionDO.setDescription(request.description());
        permissionDO.setStatus(request.status());

        permissionMapper.updateById(permissionDO);
        log.info("更新权限成功: {}", id);

        return convertToDTO(permissionManager.getByPermissionCode(permissionDO.getPermissionCode()));
    }

    @Override
    public void deletePermission(Long id) {
        PermissionDO permissionDO = permissionMapper.selectById(id);
        if (permissionDO == null) {
            throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND);
        }

        permissionMapper.deleteById(id);
        log.info("删除权限成功: {}", id);
    }

    private PermissionDTO convertToDTO(PermissionBO permissionBO) {
        if (permissionBO == null) {
            return null;
        }
        return new PermissionDTO(
                permissionBO.getId(),
                permissionBO.getPermissionCode(),
                permissionBO.getPermissionName(),
                permissionBO.getModule(),
                permissionBO.getDescription(),
                permissionBO.getStatus(),
                permissionBO.getCreatedAt(),
                permissionBO.getUpdatedAt()
        );
    }
}

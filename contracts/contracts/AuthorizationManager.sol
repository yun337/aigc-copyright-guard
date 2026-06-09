// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/Ownable.sol";
import "@openzeppelin/contracts/access/AccessControl.sol";
import "@openzeppelin/contracts/security/ReentrancyGuard.sol";

/**
 * @title AuthorizationManager
 * @dev 链创守护 - 版权授权管理合约
 * @notice 管理版权授权、许可发放、授权验证
 */
contract AuthorizationManager is Ownable, AccessControl, ReentrancyGuard {

    // ============ 角色定义 ============
    bytes32 public constant ARBITER_ROLE = keccak256("ARBITER_ROLE");

    // ============ 数据结构 ============

    /// @dev 授权记录结构体
    struct Authorization {
        uint256 id;                 // 授权ID
        uint256 copyrightId;        // 版权记录ID
        address licensor;           // 授权人(版权方)
        address licensee;           // 被授权人
        LicenseType licenseType;    // 授权类型
        uint256 fee;                // 授权费用 (wei)
        uint256 startTime;          // 开始时间
        uint256 endTime;            // 结束时间 (0=永久)
        bool active;                // 是否有效
        uint256 createdAt;          // 创建时间
    }

    enum LicenseType {
        EXCLUSIVE,       // 独家授权
        NON_EXCLUSIVE,   // 非独家授权
        TEMPORARY        // 临时授权
    }

    /// @dev 授权许可结构体
    struct License {
        uint256 id;
        uint256 authorizationId;    // 关联授权ID
        address licensee;           // 被许可人
        string licenseURI;          // 许可证书URI
        uint256 issuedAt;           // 发放时间
        bool valid;                 // 是否有效
    }

    // ============ 状态变量 ============

    uint256 private _authIdCounter;
    uint256 private _licenseIdCounter;

    // 授权记录映射
    mapping(uint256 => Authorization) private _authorizations;

    // 许可映射
    mapping(uint256 => License) private _licenses;

    // 版权ID => 授权ID列表
    mapping(uint256 => uint256[]) private _copyrightAuthorizations;

    // 被授权人 => 授权ID列表
    mapping(address => uint256[]) private _licenseeAuthorizations;

    // 全部授权ID
    uint256[] private _allAuthIds;

    // ============ 事件 ============

    event AuthorizationCreated(
        uint256 indexed authId,
        uint256 indexed copyrightId,
        address indexed licensor,
        address licensee,
        LicenseType licenseType,
        uint256 fee
    );

    event AuthorizationRevoked(
        uint256 indexed authId,
        address indexed revoker
    );

    event LicenseIssued(
        uint256 indexed licenseId,
        uint256 indexed authorizationId,
        address indexed licensee,
        string licenseURI
    );

    event LicenseRevoked(
        uint256 indexed licenseId
    );

    event FeePaid(
        uint256 indexed authId,
        address indexed payer,
        uint256 amount
    );

    // ============ 构造函数 ============

    constructor() Ownable() {
        _transferOwnership(msg.sender);
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
        _grantRole(ARBITER_ROLE, msg.sender);
    }

    // ============ 修饰器 ============

    modifier authExists(uint256 authId) {
        require(_authorizations[authId].id > 0, "AuthorizationManager: authorization not found");
        _;
    }

    modifier onlyLicensor(uint256 authId) {
        require(
            _authorizations[authId].licensor == msg.sender ||
            hasRole(DEFAULT_ADMIN_ROLE, msg.sender),
            "AuthorizationManager: not the licensor"
        );
        _;
    }

    modifier onlyActive(uint256 authId) {
        require(_authorizations[authId].active, "AuthorizationManager: not active");
        _;
    }

    // ============ 核心功能 ============

    /**
     * @dev 创建版权授权
     * @param copyrightId 版权记录ID
     * @param licensee 被授权人地址
     * @param licenseType 授权类型
     * @param durationDays 授权天数 (0=永久)
     * @return authId 授权ID
     */
    function createAuthorization(
        uint256 copyrightId,
        address licensee,
        LicenseType licenseType,
        uint256 durationDays
    )
        external
        payable
        nonReentrant
        returns (uint256 authId)
    {
        require(licensee != address(0), "AuthorizationManager: invalid licensee");
        require(licensee != msg.sender, "AuthorizationManager: cannot authorize to self");

        _authIdCounter++;
        authId = _authIdCounter;

        uint256 endTime = 0;
        if (durationDays > 0) {
            endTime = block.timestamp + (durationDays * 1 days);
        }

        Authorization storage auth = _authorizations[authId];
        auth.id = authId;
        auth.copyrightId = copyrightId;
        auth.licensor = msg.sender;
        auth.licensee = licensee;
        auth.licenseType = licenseType;
        auth.fee = msg.value;
        auth.startTime = block.timestamp;
        auth.endTime = endTime;
        auth.active = true;
        auth.createdAt = block.timestamp;

        _copyrightAuthorizations[copyrightId].push(authId);
        _licenseeAuthorizations[licensee].push(authId);
        _allAuthIds.push(authId);

        emit AuthorizationCreated(authId, copyrightId, msg.sender, licensee, licenseType, msg.value);

        return authId;
    }

    /**
     * @dev 撤销授权
     */
    function revokeAuthorization(
        uint256 authId
    )
        external
        authExists(authId)
        onlyLicensor(authId)
        onlyActive(authId)
    {
        _authorizations[authId].active = false;

        emit AuthorizationRevoked(authId, msg.sender);
    }

    /**
     * @dev 发放授权许可证书
     * @param authId 授权ID
     * @param licenseURI 许可证书URI
     * @return licenseId 许可ID
     */
    function issueLicense(
        uint256 authId,
        string calldata licenseURI
    )
        external
        authExists(authId)
        onlyLicensor(authId)
        onlyActive(authId)
        returns (uint256 licenseId)
    {
        _licenseIdCounter++;
        licenseId = _licenseIdCounter;

        License storage lic = _licenses[licenseId];
        lic.id = licenseId;
        lic.authorizationId = authId;
        lic.licensee = _authorizations[authId].licensee;
        lic.licenseURI = licenseURI;
        lic.issuedAt = block.timestamp;
        lic.valid = true;

        emit LicenseIssued(licenseId, authId, lic.licensee, licenseURI);

        return licenseId;
    }

    /**
     * @dev 撤销许可证书
     */
    function revokeLicense(
        uint256 licenseId
    )
        external
    {
        require(_licenses[licenseId].id > 0, "AuthorizationManager: license not found");
        uint256 authId = _licenses[licenseId].authorizationId;
        require(
            _authorizations[authId].licensor == msg.sender ||
            hasRole(DEFAULT_ADMIN_ROLE, msg.sender),
            "AuthorizationManager: not authorized"
        );

        _licenses[licenseId].valid = false;
        emit LicenseRevoked(licenseId);
    }

    // ============ 查询功能 ============

    /**
     * @dev 获取授权详情
     */
    function getAuthorization(
        uint256 authId
    )
        external
        view
        authExists(authId)
        returns (Authorization memory)
    {
        return _authorizations[authId];
    }

    /**
     * @dev 获取许可详情
     */
    function getLicense(
        uint256 licenseId
    )
        external
        view
        returns (License memory)
    {
        require(_licenses[licenseId].id > 0, "AuthorizationManager: license not found");
        return _licenses[licenseId];
    }

    /**
     * @dev 检查授权是否有效
     */
    function isAuthorizationValid(
        uint256 authId
    )
        external
        view
        authExists(authId)
        returns (bool)
    {
        Authorization storage auth = _authorizations[authId];
        if (!auth.active) return false;
        if (auth.endTime > 0 && block.timestamp > auth.endTime) return false;
        return true;
    }

    /**
     * @dev 检查许可是否有效
     */
    function isLicenseValid(
        uint256 licenseId
    )
        external
        view
        returns (bool)
    {
        License storage lic = _licenses[licenseId];
        if (lic.id == 0 || !lic.valid) return false;

        Authorization storage auth = _authorizations[lic.authorizationId];
        if (!auth.active) return false;
        if (auth.endTime > 0 && block.timestamp > auth.endTime) return false;

        return true;
    }

    /**
     * @dev 获取版权的授权列表
     */
    function getCopyrightAuthorizations(
        uint256 copyrightId
    )
        external
        view
        returns (uint256[] memory)
    {
        return _copyrightAuthorizations[copyrightId];
    }

    /**
     * @dev 获取被授权人的授权列表
     */
    function getLicenseeAuthorizations(
        address licensee
    )
        external
        view
        returns (uint256[] memory)
    {
        return _licenseeAuthorizations[licensee];
    }

    /**
     * @dev 分页获取全部授权
     */
    function getAuthorizationsPaginated(
        uint256 offset,
        uint256 limit
    )
        external
        view
        returns (
            Authorization[] memory records,
            uint256 total
        )
    {
        total = _allAuthIds.length;
        if (offset >= total) {
            return (new Authorization[](0), total);
        }

        uint256 end = offset + limit;
        if (end > total) end = total;
        uint256 size = end - offset;

        records = new Authorization[](size);
        for (uint256 i = 0; i < size; i++) {
            records[i] = _authorizations[_allAuthIds[offset + i]];
        }
        return (records, total);
    }

    // ============ 提款功能 ============

    /**
     * @dev 授权人提取授权费
     */
    function withdrawFees(
        uint256 authId
    )
        external
        authExists(authId)
        onlyLicensor(authId)
        nonReentrant
    {
        Authorization storage auth = _authorizations[authId];
        uint256 amount = auth.fee;
        require(amount > 0, "AuthorizationManager: no fees to withdraw");
        auth.fee = 0;

        (bool success, ) = payable(msg.sender).call{value: amount}("");
        require(success, "AuthorizationManager: withdraw failed");
    }

    // ============ 管理功能 ============

    /**
     * @dev 授予仲裁者角色
     */
    function grantArbiterRole(address account)
        external
        onlyRole(DEFAULT_ADMIN_ROLE)
    {
        grantRole(ARBITER_ROLE, account);
    }

    /**
     * @dev 获取全部授权数量
     */
    function getTotalAuthorizations() external view returns (uint256) {
        return _allAuthIds.length;
    }
}

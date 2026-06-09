// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/Ownable.sol";
import "@openzeppelin/contracts/access/AccessControl.sol";
import "@openzeppelin/contracts/security/ReentrancyGuard.sol";
import "@openzeppelin/contracts/security/Pausable.sol";

/**
 * @title CopyrightRegistry
 * @dev 链创守护 - AIGC作品版权存证合约
 * @notice 提供作品版权存证、时间戳记录、版权查询功能
 */
contract CopyrightRegistry is Ownable, AccessControl, ReentrancyGuard, Pausable {

    // ============ 角色定义 ============
    bytes32 public constant REGISTRAR_ROLE = keccak256("REGISTRAR_ROLE");
    bytes32 public constant VERIFIER_ROLE = keccak256("VERIFIER_ROLE");

    // ============ 数据结构 ============

    /// @dev 版权记录结构体
    struct CopyrightRecord {
        uint256 id;                 // 版权记录ID
        address creator;            // 创作者钱包地址
        string fileHash;            // 作品文件SHA256哈希
        string ipfsCid;             // IPFS内容标识符
        string metadataCid;         // 元数据IPFS CID
        string certCid;             // 版权证书IPFS CID
        string workTitle;           // 作品标题
        string workType;            // 作品类型 (IMAGE/VIDEO/MUSIC/TEXT)
        uint256 timestamp;          // 存证时间戳
        bool exists;                // 记录是否存在
    }

    /// @dev 侵权报告结构体
    struct InfringementReport {
        uint256 id;
        uint256 originalCopyrightId;    // 原始版权记录ID
        string infringingFileHash;      // 侵权作品哈希
        string infringingIpfsCid;       // 侵权作品IPFS CID
        uint256 similarityScore;        // 相似度分数 (0-10000, 即0%-100%)
        address reporter;               // 报告人
        ReportStatus status;            // 报告状态
        uint256 timestamp;
    }

    enum ReportStatus {
        PENDING,      // 待处理
        CONFIRMED,    // 已确认侵权
        DISMISSED     // 已驳回
    }

    // ============ 状态变量 ============

    uint256 private _copyrightIdCounter;
    uint256 private _reportIdCounter;

    // 版权记录映射: copyrightId => CopyrightRecord
    mapping(uint256 => CopyrightRecord) private _copyrightRecords;

    // 文件哈希 => 版权记录ID (快速查重)
    mapping(string => uint256) private _fileHashToId;

    // 创作者地址 => 版权记录ID列表
    mapping(address => uint256[]) private _creatorCopyrights;

    // 侵权报告映射
    mapping(uint256 => InfringementReport) private _infringementReports;

    // 全部版权记录ID列表
    uint256[] private _allCopyrightIds;

    // ============ 事件 ============

    event CopyrightRegistered(
        uint256 indexed copyrightId,
        address indexed creator,
        string fileHash,
        string ipfsCid,
        uint256 timestamp
    );

    event CopyrightUpdated(
        uint256 indexed copyrightId,
        string newMetadataCid
    );

    event CopyrightRevoked(
        uint256 indexed copyrightId,
        address indexed creator
    );

    event InfringementReported(
        uint256 indexed reportId,
        uint256 indexed originalCopyrightId,
        string infringingFileHash,
        uint256 similarityScore
    );

    event InfringementResolved(
        uint256 indexed reportId,
        ReportStatus status
    );

    // ============ 构造函数 ============

    constructor() Ownable() {
        _transferOwnership(msg.sender);
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
        _grantRole(REGISTRAR_ROLE, msg.sender);
        _grantRole(VERIFIER_ROLE, msg.sender);
    }

    // ============ 修饰器 ============

    modifier copyrightExists(uint256 copyrightId) {
        require(_copyrightRecords[copyrightId].exists, "CopyrightRegistry: copyright not found");
        _;
    }

    modifier onlyCreator(uint256 copyrightId) {
        require(
            _copyrightRecords[copyrightId].creator == msg.sender ||
            hasRole(DEFAULT_ADMIN_ROLE, msg.sender),
            "CopyrightRegistry: not the creator"
        );
        _;
    }

    // ============ 核心功能 ============

    /**
     * @dev 注册新的版权存证
     * @param fileHash 作品文件SHA256哈希
     * @param ipfsCid 作品IPFS CID
     * @param metadataCid 元数据IPFS CID
     * @param workTitle 作品标题
     * @param workType 作品类型
     * @return copyrightId 版权记录ID
     */
    function registerCopyright(
        string calldata fileHash,
        string calldata ipfsCid,
        string calldata metadataCid,
        string calldata workTitle,
        string calldata workType
    )
        external
        whenNotPaused
        nonReentrant
        returns (uint256 copyrightId)
    {
        require(bytes(fileHash).length > 0, "CopyrightRegistry: empty file hash");
        require(bytes(ipfsCid).length > 0, "CopyrightRegistry: empty IPFS CID");
        require(_fileHashToId[fileHash] == 0, "CopyrightRegistry: file already registered");

        _copyrightIdCounter++;
        copyrightId = _copyrightIdCounter;

        CopyrightRecord storage record = _copyrightRecords[copyrightId];
        record.id = copyrightId;
        record.creator = msg.sender;
        record.fileHash = fileHash;
        record.ipfsCid = ipfsCid;
        record.metadataCid = metadataCid;
        record.certCid = "";
        record.workTitle = workTitle;
        record.workType = workType;
        record.timestamp = block.timestamp;
        record.exists = true;

        _fileHashToId[fileHash] = copyrightId;
        _creatorCopyrights[msg.sender].push(copyrightId);
        _allCopyrightIds.push(copyrightId);

        emit CopyrightRegistered(copyrightId, msg.sender, fileHash, ipfsCid, block.timestamp);

        return copyrightId;
    }

    /**
     * @dev 更新版权证书CID
     * @param copyrightId 版权记录ID
     * @param certCid 版权证书IPFS CID
     */
    function updateCertificateCid(
        uint256 copyrightId,
        string calldata certCid
    )
        external
        copyrightExists(copyrightId)
        onlyCreator(copyrightId)
    {
        _copyrightRecords[copyrightId].certCid = certCid;
        emit CopyrightUpdated(copyrightId, certCid);
    }

    /**
     * @dev 撤销版权存证
     * @param copyrightId 版权记录ID
     */
    function revokeCopyright(
        uint256 copyrightId
    )
        external
        copyrightExists(copyrightId)
        onlyCreator(copyrightId)
    {
        CopyrightRecord storage record = _copyrightRecords[copyrightId];
        string memory fileHash = record.fileHash;

        delete _fileHashToId[fileHash];
        record.exists = false;

        emit CopyrightRevoked(copyrightId, msg.sender);
    }

    // ============ 查询功能 ============

    /**
     * @dev 根据版权ID获取版权详情
     */
    function getCopyright(
        uint256 copyrightId
    )
        external
        view
        copyrightExists(copyrightId)
        returns (CopyrightRecord memory)
    {
        return _copyrightRecords[copyrightId];
    }

    /**
     * @dev 根据文件哈希查询版权记录ID
     */
    function getCopyrightIdByHash(
        string calldata fileHash
    )
        external
        view
        returns (uint256)
    {
        uint256 id = _fileHashToId[fileHash];
        require(id > 0, "CopyrightRegistry: not registered");
        return id;
    }

    /**
     * @dev 检查文件是否已注册版权
     */
    function isCopyrightRegistered(
        string calldata fileHash
    )
        external
        view
        returns (bool)
    {
        return _fileHashToId[fileHash] > 0;
    }

    /**
     * @dev 获取创作者的版权记录列表
     */
    function getCreatorCopyrights(
        address creator
    )
        external
        view
        returns (uint256[] memory)
    {
        return _creatorCopyrights[creator];
    }

    /**
     * @dev 获取创作者版权记录数量
     */
    function getCreatorCopyrightCount(
        address creator
    )
        external
        view
        returns (uint256)
    {
        return _creatorCopyrights[creator].length;
    }

    /**
     * @dev 获取全部版权记录数量
     */
    function getTotalCopyrights()
        external
        view
        returns (uint256)
    {
        return _allCopyrightIds.length;
    }

    /**
     * @dev 分页获取版权记录列表
     */
    function getCopyrightsPaginated(
        uint256 offset,
        uint256 limit
    )
        external
        view
        returns (
            CopyrightRecord[] memory records,
            uint256 total
        )
    {
        total = _allCopyrightIds.length;
        if (offset >= total) {
            return (new CopyrightRecord[](0), total);
        }

        uint256 end = offset + limit;
        if (end > total) {
            end = total;
        }
        uint256 resultSize = end - offset;

        records = new CopyrightRecord[](resultSize);
        for (uint256 i = 0; i < resultSize; i++) {
            records[i] = _copyrightRecords[_allCopyrightIds[offset + i]];
        }

        return (records, total);
    }

    // ============ 侵权报告功能 ============

    /**
     * @dev 提交侵权报告
     * @param originalCopyrightId 原始版权记录ID
     * @param infringingFileHash 疑似侵权作品哈希
     * @param infringingIpfsCid 疑似侵权作品IPFS CID
     * @param similarityScore 相似度分数 (0-10000)
     * @return reportId 报告ID
     */
    function reportInfringement(
        uint256 originalCopyrightId,
        string calldata infringingFileHash,
        string calldata infringingIpfsCid,
        uint256 similarityScore
    )
        external
        copyrightExists(originalCopyrightId)
        returns (uint256 reportId)
    {
        require(similarityScore <= 10000, "CopyrightRegistry: invalid score");

        _reportIdCounter++;
        reportId = _reportIdCounter;

        InfringementReport storage report = _infringementReports[reportId];
        report.id = reportId;
        report.originalCopyrightId = originalCopyrightId;
        report.infringingFileHash = infringingFileHash;
        report.infringingIpfsCid = infringingIpfsCid;
        report.similarityScore = similarityScore;
        report.reporter = msg.sender;
        report.status = ReportStatus.PENDING;
        report.timestamp = block.timestamp;

        emit InfringementReported(
            reportId,
            originalCopyrightId,
            infringingFileHash,
            similarityScore
        );

        return reportId;
    }

    /**
     * @dev 解决侵权报告 (管理员/验证者调用)
     */
    function resolveInfringement(
        uint256 reportId,
        ReportStatus resolution
    )
        external
        onlyRole(VERIFIER_ROLE)
    {
        require(
            _infringementReports[reportId].id > 0,
            "CopyrightRegistry: report not found"
        );
        require(
            _infringementReports[reportId].status == ReportStatus.PENDING,
            "CopyrightRegistry: already resolved"
        );

        _infringementReports[reportId].status = resolution;

        emit InfringementResolved(reportId, resolution);
    }

    /**
     * @dev 获取侵权报告详情
     */
    function getInfringementReport(
        uint256 reportId
    )
        external
        view
        returns (InfringementReport memory)
    {
        require(_infringementReports[reportId].id > 0, "CopyrightRegistry: report not found");
        return _infringementReports[reportId];
    }

    // ============ 管理功能 ============

    /**
     * @dev 暂停合约
     */
    function pause() external onlyRole(DEFAULT_ADMIN_ROLE) {
        _pause();
    }

    /**
     * @dev 恢复合约
     */
    function unpause() external onlyRole(DEFAULT_ADMIN_ROLE) {
        _unpause();
    }

    /**
     * @dev 授予注册者角色
     */
    function grantRegistrarRole(address account) external onlyRole(DEFAULT_ADMIN_ROLE) {
        grantRole(REGISTRAR_ROLE, account);
    }

    /**
     * @dev 授予验证者角色
     */
    function grantVerifierRole(address account) external onlyRole(DEFAULT_ADMIN_ROLE) {
        grantRole(VERIFIER_ROLE, account);
    }
}

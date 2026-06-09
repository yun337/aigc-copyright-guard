// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC721/ERC721.sol";
import "@openzeppelin/contracts/access/Ownable.sol";
import "@openzeppelin/contracts/access/AccessControl.sol";
import "@openzeppelin/contracts/security/ReentrancyGuard.sol";

/**
 * @title CopyrightNFT
 * @dev 链创守护 - AIGC作品版权NFT合约 (ERC-721)
 */
contract CopyrightNFT is ERC721, Ownable, AccessControl, ReentrancyGuard {

    bytes32 public constant MINTER_ROLE = keccak256("MINTER_ROLE");

    struct NFTMetadata {
        uint256 tokenId;
        uint256 copyrightId;
        string ipfsCid;
        address creator;
        uint256 royaltyRate;
        uint256 mintedAt;
    }

    uint256 private _tokenIdCounter;

    mapping(uint256 => NFTMetadata) private _nftMetadata;
    mapping(uint256 => uint256) private _copyrightToToken;
    mapping(uint256 => uint256) private _royaltyRates;

    string private constant _NAME = "CopyrightGuard NFT";
    string private constant _SYMBOL = "CGNFT";

    event NFTMinted(uint256 indexed tokenId, uint256 indexed copyrightId, address indexed creator, string ipfsCid, string tokenURI);
    event NFTRoyaltySet(uint256 indexed tokenId, uint256 royaltyRate);

    constructor() ERC721(_NAME, _SYMBOL) Ownable() {
        _transferOwnership(msg.sender);
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
        _grantRole(MINTER_ROLE, msg.sender);
    }

    function mintCopyrightNFT(
        address to,
        uint256 copyrightId,
        string calldata ipfsCid,
        string calldata tokenURI_,
        uint256 royaltyRate
    )
        public
        onlyRole(MINTER_ROLE)
        nonReentrant
        returns (uint256)
    {
        require(to != address(0), "Zero address");
        require(bytes(ipfsCid).length > 0, "Empty CID");
        require(royaltyRate <= 10000, "Invalid royalty");
        require(_copyrightToToken[copyrightId] == 0, "Already minted");

        _tokenIdCounter++;
        uint256 tokenId = _tokenIdCounter;

        _safeMint(to, tokenId);
        // 简化版：tokenURI 存入 mapping（不用 URIStorage）
        _setTokenURI(tokenId, tokenURI_);

        NFTMetadata storage meta = _nftMetadata[tokenId];
        meta.tokenId = tokenId;
        meta.copyrightId = copyrightId;
        meta.ipfsCid = ipfsCid;
        meta.creator = to;
        meta.royaltyRate = royaltyRate;
        meta.mintedAt = block.timestamp;

        _copyrightToToken[copyrightId] = tokenId;
        _royaltyRates[tokenId] = royaltyRate;

        emit NFTMinted(tokenId, copyrightId, to, ipfsCid, tokenURI_);
        return tokenId;
    }

    // 存储 tokenURI 的 mapping
    mapping(uint256 => string) private _tokenURIs;

    function _setTokenURI(uint256 tokenId, string memory _tokenURI) internal {
        _tokenURIs[tokenId] = _tokenURI;
    }

    function tokenURI(uint256 tokenId) public view virtual override returns (string memory) {
        string memory uri = _tokenURIs[tokenId];
        if (bytes(uri).length > 0) return uri;
        return super.tokenURI(tokenId);
    }

    function getNFTMetadata(uint256 tokenId) external view returns (NFTMetadata memory) {
        require(_exists(tokenId), "Token not exists");
        return _nftMetadata[tokenId];
    }

    function getTokenByCopyright(uint256 copyrightId) external view returns (uint256) {
        uint256 tokenId = _copyrightToToken[copyrightId];
        require(tokenId > 0, "Not minted");
        return tokenId;
    }

    function getRoyaltyRate(uint256 tokenId) external view returns (uint256) {
        return _royaltyRates[tokenId];
    }

    function getCreatorNFTs(address creator) external view returns (uint256[] memory) {
        uint256 balance = balanceOf(creator);
        uint256[] memory tokens = new uint256[](balance);
        uint256 idx = 0;
        for (uint256 i = 1; i <= _tokenIdCounter && idx < balance; i++) {
            if (_exists(i) && ownerOf(i) == creator) {
                tokens[idx++] = i;
            }
        }
        return tokens;
    }

    function getCurrentTokenId() external view returns (uint256) { return _tokenIdCounter; }
    function isCopyrightMinted(uint256 copyrightId) external view returns (bool) { return _copyrightToToken[copyrightId] > 0; }

    function setRoyaltyRate(uint256 tokenId, uint256 royaltyRate) external {
        require(_exists(tokenId) && ownerOf(tokenId) == msg.sender, "Not owner");
        require(royaltyRate <= 10000, "Invalid rate");
        _royaltyRates[tokenId] = royaltyRate;
        if (_nftMetadata[tokenId].tokenId > 0) _nftMetadata[tokenId].royaltyRate = royaltyRate;
        emit NFTRoyaltySet(tokenId, royaltyRate);
    }

    function grantMinterRole(address account) external onlyRole(DEFAULT_ADMIN_ROLE) { grantRole(MINTER_ROLE, account); }

    function supportsInterface(bytes4 interfaceId) public view override(ERC721, AccessControl) returns (bool) {
        return super.supportsInterface(interfaceId);
    }

    function _burn(uint256 tokenId) internal override {
        super._burn(tokenId);
        delete _tokenURIs[tokenId];
    }
}

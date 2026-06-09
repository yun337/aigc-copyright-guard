package com.copyright.utils;

import com.copyright.exception.BlockchainException;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 区块链交互工具类
 */
@Slf4j
@Component
public class BlockchainUtil {

    private Web3j web3j;
    private Credentials credentials;

    @Getter
    @Value("${blockchain.contracts.copyright-registry}")
    private String copyrightRegistryAddress;

    @Getter
    @Value("${blockchain.contracts.copyright-nft}")
    private String copyrightNFTAddress;

    @Getter
    @Value("${blockchain.contracts.authorization-manager}")
    private String authorizationManagerAddress;

    @Value("${blockchain.ethereum.rpc-url}")
    private String rpcUrl;

    @Value("${blockchain.ethereum.private-key}")
    private String privateKey;

    @Value("${blockchain.ethereum.gas-limit}")
    private BigInteger gasLimit;

    @Value("${blockchain.ethereum.gas-price}")
    private BigInteger gasPrice;

    @PostConstruct
    public void init() {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        if (privateKey != null && !privateKey.isEmpty()) {
            this.credentials = Credentials.create(privateKey);
            try {
                BigInteger chainId = web3j.ethChainId().send().getChainId();
                log.info("区块链节点已连接: {} (ChainID: {})", rpcUrl, chainId);
            } catch (Exception e) {
                log.error("区块链节点连接失败: {}", e.getMessage());
            }
        } else {
            log.warn("区块链私钥未配置, 版权存证/NFT铸造等功能将不可用. 请设置环境变量 ETH_PRIVATE_KEY");
        }
        if (copyrightRegistryAddress == null || copyrightRegistryAddress.isEmpty()) {
            log.warn("版权注册合约地址未配置. 请设置环境变量 COPYRIGHT_REGISTRY_ADDR");
        }
        if (copyrightNFTAddress == null || copyrightNFTAddress.isEmpty()) {
            log.warn("NFT合约地址未配置. 请设置环境变量 COPYRIGHT_NFT_ADDR");
        }
    }

    /**
     * 检查区块链是否已配置
     */
    public boolean isReady() {
        return credentials != null
                && copyrightRegistryAddress != null && !copyrightRegistryAddress.isEmpty()
                && copyrightNFTAddress != null && !copyrightNFTAddress.isEmpty();
    }

    public String registerCopyright(String fileHash, String ipfsCid, String metadataCid,
                                     String workTitle, String workType) throws Exception {
        Function func = new Function("registerCopyright",
                Arrays.asList(
                        new Utf8String(fileHash),
                        new Utf8String(ipfsCid),
                        new Utf8String(metadataCid),
                        new Utf8String(workTitle),
                        new Utf8String(workType)
                ),
                Collections.emptyList());
        return sendTx(copyrightRegistryAddress, func);
    }

    public String mintNFT(String to, long copyrightId, String ipfsCid,
                           String tokenURI, int royaltyRate) throws Exception {
        Function func = new Function("mintCopyrightNFT",
                Arrays.asList(
                        new Address(to),
                        new Uint256(copyrightId),
                        new Utf8String(ipfsCid),
                        new Utf8String(tokenURI),
                        new Uint256(royaltyRate)
                ),
                Collections.emptyList());
        return sendTx(copyrightNFTAddress, func);
    }

    public String updateCertificateCid(long copyrightId, String certCid) throws Exception {
        Function func = new Function("updateCertificateCid",
                Arrays.asList(new Uint256(copyrightId), new Utf8String(certCid)),
                Collections.emptyList());
        return sendTx(copyrightRegistryAddress, func);
    }

    public BigInteger getCopyrightIdByHash(String fileHash) throws Exception {
        Function func = new Function("getCopyrightIdByHash",
                Collections.singletonList(new Utf8String(fileHash)),
                Collections.singletonList(new org.web3j.abi.TypeReference<Uint256>() {}));
        EthCall resp = callContract(copyrightRegistryAddress, func);
        String result = resp.getValue();
        if (result == null || result.length() < 3) throw new BlockchainException("链上未找到该文件哈希对应的版权记录");
        return new BigInteger(result.substring(2), 16);
    }

    public boolean isFileRegistered(String fileHash) {
        try {
            Function func = new Function("isCopyrightRegistered",
                    Collections.singletonList(new Utf8String(fileHash)),
                    Collections.emptyList());
            EthCall resp = callContract(copyrightRegistryAddress, func);
            String result = resp.getValue();
            return result != null && !result.equals("0x") && result.length() > 3 &&
                   new BigInteger(result.substring(2), 16).compareTo(BigInteger.ZERO) > 0;
        } catch (Exception e) {
            log.warn("检查注册状态失败: {}", e.getMessage());
            return false;
        }
    }

    public BigInteger getTokenByCopyright(long copyrightId) throws Exception {
        Function func = new Function("getTokenByCopyright",
                Collections.singletonList(new Uint256(copyrightId)),
                Collections.singletonList(new org.web3j.abi.TypeReference<Uint256>() {}));
        EthCall resp = callContract(copyrightNFTAddress, func);
        String result = resp.getValue();
        if (result == null || result.length() < 3) throw new BlockchainException("链上未找到版权ID对应的Token");
        return new BigInteger(result.substring(2), 16);
    }

    public BigInteger getTransactionBlockNumber(String txHash) {
        try {
            EthTransaction tx = web3j.ethGetTransactionByHash(txHash).send();
            if (tx.getTransaction().isPresent()) return tx.getTransaction().get().getBlockNumber();
        } catch (Exception e) { log.warn("获取区块号失败: {}", e.getMessage()); }
        return BigInteger.valueOf(18000000);
    }

    public BigInteger getBalance(String address) throws Exception {
        return web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send().getBalance();
    }

    // ===== 底层发送 =====

    private String sendTx(String contractAddr, Function func) throws Exception {
        String encoded = FunctionEncoder.encode(func);
        BigInteger nonce = web3j.ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.PENDING).send().getTransactionCount();
        long chainId = web3j.ethChainId().send().getChainId().longValue();

        RawTransaction rawTx = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, contractAddr, encoded);
        byte[] signed = TransactionEncoder.signMessage(rawTx, chainId, credentials);
        String hex = Numeric.toHexString(signed);

        EthSendTransaction tx = web3j.ethSendRawTransaction(hex).send();
        if (tx.hasError()) throw new BlockchainException("链上交易失败: " + tx.getError().getMessage());

        String txHash = tx.getTransactionHash();
        waitForTx(txHash);
        return txHash;
    }

    private EthCall callContract(String contractAddr, Function func) throws Exception {
        String encoded = FunctionEncoder.encode(func);
        return web3j.ethCall(
                org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                        credentials.getAddress(), contractAddr, encoded),
                DefaultBlockParameterName.LATEST).send();
    }

    private void waitForTx(String txHash) throws Exception {
        int attempts = 0;
        while (attempts < 60) {
            EthGetTransactionReceipt receipt = web3j.ethGetTransactionReceipt(txHash).send();
            if (receipt.getTransactionReceipt().isPresent()) {
                TransactionReceipt tr = receipt.getTransactionReceipt().get();
                if ("0x1".equals(tr.getStatus())) { log.info("交易确认: {}", txHash); return; }
                else throw new BlockchainException("链上交易被回滚: " + txHash);
            }
            Thread.sleep(1000);
            attempts++;
        }
        throw new BlockchainException("链上交易确认超时: " + txHash);
    }
}

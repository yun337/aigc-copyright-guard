// Direct deployment to Hardhat node using ethers v6
const { ethers } = require("ethers");
const fs = require("fs");
const path = require("path");

async function main() {
  const provider = new ethers.JsonRpcProvider("http://localhost:8545");
  const signer = new ethers.Wallet(
    "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80",
    provider
  );

  let nonce = await signer.getNonce();

  // 1. Deploy CopyrightRegistry
  const crJson = JSON.parse(fs.readFileSync(
    path.join(__dirname, "artifacts/contracts/CopyrightRegistry.sol/CopyrightRegistry.json")
  ));
  const CR = new ethers.ContractFactory(crJson.abi, crJson.bytecode, signer);
  const cr = await CR.deploy({ nonce: nonce++ });
  await cr.waitForDeployment();
  const crAddr = await cr.getAddress();
  console.log("CopyrightRegistry:", crAddr);

  // 2. Deploy CopyrightNFT
  const nftJson = JSON.parse(fs.readFileSync(
    path.join(__dirname, "artifacts/contracts/CopyrightNFT.sol/CopyrightNFT.json")
  ));
  const NFT = new ethers.ContractFactory(nftJson.abi, nftJson.bytecode, signer);
  const nft = await NFT.deploy({ nonce: nonce++ });
  await nft.waitForDeployment();
  const nftAddr = await nft.getAddress();
  console.log("CopyrightNFT:", nftAddr);

  // 3. Deploy AuthorizationManager
  const amJson = JSON.parse(fs.readFileSync(
    path.join(__dirname, "artifacts/contracts/AuthorizationManager.sol/AuthorizationManager.json")
  ));
  const AM = new ethers.ContractFactory(amJson.abi, amJson.bytecode, signer);
  const am = await AM.deploy({ nonce: nonce++ });
  await am.waitForDeployment();
  const amAddr = await am.getAddress();
  console.log("AuthorizationManager:", amAddr);

  // Save
  const info = {
    network: "localhost",
    chainId: "1337",
    deployer: signer.address,
    contracts: {
      copyrightRegistry: crAddr,
      copyrightNFT: nftAddr,
      authorizationManager: amAddr,
    },
    deployedAt: new Date().toISOString(),
  };
  fs.writeFileSync(path.join(__dirname, "deployments.json"), JSON.stringify(info, null, 2));
  console.log("\nAll contracts deployed successfully!");
  console.log("Registry:", crAddr);
  console.log("NFT:     ", nftAddr);
  console.log("Auth:    ", amAddr);
}

main().catch(e => { console.error(e); process.exit(1); });

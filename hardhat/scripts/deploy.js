const hre = require("hardhat");

/**
 * Deploy script for ProcurementOrder contract
 * Deploys to configured network (Sepolia, localhost, etc.)
 */
async function main() {
  console.log("Deploying ProcurementOrder contract...");

  const [deployer] = await hre.ethers.getSigners();
  console.log("Deploying contracts with the account:", deployer.address);
  
  const balance = await hre.ethers.provider.getBalance(deployer.address);
  console.log("Account balance:", hre.ethers.formatEther(balance), "ETH");

  // Deploy ProcurementOrder contract
  const ProcurementOrder = await hre.ethers.getContractFactory("ProcurementOrder");
  const procurementOrder = await ProcurementOrder.deploy();
  
  await procurementOrder.waitForDeployment();
  const contractAddress = await procurementOrder.getAddress();

  console.log("ProcurementOrder deployed to:", contractAddress);
  console.log("Network:", hre.network.name);
  console.log("Chain ID:", (await hre.ethers.provider.getNetwork()).chainId);
  
  // Save deployment info
  console.log("\nDeployment completed!");
  console.log("=====================================================");
  console.log("Update application-blockchain.yml with:");
  console.log("  contracts:");
  console.log("    procurement-order:");
  console.log("      address:", contractAddress);
  console.log("=====================================================");
  
  // Verify on Etherscan if deploying to Sepolia
  if (hre.network.name === "sepolia") {
    console.log("\nWaiting for block confirmations...");
    await procurementOrder.deploymentTransaction().wait(6);
    
    console.log("Verifying contract on Etherscan...");
    try {
      await hre.run("verify:verify", {
        address: contractAddress,
        constructorArguments: [],
      });
      console.log("Contract verified successfully!");
    } catch (error) {
      console.log("Error verifying contract:", error.message);
    }
  }
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
